package field.app;

import com.google.common.collect.MapMaker;
import field.utility.Dict;
import field.utility.Options;
import field.utility.Util;
import fieldbox.execution.Execution;
//import jdk.nashorn.internal.runtime.Undefined;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Yield with (an unbounded but recycled number of) threads.
 * <p>
 * We used to use a wonderful homemade byte-code rewriting based trick to implement 'yield' for annotated methods. This trick takes a very different path, it's less space and time efficient (the
 * context switch into the method is much slower (across threads)), but it's _completely_ language independent and it's easy to pause and resume from arbitrarily nested functions.
 *
 * TODO: make Fiber finalizable and send a stop() externally to it when it's done.
 * TODO: finish making _r = wrap( () => {}) work
 */
public class ThreadSync {
	static public final boolean enabled = Options.dict()
		.isTrue(new Dict.Prop("threaded"), false);
	static private final ExecutorService executor = Executors.newCachedThreadPool();
	static public Object NULL = new Object();
	static ThreadLocal<Fiber> fiber = new ThreadLocal<>();
	static ThreadLocal<ThreadSync> threadingModel = new ThreadLocal<>();
	static private Map<Thread, ThreadSync> models = new MapMaker().weakKeys()
		.makeMap();
	static public Thread mainThread;
	Set<Fiber> live = new LinkedHashSet<Fiber>();

	// this should be getThreadingModelForCurrentThread();
	protected ThreadSync() {
		mainThread = Thread.currentThread();
	}

	static public ThreadSync get() {
		return models.computeIfAbsent(Thread.currentThread(), k -> new ThreadSync());
	}

	static public <K> Supplier<K> input(Iterator<K> a) {
		return () -> {
			if (a.hasNext()) {
				K k = a.next();
				if (k == null) throw new Stop("input returned null");
				return a.next();
			}
			throw new Stop();
		};
	}

	static public <T> T callInMainThreadAndWait(Callable<T> c) throws Exception {
		if (Thread.currentThread() == get().mainThread) {
			return c.call();
		} else {
			CompletableFuture<T> f = new CompletableFuture<>();
			RunLoop.main.once(() -> {
				try {
					f.complete(c.call());
				} catch (Throwable t) {
					f.completeExceptionally(t);
				}
			});

			// should be rejoin?

			while (!f.isDone()) {
				ThreadSync.yield(true);
			}
			return f.get();
		}
	}


	static public <K> Supplier<K> input(Iterable<K> a) {
		return input(a.iterator());
	}

	static public <K> Supplier<K> constant(K k) {
		return () -> k;
	}

	static public <K> Consumer<K> discard() {
		return (x) -> {
		};
	}

	static public Supplier NULL() {
		return constant("NULL");
	}

	static public <V> Consumer<V> toCollection(Collection<V> to) {
		return (v) -> to.add(v);
	}

