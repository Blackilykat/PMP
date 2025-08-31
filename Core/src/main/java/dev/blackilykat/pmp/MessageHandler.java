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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Class used to define handling logic for messages.
 * <p>
 * Handlers are defined once throughout the program and will get called for every connection.
 * <p>
 * Once a handler is register, it cannot be unregistered.
 *
 * @see MessageListener
 */
public abstract class MessageHandler<T extends Message> {
	static final List<MessageHandler<?>> registeredHandlers = new LinkedList<>();

	private static final Logger LOGGER = LogManager.getLogger(MessageHandler.class);
	public final Class<T> type;

	public MessageHandler(Class<T> type) {
		this.type = type;
	}

	/**
	 * Run the handler casting Message to the generic type.
	 *
	 * @throws ClassCastException if the type is incorrect
	 */
	@SuppressWarnings("unchecked")
	public void runCasting(PMPConnection connection, Message message) {
		run(connection, (T) message);
	}

	public abstract void run(PMPConnection connection, T message);

	public void register() {
		if(registeredHandlers.contains(this)) {
			throw new IllegalStateException("Handler is already registered");
		}
		registeredHandlers.add(this);
		LOGGER.debug("Registered {} handler", type.getName());
	}
}
