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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import dev.blackilykat.pmp.PMPConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

public class TransferHandler implements HttpHandler {
	private static final Logger LOGGER = LogManager.getLogger(TransferHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		InetSocketAddress address = exchange.getRemoteAddress();
		LOGGER.info("(HTTP) Handling {} request at {} from {}", exchange.getRequestMethod(), exchange.getRequestURI(),
				address);
		Integer deviceId = checkAuthorization(exchange);
		if(deviceId == null) {
			return;
		}

		switch(exchange.getRequestMethod()) {
			case "GET" -> handleAuthenticatedGet(exchange);
			case "PUT" -> handleAuthenticatedPut(exchange, deviceId);
			default -> {
				LOGGER.info("(HTTP) {} sent unsupported {} request -> 405", address, exchange.getRequestMethod());
				exchange.sendResponseHeaders(405, 0);
				exchange.close();
			}
		}
	}

	private void handleAuthenticatedGet(HttpExchange exchange) throws IOException {
		LOGGER.info("(HTTP) Authenticated get");
		InetSocketAddress address = exchange.getRemoteAddress();
		String path = exchange.getRequestURI().getPath();
		if(path.equals("/")) {
			sendTrackList(exchange);
		} else {
			if(path.lastIndexOf('/') != 0) {
				LOGGER.info("(HTTP) {} requested resource with '/' in filename -> 400", address);
				exchange.sendResponseHeaders(400, 0);
				exchange.close();
				return;
			}
			File file = Library.LIBRARY.toPath().resolve(path.substring(1)).toFile();
			if(!file.exists()) {
				LOGGER.info("(HTTP) {} requested unknown resource -> 404", address);
				exchange.sendResponseHeaders(404, 0);
				exchange.close();
				return;
			}
			if(file.isDirectory()) {
				LOGGER.error("(HTTP) {} requested directory '{}' in library -> 500", address, file);
				exchange.sendResponseHeaders(500, 0);
				exchange.close();
				return;
			}

			try(var is = new FileInputStream(file)) {
				exchange.sendResponseHeaders(200, file.length());
				Files.copy(file.toPath(), exchange.getResponseBody());
				exchange.close();
			} catch(IOException e) {
				LOGGER.error("(HTTP) IO exception when serving file {} to {}", file, address, e);
				exchange.close();
			}
		}
	}

	private void handleAuthenticatedPut(HttpExchange exchange, int deviceId) throws IOException {
		LOGGER.info("(HTTP) Authenticated put");
		InetSocketAddress address = exchange.getRemoteAddress();
		String filename = exchange.getRequestURI().getPath().substring(1);

		ServerStorage ss = ServerStorage.getInstance();
		Device device = null;
		for(var d : ss.getDevices()) {
			if(d.id == deviceId) {
				device = d;
				break;
			}
		}

		if(!Library.startPendingAction(device, filename)) {
			LOGGER.info("(HTTP) {} tried to send non-pending action -> 403", address);
			exchange.sendResponseHeaders(403, 0);
			exchange.close();
			return;
		}

		try {
			Library.add(filename, exchange.getRequestBody());
			Library.finishPendingAction(true);
		} catch(IOException e) {
			LOGGER.error("(HTTP) Unexpected IOException when receiving track from {} -> 500", address, e);
			exchange.sendResponseHeaders(500, 0);
			exchange.close();
			Library.finishPendingAction(false);
			return;
		} catch(IllegalArgumentException e) {
			LOGGER.info("(HTTP) {} sent an invalid flac file -> 400", address);
			exchange.sendResponseHeaders(400, 0);
			exchange.close();
			Library.finishPendingAction(false);
			return;
		}

		exchange.sendResponseHeaders(200, 0);
		exchange.close();
	}

	private void sendTrackList(HttpExchange exchange) throws IOException {
		ObjectMapper om = new ObjectMapper();
		// omit lastModified
		om.addMixIn(Track.class, TrackMixin.class);
		byte[] res = om.writeValueAsString(Library.TRACKS).getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, res.length);
		OutputStream os = exchange.getResponseBody();
		os.write(res);
		os.close();
	}

	/**
	 * Checks the request is coming from an authenticated device and responds appropriately if not
	 *
	 * @return device id if authorized, null if not
	 */
	private Integer checkAuthorization(HttpExchange exchange) throws IOException {
		InetSocketAddress address = exchange.getRemoteAddress();

		Headers headers = exchange.getRequestHeaders();
		String claimedDeviceIdStr = headers.getFirst("device");
		String claimedToken = headers.getFirst("token");
		if(claimedDeviceIdStr == null || claimedToken == null) {
			LOGGER.info("(HTTP) {} had no device or token -> 401", address);
			exchange.sendResponseHeaders(401, 0);
			exchange.close();
			return null;
		}


		int claimedDeviceId;
		try {
			claimedDeviceId = Integer.parseInt(claimedDeviceIdStr);
		} catch(NumberFormatException _) {
			LOGGER.info("(HTTP) {} had a non-numerical device id -> 400", address);
			exchange.sendResponseHeaders(400, 0);
			exchange.close();
			return null;
		}

		ServerStorage ss = ServerStorage.getInstance();
		boolean authorized = false;
		for(Device device : ss.getDevices()) {
			if(device.id != claimedDeviceId) {
				continue;
			}
			if(Objects.equals(device.getToken(), claimedToken)) {
				authorized = true;
				break;
			}
			LOGGER.info("(HTTP) {} has an incorrect token for device {} -> 401", address, device.id);
			exchange.sendResponseHeaders(401, 0);
			exchange.close();
			return null;
		}

		if(!authorized) {
			LOGGER.info("(HTTP) {} tried to log in with non-existent device {} -> 401", address, claimedDeviceId);
			exchange.sendResponseHeaders(401, 0);
			exchange.close();
			return null;
		}

		return claimedDeviceId;
	}

	public static void init() throws IOException {
		LOGGER.info("Initializing transfer server...");
		HttpsServer server = HttpsServer.create(new InetSocketAddress(PMPConnection.DEFAULT_FILE_PORT), 0);
		server.setHttpsConfigurator(new HttpsConfigurator(Encryption.getSslContext()));
		server.createContext("/", new TransferHandler());
		server.start();
	}

	@JsonIgnoreProperties(value = {"lastModified"})
	private static abstract class TrackMixin {}
}
