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

package dev.blackilykat.pmp.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Single thread executor service which interrupts a previous task whenever a new one is submitted.
 * <p>
 * Relies on submitted tasks to properly handle interrupts.
 */
@SuppressWarnings({"NullableProblems", "unchecked"})
public class OverridingSingleThreadExecutor implements ExecutorService {
	private final ExecutorService base;
	private Future<?> currentTask = null;

	public OverridingSingleThreadExecutor() {
		base = Executors.newSingleThreadExecutor();
	}

	@Override
	public void shutdown() {
		base.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return base.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return base.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return base.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return base.awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		interrupt();
		return (Future<T>) (currentTask = base.submit(task));
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		interrupt();
		return (Future<T>) (currentTask = base.submit(task, result));
	}

	@Override
	public Future<?> submit(Runnable task) {
		interrupt();
		return currentTask = base.submit(task);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
		return invokeAll(tasks, Long.MAX_VALUE, TimeUnit.DAYS);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
		Map<Callable<T>, CompletableFuture<T>> futureMap = new HashMap<>();
		List<Future<T>> futures = new LinkedList<>();
		for(Callable<T> task : tasks) {
			CompletableFuture<T> future = new CompletableFuture<>();
			futureMap.put(task, future);
			futures.add(future);
		}

		submit(() -> {
			for(var entry : futureMap.entrySet()) {
				if(Thread.interrupted()) {
					futureMap.values().forEach(otherFuture -> {
						otherFuture.cancel(false);
					});
					break;
				}

				Callable<T> callable = entry.getKey();
				CompletableFuture<T> future = entry.getValue();
				try {
					T result = callable.call();
					future.complete(result);
				} catch(InterruptedException e) {
					future.completeExceptionally(e);
					futureMap.values().forEach(otherFuture -> {
						otherFuture.cancel(false);
					});
					break;
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});

		CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0])).orTimeout(timeout, unit).join();

		return futures;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
		try {
			return invokeAny(tasks, Long.MAX_VALUE, TimeUnit.DAYS);
		} catch(TimeoutException e) {
			// unreachable
			throw new ExecutionException(e);
		}
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws ExecutionException, InterruptedException, TimeoutException {

		CompletableFuture<T> future = new CompletableFuture<>();

		submit(() -> {
			if(tasks.isEmpty()) {
				future.completeExceptionally(new ExecutionException(null));
			}

			Callable<T> task = tasks.iterator().next();
			try {
				T result = task.call();
				future.complete(result);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});

		return future.get(timeout, unit);
	}

	@Override
	public void execute(Runnable command) {
		submit(command);
	}

	public void interrupt() {
		if(currentTask != null) {
			currentTask.cancel(true);
		}
	}
}
