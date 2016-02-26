package field.message;

import field.utility.Pair;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MessageQueue<t_message, t_address> {

	ExecutorService handlerDispatch;
	protected BlockingQueue<Pair<t_address, t_message>> queue;
	Consumer<Boolean> queueService;

	public class Handler {
		final Consumer<t_message> handledBy;
		final Predicate<t_address> matchedBy;
		final Object tag;

		protected Handler(Predicate<t_address> address, Consumer<t_message> destination, Object tag) {
			this.matchedBy = address;
			this.handledBy = destination;
			this.tag = tag;
		}

		protected Handler(Predicate<t_address> address, Consumer<t_message> destination) {
			this.matchedBy = address;
			this.handledBy = destination;
			this.tag = null;
		}

		public Callable<Void> call(t_message arguments) {
			return () -> {
				handledBy.accept(arguments);
				return null;
			};
		}
	}

	CopyOnWriteArrayList<Handler> handlers = new CopyOnWriteArrayList<>();

	public MessageQueue() {
		this(() -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2), LinkedBlockingQueue::new);
	}

	public MessageQueue(Supplier<? extends ExecutorService> handlerDispatch, Supplier<? extends BlockingQueue<Pair<t_address, t_message>>> queue) {
		this.handlerDispatch = handlerDispatch.get();
		this.queue = queue.get();
		this.queueService = makeQueueServiceThread(this::dispatch);
	}



	protected Consumer<Boolean> makeQueueServiceThread(BiConsumer<t_address, t_message> to) {
		CompletableFuture<Boolean> stop = new CompletableFuture<Boolean>();
		new Thread(() -> {
			while (!stop.isDone()) {
				try {
					Pair<t_address, t_message> m = queue.poll(1, TimeUnit.SECONDS);
					if (m != null && !stop.isDone()) to.accept(m.first, m.second);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		return x -> stop.complete(x);
	}

	protected void dispatch(t_address a, t_message m) {
		try {
			handlerDispatch.invokeAll(handlers.stream().filter(x -> x.matchedBy.test(a)).map(x -> x.call(m)).collect(Collectors.toList()));
		} catch (InterruptedException e) {
		}
	}

	public void accept(t_address address, t_message message) {
		queue.add(new Pair<>(address, message));
	}

	public void stop() {
		try {
			queueService.accept(true);
		} catch (Exception e) {
		}
	}

	public void register(Predicate<t_address> address, Consumer<t_message> destination, Object tag) {
		deregister(tag);
		handlers.add(new Handler(address, destination, tag));
	}

	public void register(Predicate<t_address> address, Consumer<t_message> destination) {
		handlers.add(new Handler(address, destination));
	}

	public void deregister(Predicate<t_address> a) {
		handlers.removeAll(handlers.stream().filter(x -> x.matchedBy.equals(a)).collect(Collectors.toList()));
	}

	public void deregister(Consumer<t_message> a) {
		handlers.removeAll(handlers.stream().filter(x -> x.handledBy.equals(a)).collect(Collectors.toList()));
	}

	public void deregister(Object tag) {
		handlers.removeAll(handlers.stream().filter(x -> tag.equals(x.tag)).collect(Collectors.toList()));
	}
}
