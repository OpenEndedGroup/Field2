package fieldbox.io;

import field.utility.Rect;
import sun.misc.Unsafe;
import us.bpsm.edn.Keyword;
import us.bpsm.edn.Tag;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;
import us.bpsm.edn.parser.TagHandler;
import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;
import us.bpsm.edn.protocols.Protocol;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by marc on 3/24/14.
 */
public class EDN {


	private final Protocol<Printer.Fn<?>> thePrinter;

	private static Unsafe getUnsafe() {
		try {

			Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
			singleoneInstanceField.setAccessible(true);
			return (Unsafe) singleoneInstanceField.get(null);

		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();;
		}
		return null;
	};

	private final Parser.Config.Builder builder;

	static public final Tag RECT = Tag.newTag("field", "rect");
	static public final Tag DOCUMENT = Tag.newTag("field", "document");
	static public final Tag EXTERNAL = Tag.newTag("field", "external");
	static public final Tag FILESPEC= Tag.newTag("field", "filespec");

	private final Protocol.Builder<Printer.Fn<?>> printer;
	private final Parser theParser;

	public EDN() {
		builder = Parsers.newParserConfigBuilder().putTagHandler(RECT, simpleDeserializeFromMap(Rect.class));
		builder.putTagHandler(DOCUMENT, simpleDeserializeFromMap(IO.Document.class));
		builder.putTagHandler(EXTERNAL, simpleDeserializeFromMap(IO.External.class));
		builder.putTagHandler(FILESPEC, simpleDeserializeFromMap(IO.Filespec.class));
		theParser = Parsers.newParser(builder.build());
		printer = Printers.prettyProtocolBuilder().put(Rect.class, simpleSerializeToMap(RECT, Rect.class));
		printer.put(IO.Document.class, simpleSerializeToMap(DOCUMENT, IO.Document.class));
		printer.put(IO.External.class, simpleSerializeToMap(EXTERNAL, IO.External.class));
		printer.put(IO.Filespec.class, simpleSerializeToMap(FILESPEC, IO.Filespec.class));
		thePrinter = printer.build();
	}

	public String write(Object o) {
		System.out.println(" writing :"+o);
		StringBuilder w = new StringBuilder();
		Printers.newPrinter(thePrinter, w).printValue(o);

		System.out.println(" value :"+w.toString());
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
				m.entrySet().forEach(e -> {
					String s = "";
					Object k = e.getKey();
					if (k instanceof Keyword) s = ((Keyword) k).getName();
					else s = k.toString();

					try {
						Field f = c.getField(s);
						f.setAccessible(true);
						if (Modifier.isFinal(f.getModifiers()))
						{
							long of = getUnsafe().objectFieldOffset(f);
							if (f.getType()==Float.TYPE)
								getUnsafe().putFloat(instance, of, ((Number)e.getValue()).floatValue());
							else
								throw new IllegalArgumentException("can't handle "+f.getType()+" / "+f+" / "+s+" "+instance);
						}
						else
						{
							f.set(instance, e.getValue());
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
				for(Field ff : f)
				{
					if (Modifier.isTransient(ff.getModifiers())) continue;
					ff.setAccessible(true);
					mm.put(Keyword.newKeyword(ff.getName()), ff.get(o));
				}

				writer.printValue(tag).printValue(mm);

			}  catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		};
	}


}
