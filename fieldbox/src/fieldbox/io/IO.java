package fieldbox.io;

import field.utility.Dict;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.FrameManipulation;
import fieldbox.execution.Execution;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class handles the persistence of Box graphs to disk. Key design challenge here is to allow individual properties of boxes to exist as separate
 * files on disk if it makes sense to do so (for example disparate pieces of source code), and otherwise have properties associated with a box bound
 * up in a single file the rides near these source code files. These boxes are free to float around the filesystem and to be shared between documents.
 * This is in stark contrast to the Field1 design where a Field "sheet" was a directory that contained all of its boxes. Sharing boxes between
 * documents was therefore completely impossible. This structure will give us the ability to drag and drop documents into the Field window and save
 * side-car .fieldbox files next to them that contain the properties.
 * <p>
 * .fieldbox (the properties) and .field2 (the master document) files are stored as EDN files (see EDN.java for specifics).
 */
public class IO {
	static public final String WORKSPACE = "{{workspace}}";
	static public final String EXECUTION = "{{execution}}";


	/**
	 * tag interface for boxes, method will be called after all boxes have been loaded (and all properties set)
	 */
	static public interface Loaded {
		public void loaded();
	}

	private final String defaultDirectory;
	static Set<String> knownProperties = new LinkedHashSet<String>();
	static Map<String, Filespec> knownFiles = new HashMap<String, Filespec>();

	public static final Dict.Prop<String> id = new Dict.Prop<>("__id__");
	public static final Dict.Prop<String> comment= new Dict.Prop<>("comment").toCannon().type().doc("A comment string that's easily searchable");

	static
	{
		persist(id);
		persist(comment);
	}

	public void addFilespec(String name, String defaultSuffix, String language) {
		Filespec f = new Filespec();
		f.name = name;
		f.defaultSuffix = defaultSuffix;
		f.language = language;
		knownFiles.put(f.name, f);
	}


	public Dict.Prop<String> lookupFileSuffix(String filename, Box root) {
		String[] pieces = filename.split("\\.");
		if (pieces.length < 2) return null;
		for (Filespec f : knownFiles.values()) {
			if (f.defaultSuffix.equals(pieces[pieces.length - 1])) return new Dict.Prop<String>(f.name).findCannon();
			if (f.defaultSuffix.equals(EXECUTION)) {
				Optional<Execution> e = root.find(Execution.execution, root.both()).findFirst();
				if (e.isPresent()) if (e.get().support(root, new Dict.Prop<String>(f.name)).getDefaultFileExtension()
					    .equals(pieces[pieces.length - 1])) return new Dict.Prop<String>(f.name).findCannon();
			}
		}

		return null;
	}

	public String getLanguageForProperty(Dict.Prop<String> editingProperty) {
		for (Filespec f : knownFiles.values()) {
			if (f.name.equals(editingProperty.getName())) return f.language;
		}
		return null;
	}


	public boolean isBoxFile(String filename) {
		String[] pieces = filename.split("\\.");
		if (pieces.length < 2) return false;
		return pieces[pieces.length - 1].equals("box");
	}

	public class Filespec {
		public String name;
		private String defaultSuffix;
		private String language;

		public String getDefaultSuffix(Box box) {

			if (defaultSuffix.equals(EXECUTION)) {
				Execution e = box.first(Execution.execution)
					    .orElseThrow(() -> new IllegalArgumentException(" no execution for box " + box));
				return e.support(e, new Dict.Prop<String>(name)).getDefaultFileExtension();
			}

			return defaultSuffix;
		}

		public String getLanguage(Box box) {

			if (defaultSuffix.equals(EXECUTION)) {
				Execution e = box.first(Execution.execution)
					    .orElseThrow(() -> new IllegalArgumentException(" no execution for box " + box));
				return e.support(e, new Dict.Prop<String>(name)).getDefaultFileExtension();
			}

			return language;
		}
	}