	static public Object yield(Object o) throws Stop {
		if (fiber.get() == null)
			throw new IllegalArgumentException(" yield called from non-fiber thread");

		fiber.get().debugStatus = "yield from " + Arrays.asList(new Exception().getStackTrace());

		if (fiber.get().stopped) throw new Stop();
		if (o == null) o = NULL;

		try {
			fiber.get().output.put(o);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (fiber.get().stopped) throw new Stop();

		Object t = null;
		try {
			t = fiber.get().input.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			try {
				t = fiber.get().input.take();
				System.out.println(" recovered with :" + t);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				System.err.println(" -- giving up --");
			}
		}

		if (fiber.get().stopped) throw new Stop();
		return t == NULL ? null : t;
	}

	static public Object wait(int n) throws InterruptedException, Stop {
		Object q = null;
		for (int i = 0; i < n; i++) {
			q = yield(null);
		}
		return q;
	}

	static public Object pause(Supplier<Object> during) throws InterruptedException, Stop {
		if (fiber.get() == null) throw new IllegalArgumentException(" pause called from non-fiber thread");
		if (fiber.get().stopped) throw new Stop();

		fiber.get().paused = () -> isFalse(during.get());
		fiber.get().output.put(NULL);

		if (fiber.get().stopped) throw new Stop();
		Object t = fiber.get().input.take();

		if (fiber.get().stopped) throw new Stop();
		return t;
	}

	private static Object isFalse(Object o) {

		if (o == null) return true;
		if (o instanceof Collection) return o;
		if (o instanceof Boolean) return !((Boolean) o).booleanValue();

		return null;

	}


	public <K, V> Fiber<K, V> run(String tag, Supplier<K> in, Consumer<V> out, Callable<V> r, Consumer<Throwable> h) throws InterruptedException {
		Fiber<K, V> f = new Fiber<>();
		live.add(f);
		f.debugDescription = tag;
		f.handler = h;
		f.in = in;
		f.input.put(in.get());
		f.out = out;
		threadingModel.set(this);
		ClassLoader loader = Thread.currentThread()
			.getContextClassLoader();

		f.runner = executor.submit(() -> {

			try {
				Thread.currentThread()
					.setName("" + f.debugDescription);
				Thread.currentThread()
					.setContextClassLoader(loader);

				fiber.set(f);
				f.input.take();
				Object a = r.call();
				if (a == null) f.output.put(NULL);
				else f.output.put(a == null ? NULL : a);

				return a;
			} catch (Stop s) {
				f.runner.cancel(true);
				f.output.put(NULL);
				return null;
			} catch (Throwable t) {
				f.exception = t;
				f.runner.cancel(true);
				System.err.println(" -- caught throwable ");
				f.output.put(NULL);
				t.printStackTrace();
				System.err.println(" -- rethrowing <" + f.output.peek() + "> -> " + f.handler);
				if (f.output.peek() == null) f.output.put(NULL);
				throw t;
			} finally {
				// need to unwide sub fibers to allow recursion.
				if (models.containsKey(Thread.currentThread())) {
					ThreadSync m = ThreadSync.get();
					m.shutdown();
				}
				fiber.set(null);
			}
		});

		Object o = debugTake(f);

		if (f.runner.isDone()) live.remove(this);
		if (o == NULL) o = null;
		out.accept((V) o);
		f.lastReturn = o == NULL ? null : (V) o;
		if (f.exception != null) if (f.handler == null) throw new RuntimeException(f.exception);
		else f.handler.accept(f.exception);

		return f;
	}

	private <K, V> Object debugTake(Fiber<K, V> f) {

		Object o = null;
		try {

			while (o == null) {
				if (f.runner.isDone()) return f.output.poll();
				o = f.output.poll(1, TimeUnit.SECONDS);
				if (o == null) {
					System.out.println(" debugTake failed to get a result, we are hanging the main thread waiting on:");
					System.out.println(f + " " + f.debugDescription + " " + f.debugStatus);
					System.out.println(f.stopped + " " + f.runner + " " + f.runner.isCancelled() + " " + f.runner.isDone());
					System.out.println(" meanwhile :");
					for (Fiber ll : live) {
						System.out.println(ll + " " + ll.debugDescription + " " + ll.debugStatus + " " + ll.runner + " " + ll.runner.isDone() + " " + ll.runner.isCancelled());
					}
				}
			}

			return o;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	private void shutdown() throws InterruptedException {
		for (Fiber f : live) {
			f.stopped = true;
			f.runner.cancel(true);
			serviceAndCull();
		}

		live.clear();
		models.remove(Thread.currentThread());
	}

	public Fiber run(String tag, Callable r, Consumer<Throwable> h) throws InterruptedException {
		return run(tag, NULL(), discard(), r, h);
	}

	public Fiber run(String tag, Callable r) throws InterruptedException {
		return run(tag, NULL(), discard(), r, t -> {
			throw new RuntimeException(t);
		});
	}

	public boolean serviceAndCull() throws InterruptedException {
		threadingModel.set(this);

		if (live.size() > 0)
			System.err.println(" -- serviceAndCull, status of " + live.size() + " fibers");


		Iterator<Fiber> i = live.iterator();
		Set<Fiber> repost = new LinkedHashSet<>();
		while (i.hasNext()) {
			Fiber f = i.next();

			System.err.println("         " + f + " runner done ? " + f.runner.isDone() + " / " + f.runner.isCancelled() + " paused ? " + f.paused);
			if (f.runner.isDone()) {
				f.wasPaused = false;
				if (f.exception != null) {
					i.remove();
					if (f.handler != null) f.handler.accept(f.exception);
					else throw new IllegalStateException(f.exception);
				} else {

					System.err.println("                        done, polling one more time");
					Object o = f.output.poll();
					System.out.println(" got :" + o);
					if (o != null) {
						f.out.accept(o);
						f.lastReturn = o;
					}
					System.out.println("                         that's it for this fiber");
					i.remove();
				}
			} else {
				Object p = f.paused.get();

				System.out.println("                     pause status is :" + p);

				f.wasPaused = true;

				if (p == null) continue;

				if (p instanceof Collection) {
					if (((Collection) p).size() == 0) {
						repost.add(f);
						continue;
					}
					Iterator q = ((Collection) p).iterator();
					q.next();
					q.remove();
				}

				if (p instanceof Boolean) {
					if (((Boolean) p).booleanValue()) {
						repost.add(f);
						continue;
					}
				}

				f.wasPaused = false;

				System.out.println("                     in...");

				Object o = f.in.get();
				if (o == null) o = NULL;
				System.out.println("                     =" + o);
				f.input.put(o);
				o = debugTake(f);
				if (o == NULL) o = null;
				System.out.println("                     take=" + o);
				f.out.accept(o);
				System.out.println("                     exception?" + f.exception);
				if (f.exception != null) {
					i.remove();
					if (f.handler != null) f.handler.accept(f.exception);
					else throw new IllegalStateException(f.exception);
				}
			}
		}

		if (live.size() > 0)
			System.out.println(" live was :" + live.size());

		live.removeAll(repost);
		live.addAll(repost);
		if (live.size() > 0)
			System.out.println(" now " + live.size() + " / " + repost.size());
		return live.size() > 0;
	}

	public List<Fiber> getFibers() {
		return new ArrayList<>(live);
	}

	public Stream<Fiber> findByTag(Object tag) {
		return live.stream()
			.filter(x -> Util.safeEq(x, tag));
	}

	public static class Stop extends RuntimeException {

		public Stop() {
		}

		public Stop(String message) {
			super(message);
		}
	}

	static public class Collect<V> implements Consumer<V> {
		List<V> items = new ArrayList<>();

		@Override
		public void accept(V v) {
			items.add(v);
		}

		public List<V> getItems() {
			return items;
		}
	}

	public class Fiber<K, V> {
		public final BlockingQueue output = new LinkedBlockingDeque<>(1);
		public final BlockingQueue input = new LinkedBlockingDeque<>(1);
		public Supplier<K> in;
		public Consumer<V> out;

		public Throwable exception;
		public Consumer<Throwable> handler;

		public Future runner;
		public V lastReturn;
		public volatile boolean stopped = false;
		public volatile Supplier<Boolean> paused = () -> false;
		public volatile boolean wasPaused = false;

		public Object tag;
		public String debugDescription;
		public String debugStatus;

		@Override
		public String toString() {
			return "fiber:" + debugDescription;
		}
	}

	static public <T> Callable<T> wrap(Callable<T> h) {
		return wrap(h, (a, b) -> a, () -> {
		});
	}

	static public Callable<Boolean> wrapBoolean(Callable h) {
		return wrap(h, (a, b) -> !b && (a!=null && ((Boolean)a).booleanValue()), () -> {
		});
	}

	static public <T> Callable<T> wrap(Callable<T> h, BiFunction<T, Boolean, T> process, Runnable notifyEnded) {
		return new Callable<T>() {
			boolean first = true;
			boolean ended = false;
			Fiber fiber;

			@Override
			public T call() throws Exception {

				if (first && !ended) {
					first = false;
					fiber = models.get(mainThread).run("" + h, h);

					if (Execution.context.get().size()>0)
						fiber.tag = Execution.context.get().peek();
					fiber.paused = () -> true;

					return (T) process.apply((T) fiber.lastReturn, fiber.runner.isDone());
				}

				fiber.paused = () -> {
					fiber.paused = () -> true;
					return false;
				};

				if (!ended && fiber.runner.isDone()) {
					ended = true;
					notifyEnded.run();
				}

				return (T) process.apply((T) fiber.lastReturn, fiber.runner.isDone());
			}

//			public void accept(Boolean willContinue) {
//				if (!willContinue) {
//					ended = true;
//					if (fiber != null) {
//						fiber.stopped = true;
//					}
//				}
//			}
		};

	}

}
