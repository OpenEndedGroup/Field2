package fieldbox.boxes.plugins;

import field.utility.Conversions;
import field.utility.Dict;
import field.utility.Util;
import fieldbox.boxes.Box;
import fieldlinker.Linker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MissingStream extends Box {

	static public final Dict.Prop<FunctionOfBoxValued<FilteredLogStreamMap>> log = new Dict.Prop<FunctionOfBoxValued<FilteredLogStreamMap>>("log").type()
																		      .toCannon()
																		      .doc("returns a map that can be used to build readers for the properties log. For example `_.log.myreader` will return, incrementally, all the items of the log (in the absence of buffer overruns). Note this only returns log events that have something to do with this part of the hierarchy (and all children), but readers, once created, are visible from children. `_.log(\".*x\").somename` creates a reader that filters by property names ending in `x`. `_.log(function(l) l...).somename` also works");

	public MissingStream(Box root_unused) {
		this.properties.put(log, x -> new FilteredLogStreamMap(x, "", null));
	}

	static public class FilteredLogStream implements Box.FunctionOfBoxValued<List<Missing.Log>> {
		final Predicate<Missing.Log> filter;
		LinkedHashSet<Missing.Log> seen = new LinkedHashSet<>();
		List<Missing.Log> buffered = new ArrayList<>();

		public FilteredLogStream() {
			this.filter = null;

		}

		public FilteredLogStream(Predicate<Missing.Log> filter) {
			this.filter = filter;
		}

		public List<Missing.Log> take(List<Missing.Log> m) {

			LinkedHashSet<Missing.Log> a = new LinkedHashSet<>(m);
			a.removeAll(seen);

			LinkedHashSet<Missing.Log> nextSeen = new LinkedHashSet<>(m);

			List<Missing.Log> out = null;
			if (filter != null) out = a.stream()
						   .filter(filter)
						   .collect(Collectors.toList());
			else out = new ArrayList<>(a);

			buffered.addAll(out);

			seen = nextSeen;
			return out;
		}

		@Override
		public List<Missing.Log> apply(Box box) {
			return take(Missing.getLog());
		}
	}

	static public class FilteredLogStreamMap implements fieldlinker.AsMap {

		private final Box at;
		private final String prefix;
		private final Function<Box, Predicate<Missing.Log>> suppliesPredicate;

		public FilteredLogStreamMap(Box x, String prefix, Function<Box, Predicate<Missing.Log>> suppliesPredicate) {
			this.at = x;
			this.prefix = prefix;
			this.suppliesPredicate = suppliesPredicate;
		}

		@Override
		public boolean asMap_isProperty(String p) {
			return true;
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			if (b instanceof String) {
				Predicate<Missing.Log> p = suppliesPredicate.apply(at);
				return new FilteredLogStreamMap(at, prefix + "_" + b + "_", $ -> (l -> (p.test(l) && l.what != null && l.what.getName()
																	     .matches("" + b))));
			} else {
				Object c = Conversions.convert(b, Predicate.class);
				if (c == null) throw new ClassCastException(" can't convert " + b + " to a predicate over log items");
				Predicate<Missing.Log> p1 = (Predicate<Missing.Log>) c;
				Predicate<Missing.Log> p2 = suppliesPredicate.apply(at);

				return new FilteredLogStreamMap(at, prefix + "_" + b + "_", $ -> (l -> (p1.test(l) && p2.test(l))));
			}
		}

		@Override
		public Object asMap_get(String p) {
			Dict.Prop<FilteredLogStream> pp = new Dict.Prop<>("__filteredLogStreamMap_" + prefix + "_" + p);
			try (Util.ExceptionlessAutoCloasable $ = Missing.pause()) {
				Object o = at.asMap_get(pp.getName());
				if (o == null) {
					if (suppliesPredicate == null) at.properties.put(pp, ((FilteredLogStream) (o = new FilteredLogStream())));
					else at.properties.put(pp, ((FilteredLogStream) (o = new FilteredLogStream(wrap(at, suppliesPredicate.apply(at))))));
					return ((FilteredLogStream) o).apply(at);
				}
				return o;
			}
		}

		private Predicate<Missing.Log> wrap(Box at, Predicate<Missing.Log> apply) {
			Predicate<Missing.Log> q = apply == null ? (x -> true) : apply;

			LinkedHashSet<Box> all = new LinkedHashSet<>(at.breadthFirst(at.downwards())
								       .collect(Collectors.toList()));

			return (x) -> q.test(x) && (all.contains(x.from) || all.contains(x.to));
		}

		@Override
		public Object asMap_set(String p, Object o) {
			return null;
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
			return null;
		}

		@Override
		public boolean asMap_delete(Object o) {
			return false;
		}
	}


}
