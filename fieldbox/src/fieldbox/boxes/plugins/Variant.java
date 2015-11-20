package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.LinkedHashMapAndArrayList;
import field.utility.Pair;
import field.utility.Util;
import fieldbox.Open;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fielded.Commands;
import fielded.RemoteEditor;
import fielded.boxbrowser.BoxBrowser;
import fielded.boxbrowser.ObjectToHTML;
import fielded.plugins.Out;
import fieldlinker.Linker;
import jdk.internal.jline.console.history.History;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A box is either included in a Variant, excluded in a Variant or it doesn't matter (it can stay if it's here, or stay away if it isn't). Here we keep enough information to disconnect boxes
 * (completely) from the hierarchy and reconnect them again when we want to This will require the cooperation of IO . When we save the graph we need to save the full graph with everything in it, not
 * the "current" subset
 * <p>
 * We have to add copy history and merge to this (from Diff, in the personal tree).
 */
public class Variant extends Box implements IO.Loaded {

	static public Dict.Prop<Set<String>> includes = new Dict.Prop<Set<String>>("includes").type()
											      .toCannon()
											      .autoConstructs(() -> new LinkedHashSet<String>()); // todo: doc


	static public Dict.Prop<Set<String>> excludes = new Dict.Prop<Set<String>>("excludes").type()
											      .toCannon()
											      .autoConstructs(() -> new LinkedHashSet<String>()); // todo: doc

	// this is set per box, because we need it to be loaded and saved with the boxes
	static public Dict.Prop<String> theVariant = new Dict.Prop<>("_theVariant").type();

	static public Dict.Prop<FunctionOfBoxValued<CurrentVariant>> variant = new Dict.Prop<FunctionOfBoxValued<CurrentVariant>>("variant").type()
																	    .toCannon();

	static {
		IO.persist(theVariant);
		IO.persist(includes);
		IO.persist(excludes);


		variant.set(BoxBrowser.toMarkdown, (box, val) -> {

			String s = "";

			s += "*Current Variant* : "+val+"\n";

			Set<String> o = box.properties.get(includes);
			s += "*Included in* : "+o+"\n";
			o = box.properties.get(excludes);
			s += "*Excluded in* : "+o+"\n";

			return s;
		});
	}

	static public class CurrentVariant implements Linker.AsMap {

		private final Box from;
		private final String variant;

		public CurrentVariant(Box from, String variant) {
			this.from = from;
			this.variant = variant;
		}

		@Override
		public boolean asMap_isProperty(String p) {
			return from.asMap_isProperty(p) || from.asMap_isProperty(prefix(p));
		}

		private String prefix(String p) {
			return "__variant:" + variant + ":" + p;
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			return from.asMap_call(a, b);
		}

		@Override
		public Object asMap_get(String p) {
			Object o = from.asMap_get(prefix(p));
			if (o != null) return o;
			return from.asMap_get(p);
		}

		@Override
		public Object asMap_set(String p, Object o) {
			from.asMap_set(p, o);
			return from.asMap_set(prefix(p), o);
		}

		@Override
		public Object asMap_new(Object a) {
			return from.asMap_new(a);
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			return from.asMap_new(a, b);
		}

		@Override
		public Object asMap_getElement(int element) {
			return from.asMap_getElement(element);
		}

		@Override
		public Object asMap_setElement(int element, Object o) {
			return from.asMap_setElement(element, o);
		}

		@Override
		public boolean asMap_delete(Object o) {
			return asMap_isProperty(prefix("" + o));
		}
	}

	static public class Memo implements Util.ExceptionlessAutoCloasable {
		Map<Box, Boolean> state = new LinkedHashMap<>();

		@Override
		public void close() {
			for (Map.Entry<Box, Boolean> e : state.entrySet()) {
				e.getKey().disconnected = e.getValue();
			}

		}
	}

	public static Memo freezeGraph(Box documentRoot) {

		Memo m = new Memo();

		documentRoot.breadthFirstAll(documentRoot.both())
			    .forEach(x -> {
				    m.state.put(x, x.disconnected);
			    });

		return m;
	}

	public static Memo connectAll(Box documentRoot) {
		Memo m = freezeGraph(documentRoot);
		documentRoot.breadthFirstAll(documentRoot.both())
			    .forEach(x -> {
				    x.disconnected = false;
			    });
		return m;
	}

	public Variant() {
		this(null);
	}

