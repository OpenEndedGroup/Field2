package fieldbox.boxes;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import field.message.MessageQueue;
import field.utility.Dict;
import field.utility.Triple;
import field.utility.Util;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * watches for properties being changed and then fires change events off to the message bus
 */
public class Watches extends Box  {

	private final MessageQueue<Triple<Dict.Prop, Object, Object>, String> messageQueue;

	public Watches(MessageQueue<Triple<Dict.Prop, Object, Object>, String> messageQueue) {
		this.messageQueue = messageQueue;
		this.properties.putToMap(Boxes.insideRunLoop, "__watch_updator__", this::update);
	}

	static public final Dict.Prop<LinkedHashMap<Dict.Prop, Object>> watchedPrevious = new Dict.Prop<>("_watchedPrevious");

	SetMultimap<Dict.Prop, String> watches = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();

	protected boolean update() {

		breadthFirst(both()).forEach((x) -> {
			LinkedHashMap<Dict.Prop, Object> previous = x.properties.computeIfAbsent(watchedPrevious, (k) -> new LinkedHashMap<>());
			for (Dict.Prop p : watches.keySet()) {
				Object was = previous.get(p);
				Object now = x.properties.get(p);

				if (!Util.safeEq(was, now))
				{
					fire(p, was, now, watches.get(p));
					previous.put(p, now);
				}
			}
		});
		return true;
	}

	public void addWatch(Dict.Prop property, String address)
	{
		watches.put(property, address);
	}

	private void fire(Dict.Prop p, Object was, Object now, Collection<String> strings) {
		strings.forEach((address) -> messageQueue.accept(address, new Triple<>(p, was, now)));
	}

}
