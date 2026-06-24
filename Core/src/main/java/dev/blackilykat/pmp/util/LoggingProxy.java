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

package dev.blackilykat.pmp.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;

/// Class used to proxy [System#out] and [System#err] to be logged through log4j.
public class LoggingProxy {
	private static final Logger LOGGER = LogManager.getLogger(LoggingProxy.class);

	/// Sets up logging proxies for [System#out] and [System#err].
	public static void setUpProxies() {
		System.setOut(proxy(System.out, "(stdout)", Level.INFO));
		System.setErr(proxy(System.err, "(stderr)", Level.ERROR));
	}

	/// Sets up a single logging proxy.
	///
	/// @param stream the original print stream which should be overridden.
	/// @param prefix the prefix added before each logged message to identify this stream.
	/// @param level the log4j logging level which should be used for each message.
	/// @return a new print stream which should be set as the variable which used to contain `stream`.
	/// @see #setUpProxies()
	public static PrintStream proxy(PrintStream stream, String prefix, Level level) {
		final String fPrefix = prefix != null ? prefix : "(stream)";

		return new PrintStream(stream) {
			@Override
			public void print(boolean b) {
				LOGGER.log(level, "{} {}", fPrefix, b);
			}

			@Override
			public void print(char c) {
				LOGGER.log(level, "{} {}", fPrefix, c);
			}

			@Override
			public void print(int i) {
				LOGGER.log(level, "{} {}", fPrefix, i);
			}

			@Override
			public void print(long l) {
				LOGGER.log(level, "{} {}", fPrefix, l);
			}

			@Override
			public void print(float f) {
				LOGGER.log(level, "{} {}", fPrefix, f);
			}

			@Override
			public void print(double d) {
				LOGGER.log(level, "{} {}", fPrefix, d);
			}

			@Override
			public void print(char[] s) {
				LOGGER.log(level, "{} {}", fPrefix, s);
			}

			@Override
			public void print(String s) {
				LOGGER.log(level, "{} {}", fPrefix, s);
			}

			@Override
			public void print(Object obj) {
				if(obj instanceof Throwable ex) {
					LOGGER.log(level, "{} exception", fPrefix, ex);
					return;
				} else if(obj instanceof String str) {
					if(str.startsWith("\tat ")) {
						return;
					}
				}

				LOGGER.log(level, "{} {}", fPrefix, obj);
			}

			@Override
			public void println() {
			}

			@Override
			public void println(boolean x) {
				print(x);
			}

			@Override
			public void println(char x) {
				print(x);
			}

			@Override
			public void println(int x) {
				print(x);
			}

			@Override
			public void println(long x) {
				print(x);
			}

			@Override
			public void println(float x) {
				print(x);
			}

			@Override
			public void println(double x) {
				print(x);
			}

			@Override
			public void println(char[] x) {
				print(x);
			}

			@Override
			public void println(String x) {
				print(x);
			}

			@Override
			public void println(Object x) {
				print(x);
			}
		};
	}
}