	public IO(String defaultDirectory) {
		this.defaultDirectory = defaultDirectory;

		if (!new File(defaultDirectory).exists()) new File(defaultDirectory).mkdir();

		knownProperties.add(Box.name.getName());
		knownProperties.add(Box.frame.getName());

		knownProperties.add(FrameManipulation.lockHeight.getName());
		knownProperties.add(FrameManipulation.lockWidth.getName());
	}

	public Document compileDocument(Box documentRoot, Map<Box, String> specialBoxes) {
		Document d = new Document();
		d.externalList = documentRoot.breadthFirst(documentRoot.downwards()).map(box -> toExternal(box, specialBoxes)).filter(x -> x != null)
			    .collect(Collectors.toList());
		d.knownFiles = new LinkedHashMap<>(knownFiles);
		d.knownProperties = new LinkedHashSet<>(knownProperties);
		return d;
	}

	public String getDefaultDirectory() {
		return defaultDirectory;
	}

	boolean lastWasNew = false;

	public Document readDocument(String filename, Map<String, Box> specialBoxes, Set<Box> created) {
		File f = filenameFor(filename);

		Log.log("io.general", " reading document :" + f);

		lastWasNew = false;

		if (!f.exists()) {
			// new file
			lastWasNew = true;

			Log.log("io.general", " document doesn't exist ");

			Document d = new Document();
			d.externalList = new ArrayList<>();
			d.knownFiles = new LinkedHashMap<>();
			d.knownProperties = new LinkedHashSet<>();
			return d;
		}

		String m = readFromFile(f);

		Document d = (Document) new EDN().read(m);
		Map<String, Box> loaded = new HashMap<String, Box>();

		Log.log("io.general", " document contains " + d.externalList.size() + " boxes ");


		for (External e : d.externalList) {
			fromExternal(e, specialBoxes);
			if (e.box != null) {
				loaded.put(e.id, e.box);
				e.box.properties.put(id, e.id);
			}
		}

		for (External e : d.externalList) {
			for (String id : e.children) {
				Box mc = specialBoxes.getOrDefault(id, loaded.get(id));

				if (mc != null) e.box.connect(mc);
				else Log.log("io.error", " lost child ? " + id + " of " + e.box + " " + specialBoxes);
			}
			for (String id : e.parents) {
				Box mc = specialBoxes.getOrDefault(id, loaded.get(id));

				if (mc != null) mc.connect(e.box);
				else Log.log("io.error", " lost child ? " + id + " of " + e.box + " " + specialBoxes);
			}
		}
		created.addAll(loaded.values());

		loaded.values().stream().filter(b -> b instanceof Loaded).forEach(b -> {
			try {
				((Loaded) b).loaded();
			} catch (Throwable t) {
				Log.log("io.error", " exception thrown while finishing loading for box " + b);
				Log.log("io.error", t);
				Log.log("io.error", " continuing on...");
			}
		});

		return d;


	}

	private void fromExternal(External ex, Map<String, Box> specialBoxes) {

//		ex.box = new Box();
		try {
			Class c = this.getClass().getClassLoader().loadClass(ex.boxClass);
			Constructor<Box> cc = c.getDeclaredConstructor();
			cc.setAccessible(true);
			ex.box = (Box) cc.newInstance();
		} catch (Throwable e) {
			Log.log("io.error", " while looking for class <" + ex.boxClass + "> needed for <" + ex.id + " / " + ex.textFiles + "> an exception was thrown");
			Log.log("io.error", e);
			Log.log("io.error", " will proceed with just a vanilla Box class, but custom behavior will be lost ");
			ex.box = new Box();
		}

		for (Map.Entry<String, String> e : ex.textFiles.entrySet()) {
			File filename = filenameFor(e.getValue());
			String text = readFromFile(filename);
			ex.box.properties.put(new Dict.Prop<String>(e.getKey()), text);
		}

		File dataFile = filenameFor(ex.dataFile);

		String read = readFromFile(dataFile);
		if (read != null) {
			Map<?, ?> m = (Map) serializeFromString(read);
			for (Map.Entry<?, ?> entry : m.entrySet()) {
				ex.box.properties.put(new Dict.Prop((String) entry.getKey()), entry.getValue());
			}
		}

	}

