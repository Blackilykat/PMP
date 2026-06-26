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
import dev.blackilykat.pmp.messages.ActionRequest;
import dev.blackilykat.pmp.messages.ActionResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

/// Manages the HTTP server for transferring files.
///
/// # Endpoints
///
/// There is a special endpoint `GET /` which returns a list of tracks known by the server, as specified
/// in [#sendTrackList].
///
/// All other endpoints are assumed to refer to a track, with the path being the track's filename.
///
/// In GET, this returns the contents of the track as specified in [#handleAuthenticatedGet].
///
/// In PUT, this sends the contents of the track to the server as specified in [#handleAuthenticatedPut].
///
/// # Authentication
///
/// All requests must have two headers to be accepted:
/// - `device`, containing the [Device#id] of the device performing the request;
/// - `token`, containing the last [Device#token] assigned to the device (not the one used to log in,
///    but the one assigned after logging in).
///
/// Performing a request to any endpoint **does not** [Device#rerollToken].
///
/// # Encryption
///
/// The same SSL certificate used in [PMPConnection] is also used here, as defined in [Encryption].
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

	/// Handle a GET request after verifying authorization headers.
	///
	/// If the path is "/", calls [#sendTrackList].
	/// Else, attempts to send the requested track responding with the following status codes:
	///
	/// | Status code | Description                                                       |
	/// |:-----------:|-------------------------------------------------------------------|
	/// |         200 | Valid request, response body contains file contents of the track. |
	/// |         404 | Requested file is not in the server's [Library#LIBRARY].          |
	/// |         400 | Found '/' in the path, which is not a legal filename in PMP.      |
	/// |         500 | Requested file is a directory, should never happen.               |
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

			try {
				exchange.sendResponseHeaders(200, file.length());
				Files.copy(file.toPath(), exchange.getResponseBody());
				exchange.close();
			} catch(IOException e) {
				LOGGER.error("(HTTP) IO exception when serving file {} to {}", file, address, e);
				exchange.close();
			}
		}
	}

	/// Handle a PUT request after verifying authorization headers.
	///
	/// The procedure to alter the server's library does not start here, but by sending an [ActionRequest].
	///
	/// Once the server allows the action through an [ActionResponse] where the type is
	/// [ActionResponse.Type#APPROVED], the client can finally perform the request to this endpoint.
	///
	/// | Status code | Description                                                                                           |
	/// |:-----------:|-------------------------------------------------------------------------------------------------------|
	/// |         403 | The device and filename do not match the pending action as specified by [Library#startPendingAction]. |
	/// |         500 | The server was unable to read the request body.                                                       |
	/// |         400 | The request body contains a file that is not parsable as a FLAC file.                                 |
	/// |         200 | Valid request, track has been received successfully and saved to the server's library.                |
	private void handleAuthenticatedPut(HttpExchange exchange, int deviceId) throws IOException {
		LOGGER.info("(HTTP) Authenticated put");
		InetSocketAddress address = exchange.getRemoteAddress();
		String filename = exchange.getRequestURI().getPath().substring(1);

		Device device = null;
		for(var d : ServerStorage.SENSITIVE.devices.get()) {
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

	/// Responds to a `GET /` after verifying authorization headers and path.
	///
	/// Responds with a JSON array containing information about all tracks in the server's library.
	///
	/// Each object in the array is a serialized [Track] object without [Track#file] and [Track#lastModified].
	private void sendTrackList(HttpExchange exchange) throws IOException {
		ObjectMapper om = new ObjectMapper();
		// omit lastModified
		om.addMixIn(Track.class, TrackMixin.class);
		byte[] res = om.writeValueAsString(ServerStorage.MAIN.tracks.values()).getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, res.length);
		OutputStream os = exchange.getResponseBody();
		os.write(res);
		os.close();
	}

	/// Checks the request is coming from an authenticated device and responds as follows if not:
	///
	/// | Status code | Description                                                   |
	/// |:-----------:|---------------------------------------------------------------|
	/// |         401 | The device, token or both headers are missing in the request. |
	/// |         400 | The device header does not contain a number.                  |
	/// |         401 | The token is incorrect.                                       |
	/// |         401 | The device does not exist.                                    |
	///
	/// @return device id if authorized, null if not
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

		boolean authorized = false;
		for(Device device : ServerStorage.SENSITIVE.devices.get()) {
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

	/// Starts the HTTP server in port {@value PMPConnection#DEFAULT_FILE_PORT}
	public static void init() throws IOException {
		LOGGER.info("Initializing transfer server...");
		HttpsServer server = HttpsServer.create(new InetSocketAddress(PMPConnection.DEFAULT_FILE_PORT), 0);
		server.setHttpsConfigurator(new HttpsConfigurator(Encryption.getSslContext()));
		server.createContext("/", new TransferHandler());
		server.start();
	}

	/// Jackson mixin used to omit `lastModified` in [TransferHandler#sendTrackList]
	@JsonIgnoreProperties(value = {"lastModified"})
	private static abstract class TrackMixin {}
}
