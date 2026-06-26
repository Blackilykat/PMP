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

package dev.blackilykat.pmp.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.blackilykat.pmp.messages.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/// Unique identifier for a client instance of PMP.
///
/// Once a client logs in for the first time, a Device will be created for it.
/// It will then be given a device ID and token which it will use next time to log in as the existing device.
///
/// A device is assigned a name, which is the computer it is running in's hostname. This allows the user to
/// easily identify different devices.
///
/// A device's token acts as a "one-time-password" which allows clients to avoid storing the server password
/// while not forcing the user to write it every time they use the program. Tokens are strings of characters
/// to be easily sent over JSON. Tokens are re-rolled at every login.
public class Device {
	/// All available characters used to generate a token. Contains all standard (7-bit) ASCII characters
	/// which can be easily used in a JSON string.
	///
	/// @see #TOKEN_LENGTH
	/// @see #rerollToken
	@SuppressWarnings("SpellCheckingInspection")
	public static final String TOKEN_CHARSET =
			"!\"#$%&'()*+,-./0123456789:;" + "<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

	/// Length of a token, measured in characters.
	///
	/// @see #TOKEN_CHARSET
	/// @see #rerollToken
	public static final int TOKEN_LENGTH = 128;

	private static final Logger LOGGER = LogManager.getLogger(Device.class);

	/// Numerical unique identifier used in the protocol to refer to a device.
	public final int id;

	/// Hostname of the device's computer.
	public String name;

	/// Which connection this device is currently logged in in. May be null.
	@JsonIgnore
	private ClientConnection clientConnection = null;

	/// The secret token used to log in.
	private String token;

	/// Create a new device and assign it a unique ID.
	public Device(String name) {
		this.name = name;
		this.id = ServerStorage.MAIN.currentDeviceID.getAndIncrement();
	}

	/// Create a device with a known ID, used while deserializing storage.
	@JsonCreator
	public Device(String name, int id) {
		this.name = name;
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public ClientConnection getClientConnection() {
		return clientConnection;
	}

	/// Set [#clientConnection] and register a listener to set it back to null when disconnected.
	public void setClientConnection(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;
		clientConnection.eventDisconnected.register(_ -> {
			this.clientConnection = null;
		});
	}

	/// Randomly generate and set a new token for this device.
	public void rerollToken() {
		try {
			SecureRandom random = SecureRandom.getInstanceStrong();
			char[] token = new char[TOKEN_LENGTH];
			for(int i = 0; i < TOKEN_LENGTH; i++) {
				token[i] = TOKEN_CHARSET.charAt(random.nextInt(TOKEN_CHARSET.length()));
			}
			this.token = new String(token);
			ServerStorage.SENSITIVE.markDirty();
		} catch(NoSuchAlgorithmException e) {
			String msg = "There are no SecureRandom algorithms supported by the JVM. This should never happen. Please "
					+ "send this log to the developer.";
			LOGGER.fatal(msg, e);
			System.exit(1);
		}
	}

	/// Send a message to all connections which have logged in.
	public static void broadcast(Message message) {
		broadcastExcept(message, null);
	}

	/// Send a message to all connections which have logged in, except the one logged in as `ignoredDevice`.
	public static void broadcastExcept(Message message, Device ignoredDevice) {
		for(Device device : ServerStorage.SENSITIVE.devices.get()) {
			if(device == ignoredDevice) {
				continue;
			}
			ClientConnection connection = device.getClientConnection();
			if(connection == null) {
				continue;
			}

			connection.send(message);
		}
	}
}
