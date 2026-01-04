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

package dev.blackilykat.pmp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blackilykat.pmp.Action;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.client.handlers.ActionMessageHandler;
import dev.blackilykat.pmp.client.handlers.FilterListMessageHandler;
import dev.blackilykat.pmp.client.handlers.LoginFailResponseHandler;
import dev.blackilykat.pmp.client.handlers.LoginSuccessResponseHandler;
import dev.blackilykat.pmp.client.handlers.PlaybackControlMessageHandler;
import dev.blackilykat.pmp.client.handlers.PlaybackOwnershipMessageHandler;
import dev.blackilykat.pmp.client.handlers.PlaybackUpdateMessageHandler;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.messages.ActionRequest;
import dev.blackilykat.pmp.messages.ActionResponse;
import dev.blackilykat.pmp.messages.ErrorMessage;
import dev.blackilykat.pmp.messages.GetActionsRequest;
import dev.blackilykat.pmp.messages.GetActionsResponse;
import dev.blackilykat.pmp.messages.LoginAsExistingDeviceRequest;
import dev.blackilykat.pmp.messages.Message;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
	public static final EventSource<Void> EVENT_CONNECTED = new EventSource<>();
	public static final EventSource<Void> EVENT_LOGGED_IN = new EventSource<>();
	public static final EventSource<Void> EVENT_DISCONNECTED = new EventSource<>();
	public static final EventSource<Void> EVENT_SHOULD_ASK_PASSWORD = new EventSource<>();

	private static final ScopedValue<Boolean> SHOULD_NOT_RECONNECT = ScopedValue.newInstance();
	private static final ScopedValue<Boolean> HANDLING_ACTION = ScopedValue.newInstance();
	private static final long RECONNECT_COOLDOWN_MS = 10_000;

	private static final Logger LOGGER = LogManager.getLogger(Server.class);
	private static final Timer RECONNECT_TIMER = new Timer("Server reconnect timer");
	private static final Object connectionLock = new Object();
	public static Integer deviceId = null;
	public static int lastActionId = -1;
	private static PMPConnection connection = null;
	private static SSLContext sslContext = null;

	private static ActionHandlingThread actionHandlingThread = null;
	private static ActionSendingThread actionSendingThread = null;
	private static ActionThreadDispatcher actionThreadDispatcher = null;
	private static List<TrackElement> serverTracks = null;

	public static void connect() {
		ClientStorage cs = ClientStorage.getInstance();
		if(sslContext == null) {
			try {
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, new TrustManager[]{new X509TrustManager() {
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {
						if(chain.length != 1) {
							throw new CertificateException(
									"Unexpected chain length, should contain 1 certificate but contains "
											+ chain.length);
						}

						Key serverKey = cs.getServerPublicKey();

						if(serverKey == null) {
							cs.setServerPublicKey(chain[0].getPublicKey());
						} else {
							if(!serverKey.equals(chain[0].getPublicKey())) {
								throw new CertificateException("Mismatching public keys");
							}
						}
					}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {
						checkClientTrusted(chain, authType);
					}

					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}
				}}, SecureRandom.getInstanceStrong());
			} catch(KeyManagementException | NoSuchAlgorithmException e) {
				LOGGER.error("Server#connect: this should be unreachable", e);
				return;
			}
		}

		String address = cs.getServerAddress();
		int port = cs.getServerPort();

		synchronized(connectionLock) {
			if(connection != null) {
				connection.disconnect("Opening new connection");
			}

			try {
				connection = new PMPConnection(sslContext.getSocketFactory().createSocket(address, port), "Server");

				connection.eventConnected.register(_ -> {
					EVENT_CONNECTED.call(null);
				});
				connection.eventDisconnected.register(_ -> {
					if(actionHandlingThread != null) {
						actionHandlingThread.interrupt();
						actionHandlingThread = null;
					}

					EVENT_DISCONNECTED.call(null);
					deviceId = null;
					if(SHOULD_NOT_RECONNECT.orElse(false)) {
						return;
					}

					scheduleReconnect();
				});

				Integer deviceId = cs.getDeviceID();
				String token = cs.getToken();
				if(deviceId != null && token != null) {
					connection.send(LoginAsExistingDeviceRequest.newWithToken(token, deviceId));
				} else {
					EVENT_SHOULD_ASK_PASSWORD.call(null);
				}
			} catch(ConnectException e) {
				connection = null;
				LOGGER.warn("Could not connect to {}:{}", address, port);
				scheduleReconnect();
			} catch(IOException e) {
				connection = null;
				LOGGER.error("There was an error connecting to {}:{}", address, port, e);
			}
		}
	}

	public static void disconnectWithoutRetrying() {
		if(connection != null) {
			ScopedValue.where(SHOULD_NOT_RECONNECT, true).run(() -> {
				connection.disconnect();
			});
		}
	}

	public static void disconnectWithoutRetrying(String reason) {
		if(connection != null) {
			ScopedValue.where(SHOULD_NOT_RECONNECT, true).run(() -> {
				connection.disconnect(reason);
			});
		}
	}

	public static void disconnect() {
		if(connection != null) {
			connection.disconnect();
		}
	}

	public static void disconnect(String reason) {
		if(connection != null) {
			connection.disconnect(reason);
		}
	}

	public static void setAddress(String address, int port, int filePort) {
		ClientStorage cs = ClientStorage.getInstance();
		cs.setServerAddress(address);
		cs.setServerPort(port);
		cs.setServerFilePort(filePort);
	}

	public static void send(Message message) {
		if(connection != null) {
			connection.send(message);
		} else {
			throw new IllegalStateException("Not connected");
		}
	}

	private static void scheduleReconnect() {
		RECONNECT_TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				if(connection != null && connection.connected) {
					return;
				}

				connect();
			}
		}, RECONNECT_COOLDOWN_MS);
	}

	/**
	 * In most cases, you'll want to use {@link #isLoggedIn()} instead.
	 *
	 * @return Whether the client's socket is connected to the server's and the PMPConnection has been established.
	 */
	public static boolean isConnected() {
		return connection != null && connection.connected;
	}

	/**
	 * @return Whether the client is connected to the server and is confirmed to be logged in successfully
	 */
	public static boolean isLoggedIn() {
		return isConnected() && deviceId != null;
	}

	/**
	 * Creates an HTTP request to the server at its transfer port, filling required authentication headers.
	 *
	 * @param method HTTP method for the request
	 * @param target URL path with or without the leading slash
	 */
	public static HttpsURLConnection startTransferRequest(String method, String target) throws IOException {
		ClientStorage cs = ClientStorage.getInstance();
		if(!target.startsWith("/")) {
			target = '/' + target;
		}
		target = URLEncoder.encode(target, StandardCharsets.UTF_8).replace("%2F", "/");
		URL url;
		try {
			url = URI.create("https://" + cs.getServerAddress() + ":" + cs.getServerFilePort() + target).toURL();
		} catch(MalformedURLException e) {
			LOGGER.error("(Server#makeTransferRequest) malformed URL, this should've been unreachable", e);
			assert false;
			throw new RuntimeException(e);
		}

		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setSSLSocketFactory(sslContext.getSocketFactory());
		// The expected certificate was checked in the socket factory. Hostname is irrelevant, skip this check.
		conn.setHostnameVerifier((_, _) -> true);
		conn.setRequestProperty("device", String.valueOf(cs.getDeviceID()));
		conn.setRequestProperty("token", cs.getToken());
		conn.setRequestMethod(method);
		if(method.equals("PUT") || method.equals("POST")) {
			conn.setDoOutput(true);
		}
		return conn;
	}

	/**
	 * Performs the HTTP request to the / endpoint to get the server's track list and stores it. This must run before
	 * the action threads start.
	 */
	public static void doTracklistRequest() throws IOException {
	}

	private static class ActionHandlingThread extends Thread {
		public ActionHandlingThread() {
			super("Action handling thread");
		}

		@Override
		public void run() {
			ScopedValue.where(HANDLING_ACTION, true).run(() -> {

				try {
					ClientStorage cs = ClientStorage.getInstance();

					{
						Action action = cs.peekActionsToHandle();
						while(action != null) {
							boolean skip = false;
							if(action.actionType == Action.Type.ADD || action.actionType == Action.Type.REPLACE) {
								for(Action viewing : cs.viewAllActionsToHandle()) {
									if(viewing != action && (viewing.actionType == Action.Type.REPLACE
											|| viewing.actionType == Action.Type.REMOVE) && viewing.filename.equals(
											action.filename)) {
										skip = true;
										break;
									}
								}
							}
							if(skip) {
								LOGGER.info("Skipping {} action of track {} because it was removed or replaced in a "
										+ "later action", action.actionType, action.filename);
								cs.takeActionToHandle();
							} else {
								LOGGER.info("(non-blocking) Handling {} action of track {}", action.actionType,
										action.filename);
								handleAction(action);
								cs.takeActionToHandle();
								LOGGER.info("(non-blocking) Handled {} action of track {}", action.actionType,
										action.filename);
							}
							action = cs.peekActionsToHandle();
						}
					}

					{
						LOGGER.info("Comparing libraries for download");
						assert serverTracks != null;

						for(TrackElement serverTrack : serverTracks) {
							Track clientTrack = Library.getTrackByFilename(serverTrack.filename);
							if(clientTrack == null) {
								boolean toBeDeleted = false;
								for(Action actionToSend : cs.viewAllActionsToSend()) {
									if(actionToSend.actionType == Action.Type.REMOVE && actionToSend.filename.equals(
											serverTrack.filename)) {
										toBeDeleted = true;
										break;
									}
								}
								if(!toBeDeleted) {
									LOGGER.warn("Client doesn't have {}, downloading", serverTrack.filename);
									handleAction(new Action(serverTrack.filename, Action.Type.ADD));
								}
							} else if(clientTrack.getChecksum() != serverTrack.checksum) {
								LOGGER.warn("Checksum for {} doesn't match, replacing with the server's version",
										serverTrack.filename);
								handleAction(new Action(serverTrack.filename, Action.Type.REPLACE));
							}
						}

						// do not find tracks to upload, that's done in the action sending thread
					}

					LOGGER.info("Listening for further actions to handle");
					while(!Thread.interrupted()) {
						Action action = cs.peekActionsToHandleBlocking();
						LOGGER.info("Handling {} action of track {}", action.actionType, action.filename);
						handleAction(action);
						cs.takeActionToHandle();
						LOGGER.info("Handled {} action of track {}", action.actionType, action.filename);
					}
				} catch(IOException e) {
					LOGGER.debug("(AHT) Network fail reason", e);
					disconnect("Failed to handle action due to network");
					return;
				} catch(InterruptedException _) {
				}
			});
			LOGGER.info("Action handling thread interrupted");
		}

		private static void handleAction(Action action) throws SocketException, InterruptedException {
			try {
				switch(action.actionType) {
					case ADD -> Library.handleAddAction(action);
					case REPLACE -> Library.handleReplaceAction(action);
					case REMOVE -> Library.handleRemoveAction(action);
					default -> LOGGER.warn("Received unhandled {} action, ignoring", action.actionType);
				}
			} catch(SocketException e) {
				// SocketException extends IOException, allow it to fall through
				throw e;
			} catch(InterruptedIOException e) {
				throw new InterruptedException();
			} catch(IOException e) {
				LOGGER.error("Failed to handle action", e);
			}
		}
	}

	private static class ActionSendingThread extends Thread {
		public ActionSendingThread() {
			super("Action sending thread");
		}

		@Override
		public void run() {
			try {
				ClientStorage cs = ClientStorage.getInstance();
				{
					Action action = cs.peekActionsToSend();
					while(action != null) {
						LOGGER.info("(non-blocking) Sending {} action of track {}", action.actionType,
								action.filename);
						sendAction(action);
						cs.takeActionToSend();
						action = cs.peekActionsToSend();
					}
				}

				clientTrackLoop:
				for(Track clientTrack : Library.getAllTracks()) {
					String filename = clientTrack.getFile().getName();
					for(TrackElement serverTrack : serverTracks) {
						if(filename.equals(serverTrack.filename)) {
							continue clientTrackLoop;
						}
					}

					for(Action action : cs.viewAllActionsToHandle()) {
						if(action.actionType == Action.Type.REMOVE && action.filename.equals(filename)) {
							continue clientTrackLoop;
						}
					}

					LOGGER.warn("Server doesn't have {}, uploading", filename);
					sendAction(new Action(filename, Action.Type.ADD));
				}

				LOGGER.info("Listening for further actions to send");
				while(!Thread.interrupted()) {
					Action action = cs.peekActionsToSendBlocking();
					LOGGER.info("Sending {} action of track {}", action.actionType, action.filename);
					sendAction(action);
					cs.takeActionToSend();
				}
			} catch(IOException e) {
				LOGGER.debug("(AST) Network fail reason", e);
				disconnect("Failed to send action due to network");
				return;
			} catch(InterruptedException _) {
			}
			LOGGER.info("Action sending thread interrupted");
		}

		private static void sendAction(Action action) throws IOException, InterruptedException {
			try {
				switch(action.actionType) {
					case ADD, REPLACE -> sendAddReplaceAction(action);
					case REMOVE -> sendRemoveAction(action);
					default -> LOGGER.warn("Attempted to send unimplemented {} action, ignoring", action.actionType);
				}
			} catch(InterruptedIOException e) {
				throw new InterruptedException();
			}
		}

		private static void sendAddReplaceAction(Action action) throws InterruptedException, IOException {
			assert action.actionType == Action.Type.ADD || action.actionType == Action.Type.REPLACE;

			Track track = Library.getTrackByFilename(action.filename);

			if(track == null) {
				LOGGER.warn("Tried to send {} on null track {}, skipping", action.actionType, action.filename);
				return;
			}

			ActionRequest req = new ActionRequest(action);
			send(req);
			ActionResponse res = req.takeResponse();

			// should only be sent once but repeating the check is free
			while(res.type == ActionResponse.Type.QUEUED) {
				res = req.takeResponse();
			}

			if(res.type == ActionResponse.Type.INVALID) {
				LOGGER.warn("Got INVALID action response, skipping this action");
				return;
			}
			if(res.type == ActionResponse.Type.COMPLETED) {
				send(new ErrorMessage("Early completed response, skipping this add/replace action"));
				return;
			}

			assert res.type == ActionResponse.Type.APPROVED;

			HttpsURLConnection conn = startTransferRequest("PUT", action.filename);
			Files.copy(track.getFile().toPath(), conn.getOutputStream());
			conn.getOutputStream().close();
			if(conn.getResponseCode() != 200) {
				LOGGER.error("Failed to upload {}, unexpected response code {}, skipping", action.filename,
						conn.getResponseCode());
				return;
			}
			res = req.takeResponse();

			if(res.type != ActionResponse.Type.COMPLETED) {
				send(new ErrorMessage("Got 200 OK but response type " + res.type));
				return;
			}

			ClientStorage cs = ClientStorage.getInstance();

			if(res.actionId == null) {
				send(new ErrorMessage("COMPLETED response without actionId"));
				cs.incrementLastReceivedAction();
				return;
			}

			assert res.actionId == cs.getLastReceivedAction() + 1;

			cs.setLastReceivedAction(res.actionId);
		}

		private static void sendRemoveAction(Action action) throws SocketException, InterruptedException {
			assert action.actionType == Action.Type.REMOVE;
			ActionRequest req = new ActionRequest(action);
			send(req);
			ActionResponse res = req.takeResponse();
			while(res.type == ActionResponse.Type.QUEUED) {
				res = req.takeResponse();
			}

			// if one of these is reached, it's likely that tracks will come back after being deleted.

			if(res.type == ActionResponse.Type.INVALID) {
				LOGGER.warn("Got INVALID action response, skipping this remove action");
				return;
			}

			if(res.type == ActionResponse.Type.APPROVED) {
				send(new ErrorMessage("Got APPROVED on REMOVE action, expected COMPLETED. Skipping it"));
				return;
			}

			assert res.type == ActionResponse.Type.COMPLETED;

			ClientStorage cs = ClientStorage.getInstance();

			if(res.actionId == null) {
				send(new ErrorMessage("COMPLETED response without actionId"));
				cs.incrementLastReceivedAction();
				return;
			}

			assert res.actionId == cs.getLastReceivedAction() + 1;

			cs.setLastReceivedAction(res.actionId);
		}
	}

	private static class ActionThreadDispatcher extends Thread {
		@Override
		public void run() {
			try {
				Library.waitUntilLoaded();

				ClientStorage cs = ClientStorage.getInstance();

				// lastActionId is set while handling LoginSuccessResponse, which happens right before this thread
				// starts. If the id in storage is -1, the client is connecting for the first time to the server and
				// should rely on comparing libraries instead of reading the action history
				if(cs.getLastReceivedAction() != -1 && lastActionId > cs.getLastReceivedAction()) {
					GetActionsRequest req = new GetActionsRequest(cs.getLastReceivedAction() + 1);
					send(req);
					GetActionsResponse res = req.takeResponse();

					for(Action action : res.actions) {
						cs.addActionToHandle(action);
					}
				}
				cs.setLastReceivedAction(lastActionId);

				HttpsURLConnection tracksRequest = startTransferRequest("GET", "/");
				if(tracksRequest.getResponseCode() != 200) {
					LOGGER.error("Got unexpected response code {} while getting track list",
							tracksRequest.getResponseCode());
					return;
				}
				ObjectMapper mapper = new ObjectMapper();
				serverTracks = mapper.readValue(tracksRequest.getInputStream(), new TypeReference<>() {});

				actionHandlingThread = new ActionHandlingThread();
				actionHandlingThread.start();
				actionSendingThread = new ActionSendingThread();
				actionSendingThread.start();
			} catch(IOException e) {
				LOGGER.error("IO Exception when dispatching action threads, disconnecting", e);
				disconnect("Failed to dispatch action threads");
			} catch(InterruptedException e) {
				LOGGER.error("Interrupted when dispatching action threads, disconnecting", e);
				disconnect("Interrupted when dispatching action threads");
			}
		}
	}

	private static class TrackElement {
		public String filename;
		public long checksum;
		public List<Pair<String, String>> metadata;
	}

	static {
		new LoginFailResponseHandler().register();
		new LoginSuccessResponseHandler().register();
		new PlaybackControlMessageHandler().register();
		new PlaybackOwnershipMessageHandler().register();
		new PlaybackUpdateMessageHandler().register();
		new FilterListMessageHandler().register();
		new ActionMessageHandler().register();

		EVENT_LOGGED_IN.register(_ -> {
			actionThreadDispatcher = new ActionThreadDispatcher();
			actionThreadDispatcher.start();
		});

		EVENT_DISCONNECTED.register(_ -> {
			if(actionThreadDispatcher != null) {
				actionThreadDispatcher.interrupt();
			}
			if(actionSendingThread != null) {
				actionSendingThread.interrupt();
			}
			if(actionHandlingThread != null) {
				actionHandlingThread.interrupt();
			}
		});

		Library.EVENT_TRACK_ADDED.register(track -> {
			if(HANDLING_ACTION.orElse(false)) {
				return;
			}

			LOGGER.debug("Added track {}, here a trace", track.getTitle(), new Throwable());

			ClientStorage cs = ClientStorage.getInstance();
			cs.addActionToSend(new Action(track.getFile().getName(), Action.Type.ADD));
		});

		Library.EVENT_TRACK_REMOVED.register(track -> {
			if(HANDLING_ACTION.orElse(false)) {
				return;
			}

			ClientStorage cs = ClientStorage.getInstance();
			cs.addActionToSend(new Action(track.getFile().getName(), Action.Type.REMOVE));
		});
	}
}
