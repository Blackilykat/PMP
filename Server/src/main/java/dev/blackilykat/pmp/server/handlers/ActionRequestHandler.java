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

package dev.blackilykat.pmp.server.handlers;

import dev.blackilykat.pmp.MessageHandler;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.messages.ActionMessage;
import dev.blackilykat.pmp.messages.ActionRequest;
import dev.blackilykat.pmp.messages.ActionResponse;
import dev.blackilykat.pmp.messages.ErrorMessage;
import dev.blackilykat.pmp.server.ClientConnection;
import dev.blackilykat.pmp.server.Device;
import dev.blackilykat.pmp.server.Library;
import dev.blackilykat.pmp.server.ServerStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ActionRequestHandler extends MessageHandler<ActionRequest> {
	private static final Logger LOGGER = LogManager.getLogger(ActionRequestHandler.class);
	private static final BlockingQueue<QueueEntry> QUEUE = new LinkedBlockingQueue<>();


	public ActionRequestHandler() {
		super(ActionRequest.class);
	}

	@Override
	public void run(PMPConnection pmpConn, ActionRequest message) {
		if(!(pmpConn instanceof ClientConnection connection)) {
			return;
		}

		pmpConn.send(new ActionResponse(message.requestId, ActionResponse.Type.QUEUED, null));
		QUEUE.add(new QueueEntry(connection, message));
	}

	@Override
	public void register() {
		super.register();

		new Thread("Action request handler thread") {
			@Override
			public void run() {
				try {
					while(!Thread.interrupted()) {
						QueueEntry entry = QUEUE.take();
						ClientConnection connection = entry.connection;
						ActionRequest request = entry.request;

						if(!connection.connected) {
							continue;
						}

						switch(request.action.actionType) {
							case ADD -> handleAddRequest(connection, request);
							case REMOVE -> handleRemoveRequest(connection, request);
							case REPLACE -> handleReplaceRequest(connection, request);
							case CHANGE_METADATA -> handleChangeMetadataRequest(connection, request);
							default -> {
								LOGGER.error("(ActionRequestHandler#register) this should've been unreachable, type "
										+ "is {}", request.action.actionType);
							}
						}
					}
				} catch(InterruptedException _) {
				}
				LOGGER.info("Action request handler thread interrupted");
			}
		}.start();
	}

	private static void handleAddRequest(ClientConnection connection, ActionRequest request)
			throws InterruptedException {
		File target = Library.LIBRARY.toPath().resolve(request.action.filename).toFile();
		if(target.exists()) {
			LOGGER.warn("Got request to add existing file {}", target);
			connection.send(new ActionResponse(request.requestId, ActionResponse.Type.INVALID, null));
			return;
		}
		// after the check above, all logic is the same. When the action is stored, the type is kept from the request
		// and is not overridden by calling this method.
		handleReplaceRequest(connection, request);
	}

	private static void handleReplaceRequest(ClientConnection connection, ActionRequest request)
			throws InterruptedException {
		while(!Library.setPendingActionIfPossible(request.action, connection.device)) {
			Library.waitForFreePendingAction();
		}
		connection.send(new ActionResponse(request.requestId, ActionResponse.Type.APPROVED, null));
		Library.onSuccessfulAction(() -> {
			int id = ServerStorage.MAIN.actions.size();
			connection.send(new ActionResponse(request.requestId, ActionResponse.Type.COMPLETED, id));
			Device.broadcastExcept(new ActionMessage(request.action, id), connection.device);
			ServerStorage.MAIN.actions.add(request.action);
		});
	}

	private static void handleRemoveRequest(ClientConnection connection, ActionRequest request) {
		try {
			Library.remove(request.action.filename);
		} catch(FileNotFoundException e) {
			LOGGER.warn("Got request to remove non-existent file");
			connection.send(new ActionResponse(request.requestId, ActionResponse.Type.INVALID, null));
			return;
		} catch(IOException e) {
			LOGGER.error("Failed to remove track {}, were file permissions messed with?", request.action.filename);
			// make the client move on without pretending the action is completed
			connection.send(new ActionResponse(request.requestId, ActionResponse.Type.INVALID, null));
			return;
		}

		int id = ServerStorage.MAIN.actions.size();
		connection.send(new ActionResponse(request.requestId, ActionResponse.Type.COMPLETED, id));
		Device.broadcastExcept(new ActionMessage(request.action, id), connection.device);
		ServerStorage.MAIN.actions.add(request.action);
	}

	private static void handleChangeMetadataRequest(ClientConnection connection, ActionRequest request) {
		connection.send(new ActionResponse(request.requestId, ActionResponse.Type.INVALID, null));
		connection.send(new ErrorMessage("Changing metadata is not supported by the server yet."));
	}

	private record QueueEntry(ClientConnection connection, ActionRequest request) {}
}
