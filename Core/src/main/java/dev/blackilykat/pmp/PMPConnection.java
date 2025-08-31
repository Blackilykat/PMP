/*
 * Copyright (C) 2025 Blackilykat and contributors
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
import dev.blackilykat.pmp.messages.DisconnectMessage;
import dev.blackilykat.pmp.messages.KeepaliveMessage;
import dev.blackilykat.pmp.messages.LoginMessage;
import dev.blackilykat.pmp.messages.Message;
import dev.blackilykat.pmp.messages.WelcomeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PMPConnection {
	private static final Logger LOGGER = LogManager.getLogger(PMPConnection.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	public final Socket socket;
	public final String name;
	public final Object connectedLock = new Object();
	private final MessageReceivingThread messageReceivingThread;
	private final InputStream inputStream;
	private final MessageSendingThread messageSendingThread;
	private final OutputStream outputStream;
	private final Object outputStreamLock = new Object();
	private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
	private final Timer keepaliveTimer;
	private final List<MessageListener<?>> listeners = new LinkedList<>();
	private final Object messageIdCounterLock = new Object();
	public Boolean connected = false;
	private long lastKeepalive;
	private int messageIdCounter = 0;

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

				if(System.currentTimeMillis() - lastKeepalive > KeepaliveMessage.KEEPALIVE_MAX_MS) {
					disconnect("Keepalive timeout");
				} else {
					send(new KeepaliveMessage());
				}
			}
		}, KeepaliveMessage.KEEPALIVE_MS, KeepaliveMessage.KEEPALIVE_MS);
	}

	/**
	 * Adds a message to the message queue
	 *
	 * @return the newly assigned message id
	 */
	public int send(Message message) {
		synchronized(messageIdCounterLock) {
			messageQueue.add(message.withMessageId(messageIdCounter));
			return messageIdCounter++;
		}
	}

	/**
	 * Sends a message ignoring the message queue and writing to the socket on this thread. Does not assign a message
	 * ID.
	 */
	private void sendNow(Message message) throws IOException {
		synchronized(outputStreamLock) {
			String messageStr = mapper.writeValueAsString(message);

			boolean anyRedacted = false;

			Message printedMessage = message.withMessageId(message.messageId);

			if(printedMessage instanceof LoginMessage loginMessage) {
				anyRedacted = true;
				if(loginMessage.password != null) {
					loginMessage.password = "REDACTED";
				}
				if(loginMessage.token != null) {
					loginMessage.token = "REDACTED";
				}
			} else if(printedMessage instanceof WelcomeMessage welcomeMessage) {
				anyRedacted = true;
				if(welcomeMessage.token != null) {
					welcomeMessage.token = "REDACTED";
				}
			}

			if(anyRedacted) {
				// re-serialize to print with redacted values
				//noinspection LoggingSimilarMessage
				LOGGER.info("Sending message to {} (some hidden values): {}", name,
						mapper.writeValueAsString(printedMessage));
			} else {
				LOGGER.info("Sending message to {}: {}", name, messageStr);
			}

			outputStream.write((messageStr + '\n').getBytes(StandardCharsets.UTF_8));
		}
	}

	public void disconnect(String reason) {
		LOGGER.warn("Disconnecting {}: {}", name, reason);
		_disconnect();
	}

	public void disconnect() {
		LOGGER.warn("Disconnecting {}", name);
		_disconnect();
	}

	private void _disconnect() {
		if(connected) {
			connected = false;
			try {
				int id;
				synchronized(messageIdCounterLock) {
					id = messageIdCounter++;
				}
				sendNow(new DisconnectMessage().withMessageId(id));
			} catch(IOException ignored) {
			}
		}
		try {
			socket.close();
		} catch(IOException ignored) {
		}

		messageReceivingThread.interrupt();
		messageSendingThread.interrupt();
		keepaliveTimer.cancel();
	}

	public void registerListener(MessageListener<?> listener) {
		if(listeners.contains(listener)) {
			throw new IllegalStateException("Listener already registered");
		}
		listeners.add(listener);
	}

	public void unregisterListener(MessageListener<?> listener) {
		if(!listeners.remove(listener)) {
			throw new IllegalStateException("Listener wasn't registered");
		}
	}

	private class MessageSendingThread extends Thread {

		public MessageSendingThread() {
			super("Message sending thread for " + name);
		}

		@Override
		public void run() {
			try {
				while(!Thread.interrupted()) {
					Message message = messageQueue.take();

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
					} else if(!inputBuffer.isEmpty()) {
						byte[] msg = new byte[inputBuffer.size()];
						for(int i = 0; i < msg.length; i++) {
							//noinspection DataFlowIssue
							msg[i] = inputBuffer.poll();
						}
						String messageString = new String(msg, StandardCharsets.UTF_8);
						if(messageString.equals("PMP")) {
							LOGGER.info("Received PMP signature from {}", name);
							connected = true;
							synchronized(connectedLock) {
								connectedLock.notifyAll();
							}
							continue;
						}
						if(!connected) {
							LOGGER.error("{} did not send PMP signature. Disconnecting", name);
							disconnect();
							break;
						}
						try {
							Message message = mapper.readValue(messageString, Message.class);

							boolean sendRaw = true;
							Message printedMessage = message.withMessageId(message.messageId);
							if(printedMessage instanceof LoginMessage loginMessage) {
								sendRaw = false;
								if(loginMessage.password != null) {
									loginMessage.password = "REDACTED";
								}
								if(loginMessage.token != null) {
									loginMessage.token = "REDACTED";
								}
							} else if(printedMessage instanceof WelcomeMessage welcomeMessage) {
								sendRaw = false;
								if(welcomeMessage.token != null) {
									welcomeMessage.token = "REDACTED";
								}
							}
							if(sendRaw) {
								//noinspection LoggingSimilarMessage
								LOGGER.info("Received message from {}: {}", name, messageString);
							} else {
								LOGGER.info("Received message from {} (some hidden values): {}", name,
										mapper.writeValueAsString(printedMessage));
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
							} else {
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

								if(!foundHandler) {
									LOGGER.warn("Unhandled message type {}", message.getClass().getSimpleName());
								}
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

	static {
		mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

		MessageHandler.registeredHandlers.add(new MessageHandler<>(KeepaliveMessage.class) {
			@Override
			public void run(PMPConnection connection, KeepaliveMessage message) {
				connection.lastKeepalive = System.currentTimeMillis();
			}
		});

		MessageHandler.registeredHandlers.add(new MessageHandler<>(DisconnectMessage.class) {
			@Override
			public void run(PMPConnection connection, DisconnectMessage message) {
				connection.connected = false;
				connection.disconnect("Received disconnect message");
			}
		});
	}
}