	public Box loadSingleBox(String f) {

		Map<Object, String> m = (Map) (serializeFromString(readFromFile(filenameFor(f))));

		String boxClass = m.getOrDefault("__boxclass__", Box.class.getName());

		Box box;

		try {
			Class c = this.getClass().getClassLoader().loadClass(boxClass);
			Constructor<Box> cc = c.getDeclaredConstructor();
			cc.setAccessible(true);
			box = (Box) cc.newInstance();
		} catch (Throwable e) {
			Log.log("io.error", " while looking for class <" + boxClass + "> an exception was thrown");
			Log.log("io.error", e);
			Log.log("io.error", " will proceed with just a vanilla Box class, but custom behavior will be lost ");
			box = new Box();
		}


		String selfFilename = m.getOrDefault("__datafilename__", null);
		String selfPrefix = new File(selfFilename).getParent();

		String currentPrefix = new File(f).getParent();

		for (Map.Entry<?, ?> entry : m.entrySet()) {
			box.properties.put(new Dict.Prop((String) entry.getKey()), entry.getValue());

			if (((String) entry.getKey()).startsWith("__filename__")) {
				String suffix = ((String) entry.getKey()).substring("__filename__".length());

				String p = (String) entry.getValue();

				//TODO: rewrite p if it's prefixed with selfFilename (splitting by '/' to handle subdirectories)

				File path = filenameFor(p);

				box.properties.put(new Dict.Prop<String>(suffix), readFromFile(path));
			}

		}

		return box;


	}


	protected External toExternal(Box box, Map<Box, String> specialBoxes) {
		if (specialBoxes.containsKey(box)) return null;

		if (box.properties.isTrue(Boxes.dontSave, false)) return null;

		External ex = new External();

		for (Map.Entry<Dict.Prop, Object> e : new LinkedHashMap<>(box.properties.getMap()).entrySet()) {
			Log.log("io.general", "checking :"+e.getKey().getName()+" against "+knownFiles.keySet());
			if (knownFiles.containsKey(e.getKey().getName())) {
				Filespec f = knownFiles.get(e.getKey().getName());

				String extantFilename = box.properties.get(new Dict.Prop<String>("__filename__" + e.getKey().getName()));
				Log.log("io.general", "extant filename is :"+extantFilename+" for "+e.getKey().getName()+" from "+box.properties);
				if (extantFilename == null) {
					box.properties.put(new Dict.Prop<String>("__filename__" + e.getKey()
						    .getName()), extantFilename = makeFilenameFor(f, box));
				}
				knownProperties.add("__filename__" + e.getKey().getName());
				ex.textFiles.put(e.getKey().getName(), extantFilename);
			}
		}

		ex.dataFile = box.properties.computeIfAbsent(new Dict.Prop<String>("__datafilename__"), (k) -> makeDataFilenameFor(ex, box));
		knownProperties.add("__datafilename__");
		ex.box = box;
		ex.id = box.properties.computeIfAbsent(id, (k) -> UUID.randomUUID().toString());

		ex.parents = new LinkedHashSet<>();
		ex.children = new LinkedHashSet<>();

		Set<Box> p = box.parents();
		for (Box pp : p) {
			if (specialBoxes.containsKey(pp)) ex.parents.add(specialBoxes.get(pp));
			else ex.parents.add(pp.properties.computeIfAbsent(id, (k) -> UUID.randomUUID().toString()));
		}

		p = box.children();
		for (Box pp : p) {
			if (specialBoxes.containsKey(pp)) ex.children.add(specialBoxes.get(pp));
			else ex.children.add(pp.properties.computeIfAbsent(id, (k) -> UUID.randomUUID().toString()));
		}

		ex.boxClass = box.getClass().getName();

		return ex;
	}

	public void writeOutDocument(String filename, Document d) throws IOException {
		for (External e : d.externalList)
			writeOutExternal(e);
		writeToFile(filenameFor(filename), serializeToString(d));
	}


