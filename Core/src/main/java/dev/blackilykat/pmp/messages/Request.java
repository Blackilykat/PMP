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

package dev.blackilykat.pmp.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.event.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Any message that expects to receive a {@link Response}.
 */
public abstract class Request extends Message {
	private static final Logger LOGGER = LogManager.getLogger(Request.class);
	private static int currentRequestId = 0;
	/**
	 * A number that uniquely identifies the request for the side that is sending it. Different sides may use the same
	 * request id.
	 */
	public Integer requestId = null;

	@JsonIgnore
	private BlockingQueue<Response> responses = new LinkedBlockingQueue<>();

	@JsonIgnore
	private PMPConnection connection = null;

	public Request() {
		super();
	}

	public void addResponse(Response response) {
		responses.add(response);
	}

	/**
	 * Takes a response from this message's queue and returns it. If the queue is empty, waits for the next response.
	 *
	 * @throws InterruptedException if the thread is interrupted
	 * @throws SocketException if the server disconnects before sending the response or sends an unexpected one
	 */
	public @Nonnull <T extends Response> T takeResponse() throws InterruptedException, SocketException {
		assert connection != null;
		AtomicBoolean disconnected = new AtomicBoolean(false);

		Thread currentThread = Thread.currentThread();

		Listener<Void> disconnectListener = _ -> {
			disconnected.set(true);
			currentThread.interrupt();
		};
		connection.eventDisconnected.register(disconnectListener);

		try {
			Response response = responses.take();
			//noinspection unchecked
			return (T) response;
		} catch(InterruptedException e) {
			if(disconnected.get()) {
				throw new SocketException(connection.name + " disconnected before sending response");
			}
			throw e;
		} catch(ClassCastException e) {
			throw new SocketException(connection.name + " sent unexpected response type: " + e.getMessage());
		} finally {
			connection.eventDisconnected.unregister(disconnectListener);
		}
	}

	public void assignId() {
		requestId = currentRequestId++;
	}

	@JsonIgnore
	public void setConnection(PMPConnection connection) {
		this.connection = connection;
	}
}