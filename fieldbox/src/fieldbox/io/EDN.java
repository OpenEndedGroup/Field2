package fieldbox.io;

import field.utility.Rect;
import fieldbox.boxes.plugins.BoxRef;
import sun.misc.Unsafe;
import us.bpsm.edn.Keyword;
import us.bpsm.edn.Tag;
import us.bpsm.edn.parser.*;
import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;
import us.bpsm.edn.protocols.Protocol;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Helper class to handle serialization of IO classes into EDN (the Extensible Data Notation).
 * <p>
 * EDN Happens to be a subset of the Clojure programming language. As a serialization format it offers a few speculative advantages --- it's human readable, it doesn't get all twisted up by the
 * _potential_ of schema validation like XML does, but it's terser and more extensible than JSON. It also offers the potential (in the future) for us to bring the Datomic + Codeq machinery to bare on
 * our internal file format (this will offer us a clean way of doing merges and diffs).
 * <p>
 * see https://github.com/edn-format/edn for more information (we're have a dependency here on https://github.com/bpsm/edn-java which is a pure java (i.e. no Clojure) EDN package)
 */
public class EDN {

	private final Protocol<Printer.Fn<?>> thePrinter;

	private static Unsafe getUnsafe() {
		try {

			Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
			singleoneInstanceField.setAccessible(true);
			return (Unsafe) singleoneInstanceField.get(null);

		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}


	private final Parser.Config.Builder builder;

	static public final Tag RECT = Tag.newTag("field", "rect");
	static public final Tag DOCUMENT = Tag.newTag("field", "document");
	static public final Tag EXTERNAL = Tag.newTag("field", "external");
	static public final Tag FILESPEC = Tag.newTag("field", "filespec");
	static public final Tag BOXREF = Tag.newTag("field", "boxref");

	private final Protocol.Builder<Printer.Fn<?>> printer;
	private final Parser theParser;

	public EDN() {
		builder = Parsers.newParserConfigBuilder()
				 .putTagHandler(RECT, simpleDeserializeFromMap(Rect.class));
		builder.putTagHandler(DOCUMENT, simpleDeserializeFromMap(IO.Document.class));
		builder.putTagHandler(EXTERNAL, simpleDeserializeFromMap(IO.External.class));
		builder.putTagHandler(FILESPEC, simpleDeserializeFromMap(IO.Filespec.class));
		builder.putTagHandler(BOXREF, simpleDeserializeFromMap(BoxRef.class));

		builder.setSetFactory(new CollectionBuilder.Factory() {

			public CollectionBuilder builder() {
				return new CollectionBuilder() {
					Set<Object> set = new LinkedHashSet();

					public void add(Object o) {
						this.set.add(o);
					}

					public Object build() {
						return Collections.unmodifiableSet(this.set);
					}
				};
			}
		});
		theParser = Parsers.newParser(builder.build());


		printer = Printers.prettyProtocolBuilder()
				  .put(Rect.class, simpleSerializeToMap(RECT, Rect.class));
		printer.put(IO.Document.class, simpleSerializeToMap(DOCUMENT, IO.Document.class));
		printer.put(IO.External.class, simpleSerializeToMap(EXTERNAL, IO.External.class));
		printer.put(IO.Filespec.class, simpleSerializeToMap(FILESPEC, IO.Filespec.class));
		printer.put(BoxRef.class, simpleSerializeToMap(BOXREF, BoxRef.class));


		thePrinter = printer.build();
	}

	public String write(Object o) {
		StringBuilder w = new StringBuilder();
		Printers.newPrinter(thePrinter, w)
			.printValue(o);
		return w.toString();
	}


	public Object read(String o) {
		Parser p = theParser;
		Parseable parseable = Parsers.newParseable(o);
		return p.nextValue(parseable);
	}


	public TagHandler simpleDeserializeFromMap(Class c) {
		return (tag, o) -> {

			try {
				Object instance = getUnsafe().allocateInstance(c);

				Map<?, ?> m = (Map) o;
				m.entrySet()
				 .forEach(e -> {
					 String s = "";
					 Object k = e.getKey();
					 if (k instanceof Keyword) s = ((Keyword) k).getName();
					 else s = k.toString();

					 try {
						 Field f = c.getField(s);
						 f.setAccessible(true);
						 if (Modifier.isFinal(f.getModifiers())) {
							 long of = getUnsafe().objectFieldOffset(f);
							 if (f.getType() == Float.TYPE) getUnsafe().putFloat(instance, of, ((Number) e.getValue()).floatValue());
							 else throw new IllegalArgumentException("can't handle " + f.getType() + " / " + f + " / " + s + " " + instance);
						 } else {
							 if (f.getType() == Float.TYPE) f.set(instance, ((Number) e.getValue()).floatValue());
							 else f.set(instance, e.getValue());

						 }
					 } catch (NoSuchFieldException e1) {
						 e1.printStackTrace();
					 } catch (IllegalAccessException e1) {
						 e1.printStackTrace();
					 }

				 });
				return instance;
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
			return null;
		};
	}

	public us.bpsm.edn.printer.Printer.Fn<?> simpleSerializeToMap(Tag tag, Class c) {
		return (o, writer) -> {
			try {
				Map<Object, Object> mm = new LinkedHashMap<>();

				Field[] f = c.getFields();
				for (Field ff : f) {
					if (Modifier.isTransient(ff.getModifiers())) continue;
					ff.setAccessible(true);
					mm.put(Keyword.newKeyword(ff.getName()), ff.get(o));
				}

				writer.printValue(tag)
				      .printValue(mm);

			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		};
	}

//	public us.bpsm.edn.printer.Printer.Fn<?> serializeToToString(Tag tag) {
//		return (o, writer) -> {
//			List<String> oo = new ArrayList<>();
//			oo.add((o==null ? "java.lang.Object" : o.getClass().getName()));
//			oo.add(""+o);
//			writer.printValue(tag)
//			      .printValue(oo);
//		};
//	}
//
//	public TagHandler serializeViaNashornEval() {
//		return (tag, o) -> {
//
//			List<String> oo = (List<String>)o;
//
//			ScriptEngineManager engineManager =
//				    new ScriptEngineManager();
//			ScriptEngine engine =
//				    engineManager.getEngineByName("nashorn");
//			try {
//				Class c2 = Class.forName(oo.get(0));
//				Object value = engine.eval("" + oo.get(1));
//
//
//				if (value instanceof ScriptObjectMirror)
//					value = ScriptUtils.unwrap(value);
//
//				if (value instanceof ScriptFunctionImpl) {
//					StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(
//						    new Class[]{c2}, (ScriptObject) value, MethodHandles.lookup());
//					try {
//						return adapterClassFor.getRepresentedClass()
//									  .newInstance();
//					} catch (InstantiationException e) {
//						Log.log("processing.error",
//							" problem instantiating adaptor class to take us from " + value + " ->" + c2,
//							e);
//					} catch (IllegalAccessException e) {
//						Log.log("processing.error", " problem instantiating adaptor class to take us from " + value + " ->" + c2, e);
//					}
//				}
//
//
//				throw new ClassCastException(" expected " + c2 + ", got " + value + " / " + value.getClass());
//
//
//			} catch (ScriptException e) {
//				e.printStackTrace();
//			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
//			}
//			return null;
//		};
//	}


}
