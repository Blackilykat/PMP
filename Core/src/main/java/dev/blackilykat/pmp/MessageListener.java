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

package dev.blackilykat.pmp;

import dev.blackilykat.pmp.messages.Message;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class used to listen to messages from a specific connection. These can be unregistered at any point after being
 * registered.
 * <p>
 * Listeners can be used to wait for an expected response to a message, or for intercepting a message before it gets
 * handled.
 * <p>
 * Listeners run before {@link MessageHandler}s and have the ability to cancel messages. Once a message is cancelled it
 * will not be handled, but other listeners will still run and have the ability to override the cancelled status.
 *
 * @see MessageHandler
 */
public abstract class MessageListener<T extends Message> {
	public final Class<T> type;

	public MessageListener(Class<T> type) {
		this.type = type;
	}

	/**
	 * Run the listener casting Message to the generic type.
	 *
	 * @throws ClassCastException if the type is incorrect
	 */
	@SuppressWarnings("unchecked")
	public void runCasting(Message message, AtomicBoolean cancelled) {
		run((T) message, cancelled);
	}

	public abstract void run(T message, AtomicBoolean cancelled);
}
