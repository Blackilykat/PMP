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

/**
 * A temporary implementation of java.lang.ScopedValue to be able to run on older JREs
 */
public class ScopedValue<T> {
	private ThreadLocal<Stack<T>> local = ThreadLocal.withInitial(() -> new Stack<>());

	private ScopedValue() {
	}

	public T get() {
		Stack<T> stack = local.get();
		if(stack.empty()) {
			throw new NoSuchElementException("ScopedValue not bound");
		}
		return stack.peek();
	}

	public T orElse(T other) {
		Stack<T> stack = local.get();
		if(stack.empty()) {
			return other;
		}
		return stack.peek();
	}

	public static <T> ScopedValue<T> newInstance() {
		return new ScopedValue<>();
	}

	public static <T> Carrier<T> where(ScopedValue<T> scopedValue, T value) {
		return new Carrier<>(scopedValue, value);
	}

	public static class Carrier<T> {
		public ScopedValue<T> scopedValue;
		public T value;

		private Carrier(ScopedValue<T> scopedValue, T value) {
			this.scopedValue = scopedValue;
			this.value = value;
		}

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
