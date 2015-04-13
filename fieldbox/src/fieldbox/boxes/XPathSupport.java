package fieldbox.boxes;

import field.utility.Dict;
import fieldbox.io.IO;
import org.apache.commons.jxpath.DynamicPropertyHandler;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathIntrospector;

import java.util.*;

/**
 * Created by marc on 3/9/15.
 */
public class XPathSupport {


	private final JXPathContext c;

	static public class Box_jxbean implements DynamicPropertyHandler {
		@Override
		public Object getProperty(Object object, String propertyName) {

			Box x = (Box) object;
//			if (propertyName.equals("p")) return x.parents();

			Object q = x.properties.getOrConstruct(new Dict.Prop(propertyName));
			if (q == null) {
				Optional<Box> qz = x.children()
						    .stream()
						    .filter(z -> z.properties.has(Box.name) && z.properties.get(Box.name)
													   .equals(propertyName))
						    .findFirst();
				if (qz.isPresent()) return qz.get();
			} else {
				return q;
			}
			return null;
		}

		@Override
		public String[] getPropertyNames(Object object) {
			List<String> a = new ArrayList<>();
			((Box) object).properties.getMap()
						 .keySet().stream()
				    .filter(x -> !x.getName().startsWith("_") || x.getName().equals("__id__"))
				    .forEach(x -> a.add(x.getName()));
			((Box) object).children()
				      .stream()
				      .map(x -> x.properties.get(Box.name))
				      .filter(x -> x != null)
				      .forEach(x -> a.add(x));

			return a.toArray(new String[a.size()]);
		}

		@Override
		public void setProperty(Object object, String propertyName, Object value) {

			if (value instanceof Box) {
				((Box) value).properties.put(Box.name, propertyName);
				((Box) object).connect((Box) value);
			} else {
				((Box) value).asMap_new(propertyName, value);
				//.properties.put(new Dict.Prop(propertyName), value);
			}
		}
	}

	static {
		JXPathIntrospector.registerDynamicClass(Box.class, Box_jxbean.class);


	}

	private final Box on;

	public XPathSupport(Box on) {
		JXPathContext c1 = null;
		this.on = on;

		Optional<Box> found = on.find(Boxes.root, on.both())
					.findFirst();
		if (found.isPresent()) {
			Box root = found.get();

			if (root.children()
				.contains(on)) {

				c1 = JXPathContext.newContext(root);

				c1 = c1.getRelativeContext(c1.getPointer("/*[@__id__='" + on.properties.get(IO.id) + "']"));
			}
		}

		if (c1==null)
		{
			c1 = JXPathContext.newContext(on);
		}
		c = c1;
	}

	public Collection<Object> get(String name) {
		Iterator ii = c.iterate(name);
		List<Object> r = new ArrayList<>();
		ii.forEachRemaining(r::add);

		return r;
	}


	public void set(String name, Object value) {
		c.setValue(name, value);
	}

}
