package field.utility;


import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Automatically serialize things out to places on disk, per user
 */
public class AutoPersist {

	static protected String dir = Options.getDirectory("preferences", () -> System.getProperty("user.home") + "/.field/");

	static protected Map<String, Runnable> hooks = Collections.synchronizedMap(new LinkedHashMap<>());

	static {
		new File(dir.substring(0, dir.length() - 1)).mkdirs();
		if (!new File(dir.substring(0, dir.length() - 1)).exists()) {
			System.err.println("WARNING: preferences dir '" + dir + "' does not exist");
			dir = null;
		} else {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {

				for (Runnable r : hooks.values()) {
					try {
						r.run();
					} catch (Throwable t) {
						System.err.println(" exception thrown in shutdown hook, will continue on");
						t.printStackTrace();
					}
				}
			}));
		}
	}

	/**
	 * loads persisted variable "name" from disk, if you can, otherwise uses the value supplied by "def"
	 * <p>
	 * At exit this variable will be saved to disk. So for mutable types "T" the final variable will be automatically saved and restored across
	 * sessions. For non-mutable types you'll want to use the three argument version of persist.
	 */
	static public <T extends Serializable> T persist(String name, Supplier<T> def) {
		if (dir == null) return def.get();

		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dir + name))))) {
			T ro = (T) ois.readObject();
			return hook(name, ro, () -> ro);
		} catch (Throwable e) {
			e.printStackTrace();
			T x = (T) def.get();
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir + name))))) {
				oos.writeObject(x);
			} catch (Throwable e2) {
				e2.printStackTrace();
			}
			return hook(name, x, () -> x);
		}
	}

	/**
	 * loads persisted variable "name" from disk, if you can, otherwise uses the value supplied by "def"
	 * <p>
	 * At exit this calls atEnd.apply(x) where x is the object that was originally passed out of this function (either the object loaded from disk
	 * or the object supplied by "def"). The value returned by atEnd.apply(x) will be persisted in the shutdown hook.
	 */
	static public <T extends Serializable> T persist(String name, Supplier<T> def, Function<T, T> atEnd) {
		if (dir == null) return def.get();

		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dir + name))))) {
			T ro = (T) ois.readObject();
			return hook(name, ro, () -> atEnd.apply(ro));
		} catch (Throwable e) {
			e.printStackTrace();
			T x = (T) def.get();
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir + name))))) {
				oos.writeObject(x);
			} catch (Throwable e2) {
				e2.printStackTrace();
			}
			return hook(name, x, () -> atEnd.apply(x));
		}
	}

	/**
	 * loads persisted variable "name" from disk, if you can, otherwise uses the value supplied by "def"
	 * <p>
	 * Values loaded from disk (or provided by "def") are passed through the validation function "validate".
	 * <p>
	 * At exit this calls atEnd.apply(x) where x is the object that was originally passed out of this function (either the object loaded from disk
	 * or the object supplied by "def"). The value returned by atEnd.apply(x) will be persisted in the shutdown hook.
	 */
	static public <T extends Serializable> T persist(String name, Supplier<T> def, Function<T, T> validate, Function<T, T> atEnd) {
		if (dir == null) return def.get();

		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dir + name))))) {
			T ro = (T) ois.readObject();
			T ro2 = validate.apply(ro);
			return hook(name, ro2, () -> atEnd.apply(ro2));
		} catch (Throwable e) {
			System.out.println(" couldn't load saved preference for "+name+" using compiled default ");
//			e.printStackTrace();
			T x = (T) def.get();
			T x2 = validate.apply(x);
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir + name))))) {
				oos.writeObject(x2);
			} catch (Throwable e2) {
				e2.printStackTrace();
			}
			return hook(name, x2, () -> atEnd.apply(x2));
		}
	}

	private static <T extends Serializable> T hook(String name, T x, Supplier<T> sx) {
		hooks.put(name, () -> {
			boolean success = true;
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir + name+"_tmp"))))) {
				oos.writeObject(sx.get());
			} catch (Throwable e2) {
				e2.printStackTrace();
				success = false;
			}
			if (success)
			{
				try {
					Files.move(Paths.get(dir+name+"_tmp"), Paths.get(dir+name), StandardCopyOption.ATOMIC_MOVE);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		return x;
	}

}
