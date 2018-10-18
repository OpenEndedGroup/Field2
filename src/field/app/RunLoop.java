package field.app;

import field.graphics.Scene;
import fieldbox.execution.Errors;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by marc on 3/25/14.
 */
public class RunLoop {

	static public final RunLoop main = new RunLoop();
	static public final ReentrantLock lock = new ReentrantLock(true);
	static public final ExecutorService workerPool = Executors.newFixedThreadPool(Runtime.getRuntime()
		.availableProcessors() * 2 + 2);
	static public long tick = 0;
	protected final Thread shutdownThread;
	public Scene mainLoop = new Scene();

	public Set<Object> shouldSleep = Collections.synchronizedSet(new LinkedHashSet<>());
	final Thread mainThread;
	List<Runnable> onExit = new LinkedList<>();
	AtomicBoolean exitStarted = new AtomicBoolean(false);

	static public Supplier<Double> time = () -> System.currentTimeMillis() + 0.0d;


	protected RunLoop() {
		mainThread = Thread.currentThread();
		Runtime.getRuntime()
			.addShutdownHook(shutdownThread = new Thread(() -> exit()));
	}

	public Scene getLoop() {
		return mainLoop;
	}

	public boolean isMainThread() {
		return Thread.currentThread() == mainThread;
	}

	long getLock;
	long hasLock;
	long service;
	long mainloop;
	long locksMissed;
	long sleepsTaken;
	long freeMemIn;

	long interval = 100;
	long intervalIn = 0;

	static public boolean printTelemetry = false;

	public void enterMainLoop() {
		if (Thread.currentThread() != mainThread)
			throw new IllegalArgumentException(" cannot enter main loop on non-main thread");

		if (ThreadSync2.getEnabled())
			ThreadSync2.setSync(new ThreadSync2());

		while (true) {
			try {
				tick++;

				long a = System.nanoTime();
				boolean didWork = false;
				if (lock.tryLock(1, TimeUnit.DAYS)) {
					long b = System.nanoTime();

					mainLoop.updateAll();

					long c = System.nanoTime();
					didWork = ThreadSync.get()
						.serviceAndCull();

					if (ThreadSync2.getEnabled()) {
						didWork |= ThreadSync2.getSync()
							.service();
					}

					long d = System.nanoTime();

					getLock += b - a;
					hasLock += d - b;
					service += d - c;
					mainloop += c - b;
				} else {
					locksMissed++;
				}

				if (shouldSleep.size() == 0 && !didWork) {
				//	Thread.sleep(2);
					sleepsTaken++;
				}

				if (tick % interval == 0) {

					if (printTelemetry) {
						System.out.println(
							" a" + (getLock / (double) interval) + " b" + (hasLock / (double) interval) + " c" + (service / (double) interval) + " d" + (mainloop / (double) interval) + " m" + locksMissed + " s" + sleepsTaken);
						System.out.println(" f" + (System.nanoTime() - intervalIn) / interval);
						System.out.println(" m" + (Runtime.getRuntime()
							.freeMemory() - freeMemIn) / interval);

					}
					getLock = 0;
					hasLock = 0;
					service = 0;
					mainloop = 0;
					locksMissed = 0;
					sleepsTaken = 0;
					intervalIn = System.nanoTime();
					freeMemIn = Runtime.getRuntime()
						.freeMemory();
				}

			} catch (Throwable t) {
				System.err.println(" exception thrown in main loop");
				t.printStackTrace();
			} finally {
				RunLoop.lock.unlock();
			}

		}
	}

	public void once(Runnable r) {
		mainLoop.attach(i -> {
			try {
				r.run();
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				return false;
			}

		});
	}

	public static long getTick() {
		return tick;
	}

	public <T> void when(Future<T> f, Consumer<T> a) {
		mainLoop.attach(i -> {
			if (!f.isDone()) return true;

			try {
				T t = f.get();
				a.accept(t);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			return false;
		});

	}

	public void nTimes(Runnable p0, int n) {
		mainLoop.attach(new Scene.Perform() {
			int t = 0;

			@Override
			public boolean perform(int pass) {
				p0.run();
				return t++ < n;
			}
		});
	}

	public void delay(Runnable p0, int ms) {
		long now = System.currentTimeMillis();

		mainLoop.attach(new Scene.Perform() {
			int t = 0;

			@Override
			public boolean perform(int pass) {
				if (System.currentTimeMillis() - now > ms) {
					p0.run();
					return false;
				}
				return true;
			}

		});
	}

	public void delayUntil(Runnable p0, long when) {

		mainLoop.attach(new Scene.Perform() {
			int t = 0;

			@Override
			public boolean perform(int pass) {
				if (System.currentTimeMillis() > when) {
					p0.run();
					return false;
				}
				return true;
			}

		});
	}

	public void delayTicks(Runnable p0, int ticks) {

		mainLoop.attach(new Scene.Perform() {
			int t = 0;

			@Override
			public boolean perform(int pass) {
				if (t++ > ticks) {
					p0.run();
					return false;
				}
				return true;
			}

		});
	}

	public void exit() {
		try {
			if (exitStarted.compareAndSet(false, true)) {
				for (Runnable r : onExit) {
					try {
						r.run();
					} catch (Throwable t) {
						System.err.println(" exception thrown during exit (will continue on regardless)");
						t.printStackTrace();
					}
				}
				if (Thread.currentThread() != shutdownThread) System.exit(0);
			}
		} catch (Throwable t) {
			System.err.println(" unexpected exception thrown during exit ");
			t.printStackTrace();
		}
	}

	/**
	 * adds a Runnable to be executed on exit. This will run before anything else that's been added.
	 */
	public void onExit(Runnable r) {
		// we add this to the start of the list, it will be run before anything that's already there.
		onExit.add(0, r);
	}

}
