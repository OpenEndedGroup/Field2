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
    static public long frameTime = 0;
    static public boolean printTelemetry = false;
    static private long timeStart = 0;
    static public Supplier<Double> time = () -> (System.nanoTime() - timeStart) / 1000000.0;
    protected final Thread shutdownThread;
    public Scene mainLoop = new Scene();
    public Set<Object> shouldSleep = Collections.synchronizedSet(new LinkedHashSet<>());
    public Vector<Runnable> serviceVector = new Vector();
    public Hashtable<String, Runnable> serviceMap = new Hashtable();
    Thread mainThread;
    List<Runnable> onExit = new LinkedList<>();
    AtomicBoolean exitStarted = new AtomicBoolean(false);
    long getLock;
    long hasLock;
    long service;
    long mainloop;
    long locksMissed;
    long sleepsTaken;
    long freeMemIn;
    long interval = 100;
    long intervalIn = 0;
    Object eventLock = new Object();

    protected RunLoop() {
        mainThread = Thread.currentThread();
        Runtime.getRuntime()
                .addShutdownHook(shutdownThread = new Thread(() -> exit()));
    }

    public static long getTick() {
        return tick;
    }

    public Scene getLoop() {
        return mainLoop;
    }

    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    public void enterMainLoop() {
        if (Thread.currentThread() != mainThread)
            throw new IllegalArgumentException(" cannot enter main loop on non-main thread");

        if (ThreadSync2.getEnabled())
            ThreadSync2.setSync(new ThreadSync2());


        mainThread = Thread.currentThread();

        timeStart = System.nanoTime();
        while (true) {
            try {
                tick++;

                long a = System.nanoTime();
                frameTime = a - timeStart;

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

                    try {
                        synchronized (eventLock) {
                            eventLock.wait(5);
                        }
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }

                    sleepsTaken++;
                }

                if (tick % interval == 0) {

                    if (printTelemetry) {
                        System.out.println(
                                " a" + (getLock / (double) interval) + " b" + (hasLock / (double) interval) + " c" + (service / (double) interval) + " d" + (mainloop / (double) interval) + " m" + locksMissed + " s" + sleepsTaken);
                        System.out.println(" f" + interval / ((System.nanoTime() - intervalIn) / (1000000000.0)));
                        System.out.println(" m" + (Runtime.getRuntime()
                                .freeMemory() - freeMemIn) / interval);

                        if (((System.nanoTime() - intervalIn) / (1000000000.0)) < 1 && interval < 5000)
                            interval *= 2;

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


                try {
                    Vector<Runnable> cp = new Vector(serviceVector);
                    serviceVector.clear();
                    cp.forEach(x -> {
                        try {
                            x.run();
                        } catch (Throwable t) {
                            System.err.println(" exception thrown in service vector " + x);
                            t.printStackTrace();
                        }
                    });

                    Hashtable<String, Runnable> ch = new Hashtable<>(serviceMap);
                    serviceMap.clear();
                    ch.values().forEach( x -> {
                        try {
                            x.run();
                        } catch (Throwable t) {
                            System.err.println(" exception thrown in service map " + x);
                            t.printStackTrace();
                        }
                    });


                } catch (Throwable t) {
                    System.err.println(" exception thrown in service vector init ");
                    t.printStackTrace();
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
        serviceVector.add(() -> {
            mainLoop.attach(i -> {
                try {
                    r.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    return false;
                }

            });
        });
    }

    public <T> void when(Future<T> f, Consumer<T> a) {
        serviceVector.add(() -> {
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
        });

    }

    public <T> void whenNamed(String name, Future<T> f, Consumer<T> a) {
        serviceMap.put(name, () -> {
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
        });

    }

    public void nTimes(Runnable p0, int n) {
        serviceVector.add(() -> {
            mainLoop.attach(new Scene.Perform() {
                int t = 0;

                @Override
                public boolean perform(int pass) {
                    p0.run();
                    return t++ < n;
                }
            });
        });
    }

    public void delay(Runnable p0, int ms) {
        long now = System.currentTimeMillis();

        serviceVector.add(() -> {
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
        });
    }

    public void delayUntil(Runnable p0, long when) {

        serviceVector.add(() -> {
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
        });
    }

    public void delayTicks(Runnable p0, int ticks) {

        serviceVector.add(() -> {
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

    public void interrupt() {
        synchronized (eventLock) {
            eventLock.notifyAll();
        }
    }

}