	protected void writeOutExternal(External external) throws IOException {
		for (Map.Entry<String, String> e : external.textFiles.entrySet()) {
			String text = external.box.properties.get(new Dict.Prop<String>(e.getKey()));
			if (text == null) continue;
			File filename = filenameFor(e.getValue());
			writeToFile(filename, text);
		}
		;

		File dataFile = filenameFor(external.dataFile);
		if (dataFile != null) {
			Map<String, Object> data = new LinkedHashMap<String, Object>();
			for (String kp : knownProperties) {
				Object d = external.box.properties.get(new Dict.Prop(kp));
				if (d != null) data.put(kp, d);
			}

			data.put("__boxclass__", external.boxClass);

			writeToFile(dataFile, serializeToString(data));
		}
	}

	private String serializeToString(Object data) {
		String written = edn.write(data);
		Log.log("io.general", () -> "edn is " + written);
		return written;
	}

	private Object serializeFromString(String data) {
		Object written = edn.read(data);
		return written;
	}

	EDN edn = new EDN();

	private void writeToFile(File filename, String text) throws IOException {
		Log.log("io.general", " will write :" + text + " to " + filename);
		BufferedWriter w = new BufferedWriter(new FileWriter(filename));
		w.append(text);
		w.close();
	}

	static public String readFromFile(File f) {
		try (BufferedReader r = new BufferedReader(new FileReader(f))) {
			String m = "";
			while (r.ready()) m += r.readLine() + "\n";
			return m;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}


	private File filenameFor(String value) {
		if (value.startsWith(WORKSPACE)) {
			return new File(defaultDirectory, safe(value.substring(WORKSPACE.length())));
		}
		return new File(value);
	}

	private String safe(String filename) {
		while (filename.startsWith("/")) filename = filename.substring(1);
		return filename.replace("/", "_");
	}

	private String makeDataFilenameFor(External ex, Box box) {
		if (ex.textFiles.size() > 0) {
			return ex.textFiles.values().iterator().next() + ".box";
		}
		return makeFilenameFor(".box", "", box);
	}

	private String makeFilenameFor(Filespec f, Box box) {
		return makeFilenameFor(f.getDefaultSuffix(box), f.name, box);
	}

	private String makeFilenameFor(String defaultSuffix, String defaultName, Box box) {
		String name = box.properties.get(Box.name);
		if (name == null) name = "untitled_box";

		String suffix = "_" + defaultName + (defaultSuffix == null ? "" : defaultSuffix);
		name = name + suffix;

		int n = 0;
		if (new File(defaultDirectory, name).exists()) {
			String n2 = name;
			while (new File(defaultDirectory, n2).exists()) {
				n2 = name.substring(0, name.length() - suffix.length()) + pad(n) + suffix;
				n++;
			}
			name = n2;

			// create it now, so that other calls to makeFilenameFor still create unique names
			try {
				new File(defaultDirectory, name).createNewFile();
			} catch (IOException e) {
			}

		}

		return WORKSPACE + name;
	}


	static private String pad(int n) {
		String r = "" + n;
		while (r.length() < 5) r = "0" + r;
		return r;
	}

	static public class Document {
		public List<External> externalList;
		public Map<String, Filespec> knownFiles;
		public Set<String> knownProperties;
	}

	static public class External {
		public Map<String, String> textFiles = new LinkedHashMap<String, String>();
		public String dataFile;
		public String id;
		public String boxClass;

		transient Box box;

		public Set<String> parents;
		public Set<String> children;
	}


	static public void persist(Dict.Prop prop) {
		knownProperties.add(prop.getName());
	}


	public static void uniqify(Box x) {
		ArrayList<Dict.Prop> k = new ArrayList<>(x.properties.getMap()
								     .keySet());
		for (Dict.Prop kk : k) {
			if (kk.getName()
			      .startsWith("__filename__") || kk.getName()
							       .startsWith("__datafilename__")) {
				x.properties.remove(kk);
			}
		}

		x.properties.put(IO.id, UUID.randomUUID()
					    .toString());
	}

}
