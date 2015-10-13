package fieldbox.boxes;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import field.app.RunLoop;
import field.message.MessageQueue;
import field.utility.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Plugin: Watches for properties being changed and then fires change events off to the message bus.
 */
public class Watches extends Box  {

	private final MessageQueue<Quad<Dict.Prop, Box,  Object, Object>, String> messageQueue;
	static public final Dict.Prop<Watches> watches = new Dict.Prop<>("_watches").type().toCannon();

	public Watches(MessageQueue<Quad<Dict.Prop, Box,  Object, Object>, String> messageQueue) {
		this.messageQueue = messageQueue;
		this.properties.putToMap(Boxes.insideRunLoop, "main.__watch_updator__", this::update);
		this.properties.put(watches, this);
	}

	public Watches()
	{
		this.messageQueue = new MessageQueue<Quad<Dict.Prop, Box,  Object, Object>, String>() {
			@Override
			protected Consumer<Boolean> makeQueueServiceThread(BiConsumer<String, Quad<Dict.Prop, Box,  Object, Object>> to) {

				CompletableFuture<Boolean> stop = new CompletableFuture<Boolean>();

				RunLoop.main.getLoop().attach(0, (x) -> {
					try {
						if (this.queue.size() > 0) Log.log("debug.messages", " message queue :" + this.queue.size());

						while (this.queue.peek() != null) {
							Pair<String, Quad<Dict.Prop, Box, Object, Object>> m = this.queue.poll(1, TimeUnit.SECONDS);
							if (m != null && !stop.isDone()) to.accept(m.first, m.second);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});

				return x -> {
				};
			}
		};
		this.properties.putToMap(Boxes.insideRunLoop, "main.__watch_updator__", this::update);
		this.properties.put(watches, this);
	}

	static public final Dict.Prop<LinkedHashMap<Dict.Prop, Object>> watchedPrevious = new Dict.Prop<>("_watchedPrevious");

	SetMultimap<Dict.Prop, String> allWatches = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();

	protected boolean update() {

		breadthFirst(both()).forEach((x) -> {
			LinkedHashMap<Dict.Prop, Object> previous = x.properties.computeIfAbsent(watchedPrevious, (k) -> new LinkedHashMap<>());
			for (Dict.Prop p : allWatches .keySet()) {
				Object was = previous.get(p);
				Object now = x.properties.get(p);

				if (!Util.safeEq(was, now))
				{
					fire(p, x, was, now, allWatches .get(p));
					// fetch it again, fire can change the value of the property
					now = x.properties.get(p);
					previous.put(p, now instanceof Mutable ? ((Mutable)now).duplicate() : now);
				}
			}
		});
		return true;
	}

	public String addWatch(Dict.Prop property, String address)
	{
		allWatches .put(property, address);
		return address;
	}

	public <T> String addWatch(Dict.Prop<T> property, Consumer<Quad<Dict.Prop<T>, Box,  T, T>> c)
	{
		// weakness in Java typing. We can't quite use 'c' below in the messageQueue callback, nor can we cast 'x'.
		Consumer cc = c;

		String address = UUID.randomUUID().toString();
		allWatches .put(property, address);
		messageQueue.register(x -> x.equals(address), x -> {
			cc.accept(x);
		});
		return address;
	}


	private void fire(Dict.Prop p, Box b, Object was, Object now, Collection<String> strings) {
		strings.forEach((address) -> messageQueue.accept(address, new Quad<>(p, b, was, now)));
	}

	public MessageQueue<Quad<Dict.Prop, Box,  Object, Object>, String> getQueue() {
		return messageQueue;
	}
}
