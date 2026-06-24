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

import javax.annotation.Nonnull;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/// Any message that expects to receive a [Response].
public abstract class Request extends Message {
	private static int currentRequestId = 0;

	/// A number that uniquely identifies the request for the side that is sending it. Different sides may use the same
	/// request id.
	public Integer requestId = null;

	/// A queue containing the received responses to this request.
	@JsonIgnore
	private BlockingQueue<Response> responses = new LinkedBlockingQueue<>();

	/// The connection this request was sent to.
	@JsonIgnore
	private PMPConnection connection = null;

	public Request() {
		super();
	}

	/// Register a response to this request, allowing it to be handled by [#takeResponse]
	public void addResponse(Response response) {
		responses.add(response);
	}

	/// Takes a response from this message's queue and returns it. If the queue is empty, waits for the next response.
	///
	/// @throws InterruptedException if the thread is interrupted
	/// @throws SocketException if the server disconnects before sending the response or sends an unexpected one
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

	/// Incrementally assign a unique ID to this request.
	public void assignId() {
		requestId = currentRequestId++;
	}

	/// Set the connection this request was sent in.
	@JsonIgnore
	public void setConnection(PMPConnection connection) {
		this.connection = connection;
	}
}
