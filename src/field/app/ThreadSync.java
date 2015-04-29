package field.app;

import com.google.common.collect.MapMaker;
import field.nashorn.internal.runtime.Undefined;
import field.utility.Dict;
import field.utility.Options;
import field.utility.Util;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Yield with (an unbounded but recycled number of) threads.
 * <p>
 * We used to use a wonderful homemade byte-code rewriting based trick to implement 'yield' for annotated methods. This trick takes a very different path, it's less space and time efficient (the
 * context switch into the method is much slower (across threads)), but it's _completely_ language independent and it's easy to pause and resume from arbitrarily nested functions.
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
	public Thread mainThread;
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

	static public Supplier nothing() {
		return constant("nothing");
	}

	static public <V> Consumer<V> toCollection(Collection<V> to) {
		return (v) -> to.add(v);
	}

	static public Object yield(Object o) throws InterruptedException, Stop {
		if (fiber.get() == null) throw new IllegalArgumentException(" yield called from non-fiber thread");
		if (fiber.get().stopped) throw new Stop();
		if (o == null) o = NULL;

		fiber.get().output.put(o);
		if (fiber.get().stopped) throw new Stop();
		Object t = fiber.get().input.take();

		if (fiber.get().stopped) throw new Stop();
		return t;
	}

	static public Object wait(int n) throws InterruptedException, Stop {
		Object q = null;
		for(int i=0;i<n;i++)
		{
			q = yield(null);
		}
		return q;
	}

	static public Object pause(Supplier<Object> during) throws InterruptedException, Stop {
		if (fiber.get() == null) throw new IllegalArgumentException(" yield called from non-fiber thread");
		if (fiber.get().stopped) throw new Stop();

		fiber.get().paused = () -> isFalse(during.get());
		fiber.get().output.put(NULL);

		if (fiber.get().stopped) throw new Stop();
		Object t = fiber.get().input.take();

		if (fiber.get().stopped) throw new Stop();
		return t;
	}

	private static Object isFalse(Object o) {

		System.out.println(" isFalse :"+o);

		if (o==null) return true;
		if (o instanceof Collection) return o;
		if (o instanceof Boolean) return !((Boolean)o).booleanValue();

		// !
		if (o instanceof Undefined) return true;

		return false;
	}

	public <K, V> Fiber<K, V> run(Supplier<K> in, Consumer<V> out, Callable<V> r, Consumer<Throwable> h) throws InterruptedException {
		Fiber<K, V> f = new Fiber<>();
		live.add(f);
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
				      .setName("running fiber");
				Thread.currentThread().setContextClassLoader(loader);
				fiber.set(f);
				f.input.take();
				Object a = r.call();
				if (a == null) f.output.put(NULL);
				else f.output.put(a);

				return a;
			} catch (Stop s) {
				f.output.put(NULL);
				f.runner.cancel(true);
				return null;
			} catch (Throwable t) {
				f.exception = t;
				System.err.println(" -- caught throwable ");
				f.output.put(NULL);
				t.printStackTrace();
				System.err.println(" -- rethrowing <" + f.output.peek() + "> -> " + f.handler);
				if (f.output.peek() == null) f.output.put(NULL);
				f.runner.cancel(true);
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

		Object o = f.output.take();
		if (f.runner.isDone()) live.remove(this);
		if (o == NULL) o = null;
		out.accept((V) o);
		f.lastReturn = (V) o;
		if (f.exception != null) if (f.handler == null) throw new RuntimeException(f.exception);
		else f.handler.accept(f.exception);

		System.out.println(" returning, exception is :" + f.exception);

		return f;
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

	public Fiber run(Callable r, Consumer<Throwable> h) throws InterruptedException {
		return run(nothing(), discard(), r, h);
	}

	public Fiber run(Callable r) throws InterruptedException {
		return run(nothing(), discard(), r, t -> {
			throw new RuntimeException(t);
		});
	}

	public boolean serviceAndCull() throws InterruptedException {
		threadingModel.set(this);
		Iterator<Fiber> i = live.iterator();
		Set<Fiber> repost = new LinkedHashSet<>();
		while (i.hasNext()) {
			Fiber f = i.next();

			if (f.runner.isDone()) {
				f.wasPaused = false;
				if (f.exception != null) {
					i.remove();
					if (f.handler != null) f.handler.accept(f.exception);
					else throw new IllegalStateException(f.exception);
				} else {

					Object o = f.output.poll();
					if (o != null) {
						f.out.accept(o);
						f.lastReturn = o;
					}
					i.remove();
				}
			} else {
				System.out.println(" -- getting paused for :"+f);
				Object p = f.paused.get();
				f.wasPaused = true;

				if (p==null)
					continue;

				if (p instanceof Collection)
				{
					if (((Collection)p).size()==0)
					{
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
				};

				f.wasPaused = false;


				Object o = f.in.get();
				if (o == null) o = NULL;
				f.input.put(o);
				o = f.output.take();
				if (o == NULL) o = null;
				f.out.accept(o);
				if (f.exception != null) {
					i.remove();
					if (f.handler != null) f.handler.accept(f.exception);
					else throw new IllegalStateException(f.exception);
				}
			}
		}

		live.removeAll(repost);
		live.addAll(repost);
		return live.size() > 0;
	}

	public List<Fiber> getFibers() {
		return new ArrayList<>(live);
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
		public final BlockingQueue output = new LinkedBlockingDeque<>(2);
		public final BlockingQueue input = new LinkedBlockingDeque<>(2);
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
	}

	public Stream<Fiber> findByTag(Object tag)
	{
		return live.stream().filter(x -> Util.safeEq(x, tag));
	}

}
