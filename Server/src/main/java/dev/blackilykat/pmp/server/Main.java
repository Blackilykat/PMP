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

import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.messages.LoginAsExistingDeviceRequest;
import dev.blackilykat.pmp.messages.LoginAsNewDeviceRequest;
import dev.blackilykat.pmp.messages.Message;
import dev.blackilykat.pmp.server.handlers.ActionRequestHandler;
import dev.blackilykat.pmp.server.handlers.FilterListMessageHandler;
import dev.blackilykat.pmp.server.handlers.GetActionsRequestHandler;
import dev.blackilykat.pmp.server.handlers.LoginAsExistingDeviceRequestHandler;
import dev.blackilykat.pmp.server.handlers.LoginAsNewDeviceRequestHandler;
import dev.blackilykat.pmp.server.handlers.PlaybackControlMessageHandler;
import dev.blackilykat.pmp.server.handlers.PlaybackOwnershipMessageHandler;
import dev.blackilykat.pmp.server.handlers.PlaybackUpdateMessageHandler;
import dev.blackilykat.pmp.util.LoggingProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLServerSocket;
import java.io.Console;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
	public static final EventSource<Void> EVENT_SHUTDOWN = new EventSource<>();
	private static final Logger LOGGER = LogManager.getLogger(Main.class);

	static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			EVENT_SHUTDOWN.call(null);
		}));

		try {
			ServerStorage.load();
		} catch(IOException e) {
			LOGGER.fatal("Failed to load storage", e);
			System.exit(1);
		}

		{
			boolean passwordArg = Arrays.stream(args).toList().contains("--password");
			if(passwordArg || ServerStorage.SENSITIVE.password.get() == null) {
				Console console = System.console();
				if(console == null) {
					LOGGER.fatal(
							"Need a real terminal to ask password, or \"password\" to have a value in "
									+ ".sensitive_server.json");
					System.exit(1);
				}

				while(true) {
					System.out.print("Enter the new password: ");
					String first = console.readLine();
					System.out.print("Confirm the new password: ");
					String second = console.readLine();
					if(!first.equals(second)) {
						System.out.println("Passwords entered do not match!");
						continue;
					}
					ServerStorage.SENSITIVE.password.set(first);
					break;
				}

				LOGGER.info("Saved new password");
				if(passwordArg) {
					LOGGER.info("Found password argument, exiting");
					System.exit(0);
				}
			}
		}

		Library.init();

		LoggingProxy.setUpProxies();

		Encryption.init();

		registerHandlers();

		Playback.init();

		try {
			TransferHandler.init();
		} catch(IOException e) {
			LOGGER.fatal("Failed to start transfer server", e);
			System.exit(1);
		}

		SSLServerSocket serverSocket = null;
		try {
			serverSocket = (SSLServerSocket) Encryption.getSslContext()
					.getServerSocketFactory()
					.createServerSocket(PMPConnection.DEFAULT_MESSAGE_PORT);
		} catch(IOException e) {
			LOGGER.fatal("Failed to create server socket", e);
			System.exit(1);
		}

		assert serverSocket != null;

		//noinspection InfiniteLoopStatement
		while(true) {
			try {
				ClientConnection conn = new ClientConnection(serverSocket.accept());
			} catch(Exception e) {
				LOGGER.warn("Failed to connect to a client", e);
			} catch(Throwable t) {
				LOGGER.fatal("Something went very wrong while connecting a client", t);
				throw t;
			}
		}
	}

	private static void registerHandlers() {
		new LoginAsNewDeviceRequestHandler().register();
		new LoginAsExistingDeviceRequestHandler().register();
		new PlaybackControlMessageHandler().register();
		new PlaybackOwnershipMessageHandler().register();
		new PlaybackUpdateMessageHandler().register();
		new FilterListMessageHandler().register();
		new GetActionsRequestHandler().register();
		new ActionRequestHandler().register();

		PMPConnection.EVENT_RECEIVING_MESSAGE.register(evt -> {
			if(!(evt.connection instanceof ClientConnection connection)) {
				return;
			}
			if(connection.device != null) {
				return;
			}

			List<Class<? extends Message>> loggedOutWhitelist = List.of(LoginAsNewDeviceRequest.class,
					LoginAsExistingDeviceRequest.class);
			if(loggedOutWhitelist.contains(evt.message.getClass())) {
				return;
			}

			evt.cancel();
		});
	}
}
