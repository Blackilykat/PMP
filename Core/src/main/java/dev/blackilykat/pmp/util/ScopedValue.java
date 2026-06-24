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

import java.util.NoSuchElementException;
import java.util.Stack;

/// An alternative implementation of [java.lang.ScopedValue] to be able to run on older JREs
public class ScopedValue<T> {
	private ThreadLocal<Stack<T>> local = ThreadLocal.withInitial(() -> new Stack<>());

	private ScopedValue() {
	}

	/// @return the value of the scoped value if bound in the current thread
	///
	/// @throws NoSuchElementException if the scoped value is not bound
	public T get() {
		Stack<T> stack = local.get();
		if(stack.empty()) {
			throw new NoSuchElementException("ScopedValue not bound");
		}
		return stack.peek();
	}

	/// Returns the value of this scoped value if bound in the current thread, otherwise
	/// returns `other`.
	///
	/// @param other the value to return if not bound
	/// @return the value of the scoped value if bound, otherwise `other`
	public T orElse(T other) {
		Stack<T> stack = local.get();
		if(stack.empty()) {
			return other;
		}
		return stack.peek();
	}

	/// Creates a scoped value that is initially unbound for all threads.
	///
	/// @param <T> the type of the value
	/// @return a new [ScopedValue]
	public static <T> ScopedValue<T> newInstance() {
		return new ScopedValue<>();
	}

	/// Create a new scope with the given value.
	///
	/// @param scopedValue the scoped value to set `value` in
	/// @param value the value to set
	/// @return a carrier containing the specified value.
	///
	/// @see java.lang.ScopedValue#where(java.lang.ScopedValue, Object)
	/// @see Carrier#run(Runnable)
	public static <T> Carrier<T> where(ScopedValue<T> scopedValue, T value) {
		return new Carrier<>(scopedValue, value);
	}


	/// Class containing a value to be set.
	public static class Carrier<T> {
		/// The scoped value to add the value in
		public ScopedValue<T> scopedValue;
		/// The value to set
		public T value;

		private Carrier(ScopedValue<T> scopedValue, T value) {
			this.scopedValue = scopedValue;
			this.value = value;
		}

		/// Add [#value] to the scoped value's stack, run the given code and
		/// remove [#value] from the stack.
		public void run(Runnable r) {
			Stack<T> stack = scopedValue.local.get();
			stack.push(value);

			try {
				r.run();
			} finally {
				stack.pop();
			}
		}
	}
}
