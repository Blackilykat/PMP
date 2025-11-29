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

package dev.blackilykat.pmp.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.blackilykat.pmp.messages.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Device {
	// all reasonably usable standard ASCII chars
	@SuppressWarnings("SpellCheckingInspection")
	public static final String TOKEN_CHARSET =
			"!\"#$%&'()*+,-./0123456789:;" + "<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
	public static final int TOKEN_LENGTH = 128;

	private static final Logger LOGGER = LogManager.getLogger(Device.class);

	public final int id;

	public String name;

	@JsonIgnore
	private ClientConnection clientConnection = null;

	private String token;

	public Device(String name) {
		this.name = name;
		ServerStorage ss = ServerStorage.getInstance();
		this.id = ss.getAndIncrementCurrentDeviceId();
	}

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

	public void setClientConnection(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;
		clientConnection.eventDisconnected.register(_ -> {
			this.clientConnection = null;
		});
	}

	public void rerollToken() {
		try {
			SecureRandom random = SecureRandom.getInstanceStrong();
			char[] token = new char[TOKEN_LENGTH];
			for(int i = 0; i < TOKEN_LENGTH; i++) {
				token[i] = TOKEN_CHARSET.charAt(random.nextInt(TOKEN_CHARSET.length()));
			}
			this.token = new String(token);
			try {
				ServerStorage.doSave();
			} catch(IOException e) {
				LOGGER.error("Failed to forcefully save new token", e);
			}
		} catch(NoSuchAlgorithmException e) {
			String msg = "There are no SecureRandom algorithms supported by the JVM. This should never happen. Please "
					+ "send this log to the developer.";
			LOGGER.fatal(msg, e);
			System.exit(1);
		}
	}

	public static void broadcast(Message message) {
		broadcastExcept(message, null);
	}

	public static void broadcastExcept(Message message, Device ignoredDevice) {
		ServerStorage ss = ServerStorage.getInstance();
		for(Device device : ss.getDevices()) {
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
