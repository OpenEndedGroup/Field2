package field.utility;

import java.io.*;
import java.util.function.Supplier;

/**
 * Automatically serialize things out to places on disk, per user
 */
public class AutoPersist {

	static protected String dir = Options.getDirectory("preferences", () -> System.getProperty("user.home") + "/.field/");

	static {
		new File(dir.substring(0, dir.length() - 1)).mkdirs();
		if (!new File(dir.substring(0, dir.length() - 1)).exists()) {
			System.err.println("WARNING: preferences dir '" + dir + "' does not exist");
			dir = null;
		}
	}

	static public <T extends Serializable> T persist(String name, Supplier<T> def) {
		if (dir == null) return def.get();

		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dir + name))))) {
			return (T) ois.readObject();
		} catch (Throwable e) {
			e.printStackTrace();
			T x = (T)def.get();
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir + name))))) {
				oos.writeObject(x);
			} catch (Throwable e2) {
				e2.printStackTrace();
			}
			return x;
		}
	}

}
