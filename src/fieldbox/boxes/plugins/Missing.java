package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Util;
import fieldbox.boxes.Box;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A class for logging, and potentially filling in, get & sets of properties from outside of Field's internal classes
 */
public class Missing {

	static public Dict.Prop<IdempotencyMap<BiConsumer<Box, Object>>> watch = new Dict.Prop<Boolean>("watch").toCanon().set(Dict.domain, "*/attributes").type().autoConstructs(() -> new IdempotencyMap<>(BiConsumer.class));

	// we need this open for now and Nashorn's enum stuff is a little in flux
	static public final String read = "READ";
	static public final String delete = "DELETE";
	static public final String write = "WRITE";

	static public int maxLogSize = 100;
	static private ArrayList<Log<?>> log = new ArrayList<>();

	static public boolean suspended = false;

	static public Util.ExceptionlessAutoClosable pause() {
		suspended = true;
		return Missing::play;
	}

	static public void play() {
		suspended = false;
	}

	static public <T> T findFrom(Box b, Dict.Prop<T> what) {
		Optional<Box> o = b.breadthFirst(b.upwards())
			.filter(x -> x.properties.has(what) && x.properties.get(what) != null)
			.findFirst();


		if (!o.isPresent()) {
			// record missing, and do something about it?
			return null;
		}


		T r = o.get().properties.get(what);


		recordGet(b, what, o.get(), r);

		return r;
	}

	static public <T> T findFrom(Box b, Dict.Prop<T> what, Set<Box> ignore) {
		Optional<Box> o = b.breadthFirst(x -> {
			LinkedHashSet<Box> p = new LinkedHashSet<>(x.parents);
			p.removeAll(ignore);
			return p;
		}).filter(x -> x.properties.has(what) && x.properties.get(what) != null)
			.findFirst();


		if (!o.isPresent()) {
			// record missing, and do something about it?
			return null;
		}


		T r = o.get().properties.get(what);


		recordGet(b, what, o.get(), r);

		return r;
	}

	static public <T> T delete(Box b, Dict.Prop<T> what) {
		T v = b.properties.get(what);
		recordDelete(b, what, b, v);
		b.properties.remove(what);
		return v;
	}


	private static <T> void recordGet(Box b, Dict.Prop<T> what, Box box, T r) {
		if (suspended) return;
		synchronized (log) {
			log.add(new Log(read, b, what, box, r));
			trim1();
		}
	}

	private static <T> void recordSet(Box b, Dict.Prop<T> what, Box box, T r, T previously) {

		IdempotencyMap<BiConsumer<Box, Object>> watch = what.getAttribute(Missing.watch);
		if (watch != null) {
			watch.values().forEach(x -> x.accept(box, previously));
		}

		if (suspended) return;
		synchronized (log) {
			log.add(new Log(write, b, what, box, r, previously));
			trim1();
		}
	}

	private static <T> void recordDelete(Box from, Dict.Prop<T> what, Box target, T val) {
		if (suspended) return;
		synchronized (log) {
			log.add(new Log(delete, from, what, target, val));
			trim1();
		}
	}

	private static void trim1() {
		if (log.size() > maxLogSize * 2) {
			log = new ArrayList<>(log.subList(maxLogSize, log.size()));
		}
	}

	static public <T> void setTo(Box b, Dict.Prop<T> what, T value) {
		T was = b.properties.get(what);
		b.properties.put(what, value);
		recordSet(b, what, b, value, was);
	}

	static public ArrayList<Log> getLog() {
		synchronized (log) {
			return new ArrayList<>(log);
		}
	}

	static public class Log<T> {
		final public String access;
		final public Box from;
		final public Dict.Prop<T> what;
		final public Box to;
		final public T value;
		final public T previous;


		public Log(String access, Box from, Dict.Prop<T> what, Box to, T value) {
			this.access = access;
			this.from = from;
			this.what = what;
			this.to = to;
			this.value = value;
			this.previous = null;
		}

		public Log(String access, Box from, Dict.Prop<T> what, Box to, T value, T previous) {
			this.access = access;
			this.from = from;
			this.what = what;
			this.to = to;
			this.value = value;
			this.previous = previous;
		}

		@Override
		public String toString() {
			return "[" + access + " " + what + " " + from + " -> " + to + " = " + value + " (" + previous + ")]";
		}
	}

	static public Predicate<Log> incoming(Box from) {
		return x -> x.from == from;
	}

	static public Predicate<Log> outgoing(Box from) {
		return x -> x.to == from;
	}

	static public Predicate<Log> across(Box head, Box tail) {
		LinkedHashSet<Box> down = new LinkedHashSet<>();
		downwardsFrom(tail, down, tail.downwards());
		LinkedHashSet<Box> up = new LinkedHashSet<>();
		downwardsFrom(head, up, head.upwards());

		return x -> down.contains(x.from) && up.contains(x.to);
	}

	private static void downwardsFrom(Box b, LinkedHashSet<Box> down, Function<Box, Collection<Box>> direction) {
		down.addAll(b.breadthFirst(direction)
			.collect(Collectors.toList()));
	}


}
