package fieldbox.boxes;

import fieldbox.execution.Completion;
import fieldbox.execution.HandlesCompletion;
import fielded.boxbrowser.ObjectToHTML;
import fieldlinker.Linker;
import fieldnashorn.annotations.SafeToToString;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class to provide more useful access to children and parent lists inside Field
 */
@SafeToToString
public class BoxChildHelper implements Linker.AsMap, Collection<Box>, ObjectToHTML.MasqueradesAs , HandlesCompletion{

	private final ArrayList<Box> c;
	LinkedHashMap<String, Box> t = new LinkedHashMap<>();
	LinkedHashMap<String, List<Box>> tl = new LinkedHashMap<>();

	public BoxChildHelper(Collection<Box> c) {
		this.c = new ArrayList<>(c);
		c.stream().forEach(x -> t.put(x.properties.get(Box.name), x));
		c.stream().forEach(x -> tl.computeIfAbsent(x.properties.get(Box.name), (k)->new ArrayList<>()).add(x));
	}

	@Override
	public boolean asMap_isProperty(String p) {
		return t.containsKey(p) || p.equals("all");
	}

	@Override
	public Object asMap_call(Object a, Object b) {
		return null;
	}

	@Override
	public Object asMap_get(String p) {
		if (p.equals("all"))
		{
			return new All();
		}
		return t.get(p);
	}

	@Override
	public Object masqueradesAs() {
		return t;
	}

	public class All implements Linker.AsMap, ObjectToHTML.MasqueradesAs, HandlesCompletion
	{

		@Override
		public String toString() {
			return ""+tl;
		}

		@Override
		public boolean asMap_isProperty(String p) {
			return tl.containsKey(p);
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			return null;
		}

		@Override
		public Object asMap_get(String p) {
			return tl.get(p);
		}

		@Override
		public Object asMap_set(String p, Object o) {
			throw new IllegalArgumentException("cannot change `_.children` / `_.parents` directly, call `_.connect` instead");
		}

		@Override
		public Object asMap_new(Object a) {
			return null;
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			return null;
		}

		@Override
		public Object asMap_getElement(int element) {
			return null;
		}

		@Override
		public Object asMap_setElement(int element, Object o) {
			throw new IllegalArgumentException("cannot change `_.children` / `_.parents` directly, call `_.connect` instead");
		}

		@Override
		public boolean asMap_delete(Object p) {
			throw new IllegalArgumentException("cannot change `_.children` / `_.parents` directly, call `_.connect` instead");
		}

		@Override
		public Object masqueradesAs() {
			return tl;
		}

		public List<Completion> getCompletionsFor(String prefix) {
			List<Completion> c = new ArrayList<>();
			for (Map.Entry<String, Box> entry : t.entrySet()) {
				if (entry.getKey().toLowerCase().startsWith(prefix.toLowerCase())) {
					c.add(new Completion(-1, -1, entry.getKey(), messageFor(entry.getValue())));
				}
			}
			return c;
		}

		private String messageFor(Box value) {
			return ""+value;
		}

	}

	@Override
	public Object asMap_set(String p, Object o) {
		throw new IllegalArgumentException("cannot change `_.children` / `_.parents` directly, call `_.connect` instead");
	}

	@Override
	public Object asMap_new(Object a) {
		return null;
	}

	@Override
	public Object asMap_new(Object a, Object b) {
		return null;
	}

	@Override
	public Object asMap_getElement(int element) {
		return c.get(element);
	}

	@Override
	public Object asMap_setElement(int element, Object o) {
		throw new IllegalArgumentException("cannot change `_.children` / `_.parents` directly, call `_.connect` instead");
	}

	@Override
	public boolean asMap_delete(Object p) {
		throw new IllegalArgumentException("cannot change `_.children` / `_.parents` directly, call `_.connect` instead");
	}

	@Override
	public int size() {
		return c.size();
	}

	@Override
	public boolean isEmpty() {
		return c.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return c.contains(o);
	}

	@Override
	public Iterator<Box> iterator() {
		return c.iterator();
	}

	@Override
	public Object[] toArray() {
		return c.toArray();
	}

	@Override
	public <T> T[] toArray(T[] ts) {
		return c.toArray(ts);
	}

	@Override
	public boolean add(Box box) {
		return c.add(box);
	}

	@Override
	public boolean remove(Object o) {
		return c.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return c.containsAll(collection);
	}

	@Override
	public boolean addAll(Collection<? extends Box> collection) {
		return c.addAll(collection);
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		return c.removeAll(collection);
	}

	@Override
	public boolean removeIf(Predicate<? super Box> predicate) {
		return c.removeIf(predicate);
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return c.retainAll(collection);
	}

	@Override
	public void clear() {
		c.clear();
	}

	@Override
	public boolean equals(Object o) {
		return c.equals(o);
	}

	@Override
	public int hashCode() {
		return c.hashCode();
	}

	@Override
	public Spliterator<Box> spliterator() {
		return c.spliterator();
	}

	@Override
	public Stream<Box> stream() {
		return c.stream();
	}

	@Override
	public Stream<Box> parallelStream() {
		return c.parallelStream();
	}

	@Override
	public void forEach(Consumer<? super Box> consumer) {
		c.forEach(consumer);
	}

	@Override
	public String toString() {
		return c+"";
	}

	public List<Completion> getCompletionsFor(String prefix) {
		List<Completion> c = new ArrayList<>();
		for (Map.Entry<String, Box> entry : t.entrySet()) {
			if (entry.getKey().toLowerCase().startsWith(prefix.toLowerCase())) {
				c.add(new Completion(-1, -1, entry.getKey(), messageFor(entry.getValue())));
			}
		}
		return c;
	}

	private String messageFor(Box value) {
		return ""+value;
	}
}
