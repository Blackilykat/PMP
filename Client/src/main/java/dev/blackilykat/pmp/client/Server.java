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

package dev.blackilykat.pmp.client;

import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.client.handlers.FilterListMessageHandler;
import dev.blackilykat.pmp.client.handlers.LoginFailResponseHandler;
import dev.blackilykat.pmp.client.handlers.LoginSuccessResponseHandler;
import dev.blackilykat.pmp.client.handlers.PlaybackControlMessageHandler;
import dev.blackilykat.pmp.client.handlers.PlaybackOwnershipMessageHandler;
import dev.blackilykat.pmp.client.handlers.PlaybackUpdateMessageHandler;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.messages.LoginAsExistingDeviceRequest;
import dev.blackilykat.pmp.messages.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.ConnectException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
	public static final EventSource<Void> EVENT_CONNECTED = new EventSource<>();
	public static final EventSource<Void> EVENT_DISCONNECTED = new EventSource<>();
	public static final EventSource<Void> EVENT_SHOULD_ASK_PASSWORD = new EventSource<>();

	private static final ScopedValue<Boolean> SHOULD_NOT_RECONNECT = ScopedValue.newInstance();
	private static final long RECONNECT_COOLDOWN_MS = 10_000;

	private static final Logger LOGGER = LogManager.getLogger(Server.class);
	private static final Timer RECONNECT_TIMER = new Timer("Server reconnect timer");
	private static final Object connectionLock = new Object();
	public static Integer deviceId = null;
	private static PMPConnection connection = null;
	private static SSLContext sslContext = null;

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

	static {
		new LoginFailResponseHandler().register();
		new LoginSuccessResponseHandler().register();
		new PlaybackControlMessageHandler().register();
		new PlaybackOwnershipMessageHandler().register();
		new PlaybackUpdateMessageHandler().register();
		new FilterListMessageHandler().register();
	}
}
