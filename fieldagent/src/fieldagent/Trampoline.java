package fieldagent;

import com.google.common.collect.MapMaker;
import com.google.common.io.ByteStreams;
import sun.misc.Resource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.*;

/**
 * Created by marc on 7/1/14.
 */
public class Trampoline {

	static public  final boolean traceLoader = false;
	static public  boolean whereLoaded = false;

	static protected Transform transform = new Transform();

	static public class Record {
		String filename;
		long modification;

		public Record(String fn, long l) {
			this.filename = fn;
			this.modification = l;
		}

		public boolean modified() {
			return new File(filename).lastModified() != modification;
		}

		public Record update() {
			modification = new File(filename).lastModified();
			return this;
		}
	}

	static public Map<Class, Record> loadMap = new MapMaker().concurrencyLevel(2).initialCapacity(1000).weakKeys().makeMap();


	static public class ExtensibleClassloader extends URLClassLoader {

		{
//			registerAsParallelCapable();
		}

		public ExtensibleClassloader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		public ExtensibleClassloader(URL[] urls) {
			super(urls);
		}

		public ExtensibleClassloader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
			super(urls, parent, factory);
		}

		public void addURL(URL url) {
			super.addURL(url);
		}


		public Class loadClass(String name) throws ClassNotFoundException {
			if (!shouldLoad(name)) return super.loadClass(name);
			return loadClass(name, false);
		}

		protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

			if (!shouldLoad(name)) return super.loadClass(name, resolve);

			if (traceLoader) System.out.println("C(lc): " + name);

			// First, check if the class has already been loaded
			Class c = findLoadedClass(name);

			if (traceLoader) System.out.println("C(lc): already loaded ? " + c);

			// if not loaded, search the local (child) resources
			if (c == null) {
				try {
					if (whereLoaded){
						URL r = this.getResource(name.replace('.', '/')
									     .concat(".class"));
						if (r!=null)
							System.out.println(name+" <- "+r);
					}

					c = findClass(name);
					if (traceLoader) System.out.println("C(lc): found  " + c + "we're done here");
					if (traceLoader && c!=null)System.out.println("C(lc): code source is :"+c.getProtectionDomain().getCodeSource().getLocation());

					try{

						File f = new File(c.getProtectionDomain()
								      .getCodeSource()
								      .getLocation()
								      .getFile(), name.replace(".", "/") + ".class");

						System.out.println(" file is :"+f);

						if (f.exists())
						{
							Record rec = new Record(f.getAbsolutePath(), f.lastModified());
							if (rec.modification != 0) {
								loadMap.put(c, rec);
							};
						}
					}
					catch(Throwable t)
					{
						t.printStackTrace();
					}

				} catch (ClassNotFoundException cnfe) {
					{
						String fn = name.replace(".", "/") + ".class";

						URL r = getResource(fn);
						if (r != null) try (InputStream where = new BufferedInputStream(r.openStream())) {
							if (where != null) {
								byte[] b = ByteStreams.toByteArray(where);
								b = transformClass(name, b);
								c = defineClass(name, b, 0, b.length);

								Record rec = new Record(r.getFile(), new File(r.getFile()).lastModified());
								if (rec.modification != 0) {
									loadMap.put(c, rec);
									if (traceLoader)System.err.println(" made loadmap rec for "+r.getFile());
								}
								else
								if (traceLoader)System.err.println(" couldn't find file for "+r.getFile());

								if (traceLoader) System.out.println(" loaded " + loadMap.size());
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

			// if we could not find it, delegate to parent
			// Note that we don't attempt to catch any ClassNotFoundException
			if (c == null) {
				if (getParent() != null) {
					c = getParent().loadClass(name);
				} else {
					c = getSystemClassLoader().loadClass(name);
				}
			}

			if (resolve) {
				resolveClass(c);
			}

			return c;
		}

		protected byte[] transformClass(String name, byte[] b) {
			return transform.transform(name, b);
		}


		LinkedHashSet<String> blacklist_prefix = new LinkedHashSet<String>(Arrays
			    .asList("fieldagent", "java", "sun", "jdk", "javax", "sunw", "apple", "com.apple", "org.cef"));

		protected boolean shouldLoad(String name) {

			if (!name.contains(".")) return true;

			String[] s = name.split("\\.");

			for (int i = 1; i < s.length; i++) {
				String m = "";
				for (int q = 0; q < i; q++) {
					m += (q == 0 ? "" : ".") + s[q];
				}

				if (blacklist_prefix.contains(m)) return false;
			}


			return true;
		}

		public URL getResource(String name) {

			URL url = findResource(name);

			// if local search failed, delegate to parent
			if (url == null) {
				url = getParent().getResource(name);
			}

			if (traceLoader) System.out.println("C: " + name + " -> " + url);
			if (traceLoader) if (url == null) System.out.println(" URL search paths are " + Arrays.asList(getURLs())+" inside "+getParent());

			return url;
		}

		public List<URL> collectURLS()
		{
			List<URL> u = new ArrayList<>();

			URLClassLoader l = this;
			while(l!=null)
			{
				u.addAll(Arrays.asList(l.getURLs()));

				if (l.getParent() instanceof URLClassLoader)
				{
					l = (URLClassLoader) l.getParent();
				}
				else l = null;
			}
			return u;

		}
	}

	static public void main(String[] a) {

		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (a.length == 0) {
			System.err.println(" No main.class specified. Add one to the command line");
			System.exit(1);
		}

		String mainClass = a[0];
		String[] a2 = new String[a.length - 1];
		System.arraycopy(a, 1, a2, 0, a.length - 1);

		ExtensibleClassloader classloader = new ExtensibleClassloader(new URL[]{}, Thread.currentThread().getContextClassLoader());

		Thread.currentThread().setContextClassLoader(classloader);

		try {
			Class clazz = classloader.loadClass(mainClass);
			Method m = clazz.getMethod("main", String[].class);
			System.err.println(" -- m -- " + m);
			m.invoke(null, new Object[]{a2});
		} catch (Throwable t) {
			System.err.println(" Exception thrown in main of " + mainClass);
			t.printStackTrace();
			System.exit(1);
		}
	}

	static public void addURL(URL n) {

		ClassLoader c = Thread.currentThread().getContextClassLoader();
		try {
			c.getClass().getMethod("addURL", URL.class).invoke(c, n);
		} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
		}

		String s = n.getPath();
		if (new File(s).isDirectory()) {
			System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
			System.setProperty("jna.library.path", System.getProperty("jna.library.path") + File.pathSeparator + s);

//		System.out.println(" library paths now "+System.getProperty("java.library.path")+" and "+System.getProperty("jna.library.path"));
			// This enables the java.library.path to be modified at runtime
			// From a Sun engineer at
			// http://forums.sun.com/thread.jspa?threadID=707176

			try {
				Field field = ClassLoader.class.getDeclaredField("usr_paths");
				field.setAccessible(true);
				String[] paths = (String[]) field.get(null);
				for (int i = 0; i < paths.length; i++) {
					if (s.equals(paths[i])) {
						return;
					}
				}
				String[] tmp = new String[paths.length + 1];
				System.arraycopy(paths, 0, tmp, 0, paths.length);
				tmp[paths.length] = s;
				field.set(null, tmp);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("Failed to get permissions to set library path");
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("Failed to get field handle to set library path");
			}
		}
	}
}
