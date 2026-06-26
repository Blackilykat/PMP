/*
 * Copyright (C) 2026 Blackilykat and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat.pmp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.event.RetroactiveEventSource;
import dev.blackilykat.pmp.messages.DisconnectMessage;
import dev.blackilykat.pmp.messages.Message;
import dev.blackilykat.pmp.messages.Request;
import dev.blackilykat.pmp.messages.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/// Defines a connection which uses PMP's protocol.
///
/// The PMP protocol uses a TCP connection which should, but does not have to, be encrypted with SSL.
///
/// The connection is established once both sides send 4 bytes: "PMP\n".
/// This is referred to as the PMP signature.
/// This allows the server to recognize when it is accepting non-PMP clients and to terminate those connections.
///
/// Messages are sent serialized using JSON.
/// The type of each message is defined by the property "messageType".
/// Messages are separated by newlines.
///
/// Every {@value #KEEPALIVE_MS} milliseconds, an extra newline should be sent.
/// This is equivalent to an empty line and will be treated as a keepalive.
/// If a keepalive is not received within {@value #KEEPALIVE_MAX_MS} milliseconds,
/// any side can assume the connection has silently dropped and terminate it.
///
/// @see dev.blackilykat.pmp.server.Encryption
public class PMPConnection {
	/// Emitted when any message is received on any client and allows to cancel it before it gets handled.
	///
	/// @see MessageListener
	public static final EventSource<ReceivingMessageEvent> EVENT_RECEIVING_MESSAGE = new EventSource<>();
	/// Event emitted when any connection has disconnected. Contains the terminated connection as its data.
	public static final EventSource<PMPConnection> EVENT_DISCONNECTED = new EventSource<>();

	/// The default port used for transferring messages.
	public static final int DEFAULT_MESSAGE_PORT = 6803;
	/// The default port used for transferring files through HTTP.
	///
	/// @see dev.blackilykat.pmp.server.TransferHandler
	public static final int DEFAULT_FILE_PORT = 6804;

	/// The amount of milliseconds between sending keepalives.
	private static final int KEEPALIVE_MS = 10_000;
	/// The amount of milliseconds since the last keepalive after which a connection can be considered dropped.
	private static final int KEEPALIVE_MAX_MS = 30_000;
	private static final Logger LOGGER = LogManager.getLogger(PMPConnection.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	/// The underlying TCP socket of this connection.
	public final Socket socket;
	/// The name of this connection, used to differentiate logging on the server side.
	public final String name;
	/// Event emitted once this connection has been confirmed by receiving the other side's PMP signature.
	public final RetroactiveEventSource<Void> eventConnected = new RetroactiveEventSource<>();
	/// Event emitted once this connection has been terminated for any reason.
	///
	/// After this is emitted, [#EVENT_DISCONNECTED] is always also emitted with this as its content.
	public final RetroactiveEventSource<Void> eventDisconnected = new RetroactiveEventSource<>();
	/// The thread which reads incoming messages and calls their listeners and handlers.
	private final MessageReceivingThread messageReceivingThread;
	/// The input stream the raw incoming serialized messages get read from.
	private final InputStream inputStream;
	/// The thread which serializes and writes outgoing messages.
	///
	/// This is not the only thread which is allowed to write to [#outputStream].
	/// Both [#keepaliveTimer] and any thread initiating a disconnect will also do so.
	/// [#outputStreamLock] is used to keep exclusive access to the output stream.
	private final MessageSendingThread messageSendingThread;
	/// The output stream the raw serialized messages get written to.
	///
	/// Any piece of code attempting to write to this stream should be enclosed in a synchronized block with [#outputStreamLock].
	private final OutputStream outputStream;
	/// The object used to keep exclusive access to [#outputStream].
	private final Object outputStreamLock = new Object();
	/// The queue of non-serialized messages to be sent.
	/// Unless the message is urgent, it will be placed here for [#messageSendingThread] to take, serialize and send over the network.
	private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
	/// The timer used to send keepalive messages.
	private final Timer keepaliveTimer;
	/// All listeners registered for this connection.
	private final List<MessageListener<?>> listeners = new LinkedList<>();
	/// All requests and their ids which are still pending a final response.
	///
	/// @see Response#isLastResponse()
	private final Map<Integer, Request> pendingRequests = new HashMap<>();
	/// Whether the connection has been confirmed by receiving the PMP signature from the other side.
	public Boolean connected = false;
	/// Unix timestamp of the last keepalive.
	private long lastKeepalive;

	/// Initiate a PMP connection:
	/// - Sets required fields
	/// - Writes the PMP signature
	/// - Starts the [#messageReceivingThread]
	/// - Starts the [#messageSendingThread]
	/// - Schedules sending keepalives and checking the other side's keepalive timeout
	public PMPConnection(Socket socket, String name) throws IOException {
		if(!(socket instanceof SSLSocket)) {
			LOGGER.warn("PMP Connection with insecure socket");
		}

		this.socket = socket;
		this.name = name;

		this.inputStream = socket.getInputStream();
		this.outputStream = socket.getOutputStream();
		this.outputStream.write(new byte[]{'P', 'M', 'P', '\n'});

		messageReceivingThread = new MessageReceivingThread();
		messageReceivingThread.start();
		messageSendingThread = new MessageSendingThread();
		messageSendingThread.start();

		keepaliveTimer = new Timer("Keepalive timer for " + name);
		lastKeepalive = System.currentTimeMillis();
		keepaliveTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// Keepalive timeout doesn't need to be exact so it's fine to send and check at the same time

				if(System.currentTimeMillis() - lastKeepalive > KEEPALIVE_MAX_MS) {
					disconnect("Keepalive timeout");
				} else {
					try {
						sendKeepalive();
					} catch(IOException e) {
						disconnect("Failed to send keepalive");
					}
				}
			}
		}, KEEPALIVE_MS, KEEPALIVE_MS);
	}

	/// Adds a message to the message queue
	public void send(Message message) {
		if(message instanceof Request request) {
			request.setConnection(PMPConnection.this);
		}
		messageQueue.add(message);
	}

	/// Sends a message ignoring the message queue and writing to the socket on this thread.
	/// Does not assign a request ID.
	private void sendNow(Message message) throws IOException {
		synchronized(outputStreamLock) {
			LOGGER.info("Sending message to {}: {}", name, mapper.writeValueAsString(message.withRedactedInfo()));

			outputStream.write((mapper.writeValueAsString(message) + '\n').getBytes(StandardCharsets.UTF_8));
		}
	}

	/// Sends a keepalive on this thread.
	///
	/// A keepalive is an extra newline, practically an "empty line" instead of containing a message.
	private void sendKeepalive() throws IOException {
		synchronized(outputStreamLock) {
			outputStream.write((int) '\n');
		}
	}

	/// Terminate this connection.
	///
	/// @param reason Human readable reason for why the connection was terminated, for logging.
	public void disconnect(String reason) {
		LOGGER.warn("Disconnecting {}: {}", name, reason);
		_disconnect();
	}

	/// Disconnect on the message sending thread as soon as all currently queued messages are sent.
	/// Useful to disconnect without performing network operations on the current thread.
	///
	/// @param reason Human readable reason for why the connection was terminated, for logging.
	public void disconnectSoon(String reason) {
		LOGGER.warn("Disconnecting soon {}: {}", name, reason);
		send(new DisconnectMessage());
	}

	/// Internal disconnect method to terminate the connection without logging.
	///
	/// Attempts to send a disconnect message on this thread, closes the socket and terminates all threads.
	private void _disconnect() {
		boolean wasConnected = connected;
		if(connected) {
			connected = false;
			try {
				sendNow(new DisconnectMessage());
			} catch(IOException ignored) {
			}
		}
		try {
			socket.close();
		} catch(IOException ignored) {
		}

		messageReceivingThread.interrupt();
		if(!messageSendingThread.equals(Thread.currentThread())) {
			messageSendingThread.interrupt();
		}
		keepaliveTimer.cancel();

		if(wasConnected) {
			eventDisconnected.call(null);
			EVENT_DISCONNECTED.call(this);
		}
	}

	/// Register a message listener for this connection.
	///
	/// @see #unregisterListener(MessageListener)
	/// @see MessageListener
	public void registerListener(MessageListener<?> listener) {
		if(listeners.contains(listener)) {
			throw new IllegalStateException("Listener already registered");
		}
		listeners.add(listener);
	}

	/// Unregister a message listener for this connection.
	///
	/// @see #unregisterListener(MessageListener)
	/// @see MessageListener
	public void unregisterListener(MessageListener<?> listener) {
		if(!listeners.remove(listener)) {
			throw new IllegalStateException("Listener wasn't registered");
		}
	}

	/// The thread which serializes and writes outgoing messages.
	private class MessageSendingThread extends Thread {
		public MessageSendingThread() {
			super("Message sending thread for " + name);
		}

		@Override
		public void run() {
			try {
				while(!Thread.interrupted()) {
					Message message = messageQueue.take();

					if(message instanceof DisconnectMessage) {
						_disconnect();
						return;
					}

					if(message instanceof Request request) {
						if(request.requestId == null) {
							request.assignId();
						}

						assert !pendingRequests.containsKey(request.requestId);
						pendingRequests.put(request.requestId, request);
					}

					sendNow(message);
				}
			} catch(IOException e) {
				if(!connected) {
					return;
				}
				LOGGER.error("IO exception in message sending thread", e);
			} catch(InterruptedException ignored) {
			} catch(Exception e) {
				LOGGER.error("Unknown exception in message sending thread", e);
			} finally {
				if(connected) {
					disconnect("Message sending thread terminated");
				}
			}
		}
	}

	/// The thread which reads incoming messages and calls their listeners and handlers.
	private class MessageReceivingThread extends Thread {
		public MessageReceivingThread() {
			super("Message receiving thread for " + name);
		}

		@Override
		public void run() {
			Queue<Byte> inputBuffer = new ArrayDeque<>();
			try {
				int read;
				while(!Thread.interrupted()) {
					read = inputStream.read();
					if(read == -1) {
						break;
					}
					if(read != ((int) '\n')) {
						inputBuffer.add((byte) read);
					} else if(inputBuffer.isEmpty()) {
						lastKeepalive = System.currentTimeMillis();
					} else {
						byte[] msg = new byte[inputBuffer.size()];
						for(int i = 0; i < msg.length; i++) {
							//noinspection DataFlowIssue
							msg[i] = inputBuffer.poll();
						}
						String messageString = new String(msg, StandardCharsets.UTF_8);
						if(messageString.equals("PMP")) {
							LOGGER.info("Received PMP signature from {}", name);
							connected = true;
							eventConnected.call(null);
							continue;
						}
						if(!connected) {
							disconnect("Did not receive PMP signature");
							break;
						}
						try {
							Message message = mapper.readValue(messageString, Message.class);


							Message printedMessage = message.withRedactedInfo();
							if(printedMessage == message) {
								//noinspection LoggingSimilarMessage
								LOGGER.info("Received message from {}: {}", name, messageString);
							} else {
								LOGGER.info("Received message from {} (some hidden values): {}", name,
										mapper.writeValueAsString(printedMessage));
							}

							ReceivingMessageEvent evt = new ReceivingMessageEvent(message, PMPConnection.this);
							EVENT_RECEIVING_MESSAGE.call(evt);
							if(evt.isCancelled()) {
								continue;
							}

							if(message instanceof Response response) {
								Request request = pendingRequests.get(response.requestId);
								if(request != null) {
									request.addResponse(response);

									if(response.isLastResponse()) {
										pendingRequests.remove(response.requestId);
									}
								}
							}

							AtomicBoolean cancelled = new AtomicBoolean(false);

							for(MessageListener<?> listener : listeners) {
								if(!listener.type.isInstance(message)) {
									continue;
								}
								LOGGER.debug("Found listener for {}", listener.type.getSimpleName());
								try {
									listener.runCasting(message, cancelled);
								} catch(Exception e) {
									LOGGER.error("Exception in message listener", e);
								}
							}

							if(cancelled.get()) {
								LOGGER.info("A {} message was cancelled", message.getClass().getSimpleName());
								continue;
							}

							boolean foundHandler = false;

							for(MessageHandler<?> handler : MessageHandler.registeredHandlers) {
								if(!handler.type.isInstance(message)) {
									continue;
								}
								if(foundHandler) {
									LOGGER.warn("Multiple handlers for message type {}",
											message.getClass().getSimpleName());
								}
								foundHandler = true;
								try {
									handler.runCasting(PMPConnection.this, message);
								} catch(Exception e) {
									LOGGER.error("Exception in message listener", e);
								}
							}

							// responses can have no handler but be handled through Request#takeResponse
							if(!foundHandler && !(message instanceof Response)) {
								LOGGER.warn("Unhandled message type {}", message.getClass().getSimpleName());
							}
						} catch(JsonProcessingException e) {
							LOGGER.error("Invalid message format: {} (original message: '{}')", e.getMessage(),
									inputBuffer.toString());
						}
					}
				}
			} catch(IOException e) {
				if(!connected) {
					return;
				}
				LOGGER.error("IO exception in message receiving thread", e);
			} catch(Exception e) {
				LOGGER.error("Unknown exception in message receiving thread", e);
			} finally {
				if(connected) {
					disconnect("Message receiving thread terminated");
				}
			}
		}
	}

	/// Data for [PMPConnection#EVENT_RECEIVING_MESSAGE]
	public static class ReceivingMessageEvent {
		public final PMPConnection connection;
		public final Message message;
		private boolean cancelled;

		public ReceivingMessageEvent(Message message, PMPConnection connection) {
			this.message = message;
			this.connection = connection;
		}

		public boolean isCancelled() {
			return cancelled;
		}

		public void cancel() {
			cancelled = true;
			LOGGER.info("Cancelled incoming message at {}", new Throwable().getStackTrace()[1]);
		}
	}

	static {
		mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

		MessageHandler.registeredHandlers.add(new MessageHandler<>(DisconnectMessage.class) {
			@Override
			public void run(PMPConnection connection, DisconnectMessage message) {
				connection.disconnect("Received disconnect message");
			}
		});
	}
}