	public Variant(Box root) {
		this.properties.put(variant, x -> new CurrentVariant(x, x.properties.getOrConstruct(theVariant)));

		properties.put(Commands.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

			long s = selection().count();
			boolean all = (s == 0) || (s == 1 && selection().findFirst()
									.get() == this);

			if (selectionOrAll() == null) return m;

			m.put(new Pair<>("Switch to variant", "Switches to an existing variant"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					this.p = prompt;
				}

				@Override
				public void run() {
					Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					fillInExistingVariants(m, x -> {
						switchAllToVariant(x);
					});

					p.prompt("Switch to...", m, new RemoteEditor.ExtendedCommand() {

						String alt = "";

						@Override
						public void run() {
							switchAllToVariant(this.alt);
						}

						@Override
						public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
							this.alt = alternativeChosen;
						}
					});
				}
			});

			m.put(new Pair<>("Copy to variant",
					 "Selects or creates a new variant" + (s > 1 ? ", copies " + (all ? "all" : "the selected") + " boxes to this variant, and switches to that variant" : (s > 0 ? ", copies the selected box to this variant, and switches to that variant" : ""))),
			      new RemoteEditor.ExtendedCommand() {

				      public RemoteEditor.SupportsPrompt p;

				      @Override
				      public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					      this.p = prompt;
				      }

				      @Override
				      public void run() {
					      Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					      fillInExistingVariants(m, x -> {
						      copySelectionToVariant(x);
						      // should we always do all here?
						      switchAllToVariant(x);
					      });

					      p.prompt("New variant name", m, new RemoteEditor.ExtendedCommand() {

						      String alt = "";

						      @Override
						      public void run() {

							      System.err.println(" MAKE NEW VARIANT CALLED :" + this.alt);
							      copySelectionToVariant(this.alt);
							      // should we always do all here?
							      switchAllToVariant(this.alt);
						      }

						      @Override
						      public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
							      this.alt = alternativeChosen;
						      }
					      });
				      }
			      });

			m.put(new Pair<>("Fork to variant",
					 "Selects or creates a new variant" + (s > 1 ? ", forks " + (all ? "all" : "the selected") + " boxes to this variant, and switches to that variant" : (s > 0 ? ", forks the selected box to this variant, and switches to that variant" : ""))),
			      new RemoteEditor.ExtendedCommand() {

				      public RemoteEditor.SupportsPrompt p;

				      @Override
				      public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					      this.p = prompt;
				      }

				      @Override
				      public void run() {
					      Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					      fillInExistingVariants(m, x -> {
						      forkSelectionToVariant(x);
						      // should we always do all here?
						      switchAllToVariant(x);
					      });

					      p.prompt("New variant name", m, new RemoteEditor.ExtendedCommand() {

						      String alt = "";

						      @Override
						      public void run() {

							      System.err.println(" MAKE NEW VARIANT CALLED :" + this.alt);
							      forkSelectionToVariant(this.alt);
							      // should we always do all here?
							      switchAllToVariant(this.alt);
						      }

						      @Override
						      public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
							      this.alt = alternativeChosen;
						      }
					      });
				      }
			      });
			m.put(new Pair<>("Remove from variant",
					 "Selects or creates a new variant" + (s > 1 ? ", disables " + (all ? "all" : "the selected") + " boxes to this variant, and switches to that variant" : (s > 0 ? ", disables the selected box to this variant, and switches to that variant" : ""))),
			      new RemoteEditor.ExtendedCommand() {

				      public RemoteEditor.SupportsPrompt p;

				      @Override
				      public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					      this.p = prompt;
				      }

				      @Override
				      public void run() {
					      Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					      fillInExistingVariants(m, x -> {
						      removeSelectionFromVariant(x);

						      // should we always do all here?
						      switchAllToVariant(x);
					      });

					      p.prompt("New variant name", m, new RemoteEditor.ExtendedCommand() {


						      String alt;

						      @Override
						      public void run() {
							      System.err.println(" MAKE NEW VARIANT CALLED :" + this.alt);
							      removeSelectionFromVariant(this.alt);
							      // should we always do all here?
							      switchAllToVariant(this.alt);
						      }

						      @Override
						      public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
							      this.alt = alternativeChosen;
						      }
					      });
				      }
			      });


			//TODO: fork new variant (copies storage, maintains history)

			return m;

		});
	}

	private void switchAllToVariant(String variant) {
		Stream<Box> s = all();
		s.forEach(x -> {
			x.properties.put(theVariant, variant);
		});
		updateToVariant();
	}

	private void updateToVariant() {
		Stream<Box> s = all();
		s.forEach(x -> {
			String t = x.properties.get(theVariant);

			if (t == null) return;

			if (isIncluded(x, t)) {
				x.disconnected = false;
				x.properties.getOrConstruct(excludes)
					    .remove(t);

				for (Map.Entry<Dict.Prop, Object> p : x.properties.getMap()
										  .entrySet())
					if (p.getKey()
					     .getName()
					     .startsWith("__variant:" + t + ":")) x.asMap_set(p.getKey()
											       .getName()
											       .substring(("__variant:" + t + ":").length()), p.getValue());

			}

			if (isExcluded(x, t)) x.disconnected = true;

			installDrawer(x, t);
		});
	}

	private void installDrawer(Box x, String tag) {
		x.properties.putToMap(FLineDrawing.lines, "__tag__", FLineDrawing.boxOrigin(() -> {


			FLine f = new FLine();
			f.attributes.put(StandardFLineDrawing.hasText, true);
			f.moveTo(5, -5);
			f.last().attributes.put(StandardFLineDrawing.text, tag);
			f.last().attributes.put(StandardFLineDrawing.textAlign, 0);

			f.attributes.put(StandardFLineDrawing.opacity, 0.5f);
			return f;

		}, new Vec2(0,1), x));
	}

	private boolean isExcluded(Box x, String t) {
		if (t.equals("")) return false;
		return x.properties.getOrConstruct(excludes)
				   .contains(t);
	}

	private boolean isIncluded(Box x, String t) {
		if (t.equals("")) return true;
		return x.properties.getOrConstruct(includes)
				   .contains(t);
	}

	private void copySelectionToVariant(String variant) {
		{
			Stream<Box> s = all();
			s.forEach(x -> {
				if (!isIncluded(x, variant)) x.properties.getOrConstruct(excludes)
									 .add(variant);
			});
		}
		{
			Stream<Box> s = selectionOrAll();
			s.forEach(x -> {
				x.properties.getOrConstruct(includes)
					    .add(variant);
				x.properties.getOrConstruct(excludes)
					    .remove(variant);
			});
		}

	}

	private void forkSelectionToVariant(String variant) {
		{
			Stream<Box> s = all();
			s.forEach(x -> {
				if (!isIncluded(x, variant)) x.properties.getOrConstruct(excludes)
									 .add(variant);
			});
		}
		{
			Stream<Box> s = selectionOrAll();
			s.forEach(x -> {
				x.properties.getOrConstruct(includes)
					    .add(variant);
				x.properties.getOrConstruct(excludes)
					    .remove(variant);

				String currently = x.properties.get(theVariant);
				if (currently == null) {
					// fork on demand?
				} else {
					Map<Dict.Prop, Object> m = x.properties.getMap();
					for (Dict.Prop p : new LinkedHashSet<>(m.keySet())) {
						if (p.getName()
						     .startsWith("__variant:" + currently + ":")) {
							forkProperty(x, p, new Dict.Prop(p.getName()
											  .replace("__variant:" + currently + ":", "__variant:" + variant + ":")));
						}
					}
				}
			});
		}

	}

	private void forkProperty(Box x, Dict.Prop from, Dict.Prop to) {
		// here's where we do version tracking

		Object m = x.properties.get(from);
		x.properties.put(to, m);
	}


	private void removeSelectionFromVariant(String variant) {
		Stream<Box> s = selectionOrAll();
		s.forEach(x -> {
			x.properties.getOrConstruct(includes)
				    .remove(variant);
			x.properties.getOrConstruct(excludes)
				    .add(variant);
		});
	}


	protected void fillInExistingVariants(Map<Pair<String, String>, Runnable> target, Consumer<String> task) {
		LinkedHashMap<String, Integer> seen = new LinkedHashMap<>();

		target.put(new Pair<>("<i>default</i>", "The default variant where everything is visible"), () -> {
			task.accept("");
		});

		this.breadthFirstAll(this.downwards())
		    .filter(x -> x.properties.has(includes))
		    .forEach(x -> {
			    x.properties.get(includes)
					.forEach(y -> inc(seen, y));
		    });

		seen.entrySet()
		    .forEach(x -> {
			    target.put(new Pair<>(x.getKey(), "Used by " + x.getValue() + " box" + (x.getValue() > 1 ? "es" : "")), () -> {
				    task.accept(x.getKey());
			    });
		    });


	}

	private void inc(Map<String, Integer> seen, String y) {
		seen.put(y, seen.getOrDefault(y, 0) + 1);
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

	private Stream<Box> selectionOrAll() {

		long s = selection().count();

		if (s == 1 && selection().findFirst()
					 .get() == this) return this.breadthFirst(downwards())
								    .filter(x -> x != this);

		return selection().filter(x -> isProgeny(x, this));
	}

	private Stream<Box> all() {

		return this.breadthFirstAll(this.downwards())
			   .filter(x -> x != this);
	}

	private boolean isProgeny(Box x, Variant me) {
		return me.breadthFirstAll(me.downwards())
			 .filter(y -> y != this)
			 .anyMatch(y -> y == x);
	}


	public void loaded() {
		updateToVariant();
	}

}
