package fieldbox.io;

import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Log;
import fieldagent.Main;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Callbacks;
import fieldbox.boxes.FrameManipulation;
import fieldbox.boxes.plugins.PluginList;
import fieldbox.boxes.plugins.Variant;
import fieldbox.execution.Execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class handles the persistence of Box graphs to disk. Key design challenge here is to allow individual properties of boxes to exist as separate files on disk if it makes sense to do so (for
 * example disparate pieces of source code), and otherwise have properties associated with a box bound up in a single file the rides near these source code files. These boxes are free to float around
 * the filesystem and to be shared between documents. This is in stark contrast to the Field1 design where a Field "sheet" was a directory that contained all of its boxes. Sharing boxes between
 * documents was therefore completely impossible. This structure will give us the ability to drag and drop documents into the Field window and save side-car .box files next to them that contain the
 * properties.
 * <p>
 * .box (the properties) and .field2 (the master document) files are stored as EDN files (see EDN.java for specifics).
 */
public class IO {
	static public final String WORKSPACE = "{{workspace}}";
	static public final String EXECUTION = "{{execution}}";
	static public final String TEMPLATES = "{{templates}}";

	public static final Dict.Prop<String> id = new Dict.Prop<>("__id__").autoConstructs(() -> Box.newID());
	public static final Dict.Prop<String> desiredBoxClass = new Dict.Prop<>("__desiredBoxClass__");
	public static final Dict.Prop<String> comment = new Dict.Prop<>("comment").toCannon()
		.type()
		.doc("A comment string that's easily searchable. Set it to provide comments on boxes used in templates.");

	public static final Dict.Prop<String> language = new Dict.Prop<>("language").toCannon()
		.type()
		.doc("marks a property's editable (CodeMirror) language")
		.set(Dict.domain, "*/attributes");
	public static final Dict.Prop<Boolean> persistent = new Dict.Prop<>("persistent").toCannon()
		.type()
		.doc("marks a property as being saved to disk")
		.set(Dict.domain, "*/attributes");
	public static final Dict.Prop<Boolean> perDocument = new Dict.Prop<>("perDocument").toCannon()
		.type()
		.doc("marks a property as being saved, conceptually, with the document rather than with the box (which might be shared between documents).")
		.set(Dict.domain, "*/attributes");
	public static final Dict.Prop<Boolean> dontCopy = new Dict.Prop<>("dontCopy").toCannon()
		.type()
		.doc("marks a property as not being copyable between boxes.")
		.set(Dict.domain, "*/attributes");

	public static final Dict.Prop<List<String>> classpath = new Dict.Prop<>("classpath").toCannon()
											    .type()
											    .doc("This box asks the classloader to extend the classpath by this list of paths");

	public static final Dict.Prop<IdempotencyMap<Runnable>> onPreparingToSave = new Dict.Prop<>("onPreparingToSave").toCannon()
														    .type()
														    .doc("Notification that this box is going to be saved to disk");
	public static final Dict.Prop<IdempotencyMap<Runnable>> onFinishingSaving = new Dict.Prop<>("onFinishingSaving").toCannon()
														    .type()
														    .doc("Notification that this box is going to be saved to disk");



	static Set<String> knownProperties = new LinkedHashSet<String>();
	static Map<String, Filespec> knownFiles = new HashMap<String, Filespec>();

	static {
		persist(id);
		persist(comment);
		persist(classpath);
		persist(perDocument);
	}

	private String defaultDirectory;
	private String templateDirectory;


	boolean lastWasNew = false;
	EDN edn = new EDN();
	private PluginList pluginList;


	public IO(String defaultDirectory) {
		try {
			this.defaultDirectory = new File(defaultDirectory).getCanonicalPath();
			this.templateDirectory = new File(Main.app + "/templates/").getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!new File(defaultDirectory).exists()) new File(defaultDirectory).mkdir();

		knownProperties.add(Box.name.getName());
		knownProperties.add(Box.frame.getName());

		knownProperties.add(FrameManipulation.lockHeight.getName());
		knownProperties.add(FrameManipulation.lockWidth.getName());
	}

	public void setDefaultDirectory(String defaultDirectory) throws IOException
	{
		this.defaultDirectory = new File(defaultDirectory).getCanonicalPath();
		if (!new File(defaultDirectory).exists()) new File(defaultDirectory).mkdir();
	}


	static public String readFromFile(File f) {
		try {
			return Files.readAllLines(sanitizeName(f).toPath())
				.stream()
				.reduce((a, b) -> a + "\n" + b).orElse("");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidPathException e) {
			System.err.println(" illegal character in filename ? " + f + ", continuing on");
		}

		return "";
	}

	static public String pad(int n) {
		String r = "" + n;
		while (r.length() < 5) r = "0" + r;
		return r;
	}

	static public void persist(Dict.Prop prop) {
		knownProperties.add(prop.getName());
	}

	public static boolean isPeristant(Dict.Prop prop) {
		return knownFiles.containsKey(prop.getName()) || knownProperties.contains(prop.getName()) || (prop.findCannon() != null && prop.findCannon()
			.getAttributes()
			.isTrue(persistent, false));
	}

	public static Runnable uniqify(Box x) {
		ArrayList<Dict.Prop> k = new ArrayList<>(x.properties.getMap()
			.keySet());

		List<Runnable> undo = new LinkedList<>();

		for (Dict.Prop kk : k) {
			if (kk.getName()
				.startsWith("__filename__") || kk.getName()
				.startsWith("__datafilename__")) {
				Object was = x.properties.remove(kk);
				undo.add(() -> {
					x.properties.put(kk, was);
				});
			}
		}

		if (x.properties.has(IO.id)) {
			String was = x.properties.remove(IO.id);
			undo.add(() -> {
				x.properties.put(IO.id, was);
			});
			x.properties.put(IO.id, Box.newID());
		} else
			x.properties.put(IO.id, Box.newID());

		return () -> {
			for (Runnable u : undo) u.run();
		};
	}


	public static boolean uniqifyIfNecessary(Box root, Box x) {

		String id = x.properties.computeIfAbsent(IO.id, k -> Box.newID());

		Set<String> ids = root.breadthFirstAll(root.both())
			.filter(bx -> bx != x)
			.map(bx -> bx.properties.computeIfAbsent(IO.id, k -> Box.newID()))
			.collect(Collectors.toSet());

		Log.log("updatebox", () -> "all ids are " + ids + " looked for " + id + " " + ids.contains(id));


		if (ids.contains(id)) {
			ArrayList<Dict.Prop> k = new ArrayList<>(x.properties.getMap()
				.keySet());
			for (Dict.Prop kk : k) {
				if (kk.getName()
					.startsWith("__filename__") || kk.getName()
					.startsWith("__datafilename__")) {
					x.properties.remove(kk);
				}
			}

			x.properties.put(IO.id, Box.newID());
			return true;
		}
		return false;
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
			if (f.defaultSuffix.equals(pieces[pieces.length - 1]))
				return new Dict.Prop<String>(f.name).findCannon();
			if (f.defaultSuffix.equals(EXECUTION)) {
				Optional<Execution> e = root.find(Execution.execution, root.both())
					.findFirst();
				if (e.isPresent()) if (e.get()
					.support(root, new Dict.Prop<String>(f.name))
					.getDefaultFileExtension()
					.equals(pieces[pieces.length - 1]))
					return new Dict.Prop<String>(f.name).findCannon();
			}
		}

		return null;
	}

	public String getLanguageForProperty(Dict.Prop<String> editingProperty) {
		for (Filespec f : knownFiles.values()) {
			if (f.name.equals(editingProperty.getName())) return f.language;
		}

		return editingProperty.getAttributes()
			.get(language);
	}

	public boolean isBoxFile(String filename) {
		String[] pieces = filename.split("\\.");
		if (pieces.length < 2) return false;
		return pieces[pieces.length - 1].equals("box");
	}

	public boolean isFieldFile(String filename) {
		return filename.endsWith(".field2") || filename.endsWith(".field");
	}

	public Document compileDocument(String defaultSubDirectory, Box documentRoot, Map<Box, String> specialBoxes) {
		return compileDocument(defaultSubDirectory, documentRoot, x -> true, specialBoxes);
	}

	public Document compileDocument(String defaultSubDirectory, Box documentRoot, Predicate<Box> include, Map<Box, String> specialBoxes) {
		Document d = new Document();

		Callbacks.call_runnable(documentRoot, onPreparingToSave, null, false, documentRoot.both());
		try (Variant.Memo memo = Variant.connectAll(documentRoot)) {

			d.externalList = documentRoot.breadthFirstAll(documentRoot.allDownwardsFrom())
				.filter(include)
				.map(box -> toExternal(defaultSubDirectory, box, specialBoxes))
				.filter(x -> x != null)
				.collect(Collectors.toList());
			d.knownFiles = new LinkedHashMap<>(knownFiles);
			d.knownProperties = new LinkedHashSet<>(knownProperties);
		}
		finally
		{
			Callbacks.call_runnable(documentRoot, onFinishingSaving, null, false, documentRoot.both());
		}
		return d;
	}

	public String getDefaultDirectory() {
		return defaultDirectory;
	}

	public String getTemplateDirectory() {
		return templateDirectory;
	}

	public IO setPluginList(PluginList pluginList) {
		this.pluginList = pluginList;
		return this;
	}

	public Document readDocument(String filename, Map<String, Box> specialBoxes, Set<Box> created) {
		File f = filenameFor(filename);

		Log.log("io.general", () -> " reading document :" + f);

		lastWasNew = false;

		if (!f.exists()) {
			// new file
			lastWasNew = true;

			Log.log("io.general", () -> " document doesn't exist ");

			Document d = new Document();
			d.externalList = new ArrayList<>();
			d.knownFiles = new LinkedHashMap<>();
			d.knownProperties = new LinkedHashSet<>();
			return d;
		}


		String m = readFromFile(f);

		Document d = (Document) new EDN().read(m);
		Map<String, Box> loaded = new HashMap<String, Box>();

		Log.log("io.general", () -> " document contains " + d.externalList.size() + " boxes ");


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
				else
					Log.log("io.error", () -> " lost child ? " + id + " of " + e.box + " " + specialBoxes);
			}
			for (String id : e.parents) {
				Box mc = specialBoxes.getOrDefault(id, loaded.get(id));

				if (mc != null) mc.connect(e.box);
				else
					Log.log("io.error", () -> " lost child ? " + id + " of " + e.box + " " + specialBoxes);
			}
		}
		created.addAll(loaded.values());

		LinkedHashSet<Box> failed = new LinkedHashSet<>();
		loaded.values()
			.stream()
			.filter(b -> b instanceof Loaded)
			.forEach(b -> {
				try {
					((Loaded) b).loaded();
				} catch (Throwable t) {
					Log.log("io.error", () -> " exception thrown while finishing loading for box " + b);
					Log.log("io.error", () -> t);
					Log.log("io.error", () -> " continuing on (box will be deleted from the graph without notification)");
					failed.add(b);
				}
			});

		loaded.values()
			.removeAll(failed);
		loaded.values()
			.forEach(b -> Callbacks.load(b));

		for (Box b : failed) {
			b.disconnectFromAll();
		}
		return d;


	}

	private void fromExternal(External ex, Map<String, Box> specialBoxes) {

		File dataFile = filenameFor(ex.dataFile);

		String currentPrefix = dataFile.getParent();

		Map<String, List<Object>> options = null;
		if (pluginList != null) try {
			options = pluginList.read(dataFile.getAbsolutePath(), false);
			pluginList.interpretClassPathAndOptions(options);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Class c = this.getClass()
				.getClassLoader()
				.loadClass(ex.boxClass);
			try {
				Constructor<Box> cc = c.getDeclaredConstructor();
				cc.setAccessible(true);
				ex.box = cc.newInstance();
				ex.box.properties.put(desiredBoxClass, ex.boxClass);
			} catch (NoSuchMethodException e) {
				Constructor<Box> cc = c.getDeclaredConstructor(Box.class);
				cc.setAccessible(true);
				ex.box = cc.newInstance(specialBoxes.get(">>root<<"));
			}
		} catch (Throwable e) {
			Log.log("io.error", () -> " while looking for class <" + ex.boxClass + "> needed for <" + ex.id + " / " + ex.textFiles + "> an exception was thrown");
			Log.log("io.error", () -> e);
			Log.log("io.error", () -> " will proceed with just a vanilla Box class, but custom behavior will be lost ");
			ex.box = new Box();

			ex.box.properties.put(desiredBoxClass, ex.boxClass);
		}

		for (Map.Entry<String, String> e : ex.textFiles.entrySet()) {
			File filename = filenameFor(currentPrefix, e.getValue());
			String text = readFromFile(filename);
			ex.box.properties.put(new Dict.Prop<String>(e.getKey()), text);
		}


		String read = readFromFile(dataFile);
		if (read != null) {
			try {
				Map<?, ?> m = (Map) serializeFromString(read);
				for (Map.Entry<?, ?> entry : m.entrySet()) {
					Dict.Prop p = new Dict.Prop((String) entry.getKey());
					p.toCannon().getAttributes().put(persistent, true);
					ex.box.properties.put(p, entry.getValue());
				}
			} catch (Exception e) {
				Log.log("io.error", () -> "trouble loading external " + dataFile + ". Corrupt file?");
				e.printStackTrace();
			}
		}

		if (ex.overriddenProperties != null) {
			ex.overriddenProperties.forEach((k, v) -> {
				Dict.Prop p = new Dict.Prop("" + k);
				ex.box.properties.put(p, v);
				p.toCannon()
					.getAttributes()
					.put(perDocument, true);
				p.toCannon()
					.getAttributes()
					.put(persistent, true);
			});
		}

	}

	/**
	 * notes: it's up to the caller to call .loaded() once this box has been connected to the graph
	 */
	public Box loadSingleBox(String f, Box root) {

		Map<String, List<Object>> options = null;
		if (pluginList != null) try {
			options = pluginList.read(f, false);
			pluginList.interpretClassPathAndOptions(options);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<Object, String> m = (Map) (serializeFromString(readFromFile(filenameFor(f))));

		String boxClass = m.getOrDefault("__boxclass__", Box.class.getName());
		Box box;

		try {
			Class c = this.getClass()
				.getClassLoader()
				.loadClass(boxClass);
			try {
				Constructor<Box> cc = c.getDeclaredConstructor();
				cc.setAccessible(true);
				box = cc.newInstance();
			} catch (NoSuchMethodException e) {
				Constructor<Box> cc = c.getDeclaredConstructor(Box.class);
				cc.setAccessible(true);
				box = cc.newInstance(root);
			}
		} catch (Throwable e) {
			Log.log("io.error", () -> " while looking for class <" + boxClass + "> an exception was thrown");
			Log.log("io.error", () -> e);
			Log.log("io.error", () -> " will proceed with just a vanilla Box class, but custom behavior will be lost ");
			box = new Box();
		}

		String currentPrefix = new File(f).getParent();


		for (Map.Entry<?, ?> entry : m.entrySet()) {
			if (!(entry.getKey() instanceof String)) continue;

			box.properties.put(new Dict.Prop((String) entry.getKey()), entry.getValue());

			if (((String) entry.getKey()).startsWith("__filename__")) {
				String suffix = ((String) entry.getKey()).substring("__filename__".length());

				String p = (String) entry.getValue();

				String additionalSearchPath = p.replace(WORKSPACE, currentPrefix + "/");
				if (new File(additionalSearchPath).exists()) {
					String p1 = readFromFile(new File(additionalSearchPath));
					box.properties.put(new Dict.Prop<String>(suffix), p1);
				} else {
					File path = filenameFor(p);
					try {

						String p1 = readFromFile(path);
						box.properties.put(new Dict.Prop<String>(suffix), p1);
					} catch (Exception e) {
						System.err.println(" exception thrown while loading a file :" + path + " for property :" + entry + " in box +" + box);
						e.printStackTrace();
						System.err.println(" continuing on");
					}
				}
			}
		}

		for (Map.Entry<String, Filespec> e : knownFiles.entrySet()) {
			File filename = filenameFor(f + (e.getValue()
				.getDefaultSuffix(root)
				.startsWith(".") ? "" : ".") + e.getValue()
				.getDefaultSuffix(root));

			if (filename.exists()) {
				String text = readFromFile(filename);
				box.properties.put(new Dict.Prop<String>(e.getKey()), text);
			}
		}

		if (m.get("__boxclass__") != null) box.properties.put(desiredBoxClass, m.get("__boxclass__"));


		return box;

	}

	protected External toExternal(String defaultSubDirectory, Box box, Map<Box, String> specialBoxes) {
		if (specialBoxes.containsKey(box)) return null;

		if (box.properties.isTrue(Boxes.dontSave, false)) return null;

		External ex = new External();

		ex.id = box.properties.computeIfAbsent(id, (k) -> UUID.randomUUID()
			.toString());
		ex.box = box;

		ex.dataFile = relativize(box.properties.computeIfAbsent(new Dict.Prop<String>("__datafilename__"), (k) -> relativize(makeDataFilenameFor(defaultSubDirectory, ex, box))));
		box.properties.put(new Dict.Prop<String>("__datafilename__"), ex.dataFile);

		System.out.println(" set datafilename to be :" + ex.dataFile);

		knownProperties.add("__datafilename__");

		for (Map.Entry<Dict.Prop, Object> e : new LinkedHashMap<>(box.properties.getMap()).entrySet()) {
			Log.log("io.general", () -> "checking :" + e.getKey()
				.getName() + " against " + knownFiles.keySet());
			if (knownFiles.containsKey(e.getKey()
				.getName())) {
				Filespec f = knownFiles.get(e.getKey()
					.getName());

				String extantFilename = box.properties.get(new Dict.Prop<String>("__filename__" + e.getKey()
					.getName()));

				String fextent = extantFilename;
				Log.log("io.general", () -> "extant filename is :" + fextent + " for " + e.getKey()
					.getName() + " from " + box.properties);

				// this is wrong. Don't set a name if we are just going to use a default; otherwise, when you move the file you have to update it.
				// key it on the datafilename

				if (extantFilename == null) {
					extantFilename = relativize(ex.dataFile + f.getDefaultSuffix(box));
					box.properties.put(new Dict.Prop<String>("__filename__" + e.getKey()
						.getName()), relativize(extantFilename));
					ex.textFiles.put(e.getKey()
						.getName(), relativize(extantFilename));
				} else {
					ex.textFiles.put(e.getKey()
						.getName(), relativize(extantFilename));
				}
				knownProperties.add("__filename__" + e.getKey()
					.getName());
			}
		}


		ex.parents = new ArrayList<>();
		ex.children = new ArrayList<>();

		Set<Box> p = box.parents();
		for (Box pp : p) {
			if (specialBoxes.containsKey(pp)) ex.parents.add(specialBoxes.get(pp));
			else ex.parents.add(pp.properties.computeIfAbsent(id, (k) -> UUID.randomUUID()
				.toString()));
		}

		p = box.children();
		for (Box pp : p) {
			if (specialBoxes.containsKey(pp)) ex.children.add(specialBoxes.get(pp));
			else ex.children.add(pp.properties.computeIfAbsent(id, (k) -> UUID.randomUUID()
				.toString()));
		}

		ex.boxClass = box.getClass()
			.equals(Box.class) ? box.properties.getOr(desiredBoxClass, () -> Box.class.getName()) : box.getClass()
			.getName();

		return ex;
	}

	public void writeOutDocument(String filename, Document d) throws IOException {

		String prefix = new File(filename).getParent() + "/";

		for (External e : d.externalList)
			writeOutExternal(prefix, e);

		writeToFile(filenameFor(filename), serializeToString(d));
	}

	protected void writeOutExternal(String defaultPrefix, External external) throws IOException {
		for (Map.Entry<String, String> e : external.textFiles.entrySet()) {
			String text = external.box.properties.get(new Dict.Prop<String>(e.getKey()));
			if (text == null) continue;
			File filename = filenameFor(defaultPrefix, e.getValue());

			try {
				writeToFile(filename, text);
			} catch (Exception ex) {
				System.err.println(" exception thrown while saving out a file :" + filename + " for property :" + e + " in box +" + external.box);
				ex.printStackTrace();
				System.err.println(" continuing on");
			}
		}

		File dataFile = filenameFor(external.dataFile);

		if (dataFile != null) {
			Map<String, Object> data = new LinkedHashMap<String, Object>();

			Set<Map.Entry<Dict.Prop, Object>> es = external.box.properties.getMap()
				.entrySet();
			for (Map.Entry<Dict.Prop, Object> e : es)
				if (isPeristant(e.getKey())) data.put(e.getKey()
					.getName(), e.getValue());


			data.put("__boxclass__", external.boxClass);

			writeToFile(dataFile, serializeToString(data));
		}

		Log.log("io.general", () -> "sweep for persistent, perDocument properties");
		for (Map.Entry<Dict.Prop, Object> e : external.box.properties.getMap()
			.entrySet()) {
			Log.log("io.general", () -> "property :" + e.getKey()
				.toCannon() + " attributes are " + e.getKey()
				.toCannon()
				.getAttributes());

			if (e.getKey()
				.toCannon()
				.getAttributes()
				.isTrue(perDocument, false) && isPeristant(e.getKey()))
				external.overriddenProperties.put(e.getKey()
					.getName(), e.getValue());
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

	private void writeToFile(File filename, String text) throws IOException {
		filename = sanitizeName(filename);
		final File finalFilename = filename;
		Log.log("io.general", () -> " will write :" + text + " to " + finalFilename);

		if (!filename.getParentFile().exists()) filename.getParentFile().mkdirs();

		BufferedWriter w = new BufferedWriter(new FileWriter(filename));
		w.append(text);
		w.close();
	}

	static private File sanitizeName(File filename) {

		String f = filename.getAbsolutePath();
		f = f.replaceAll(">", "_");
		f = f.replaceAll("\\?", "_");
		return new File(f);
	}

	public File filenameFor(String value) {
		if (value.startsWith(TEMPLATES)) {
			return new File(templateDirectory,
					/*safe*/(value.substring(TEMPLATES.length())));
		}
		if (value.startsWith(WORKSPACE)) {
			return new File(defaultDirectory,
					/*safe*/(value.substring(WORKSPACE.length())));
		}
		return new File(value);
	}

	public File filenameFor(String defaultPrefix, String value) {
		if (value.startsWith(TEMPLATES)) {
			return new File(templateDirectory,
					/*safe*/(value.substring(TEMPLATES.length())));
		}
		if (value.startsWith(WORKSPACE)) {
			return new File(defaultDirectory,
					/*safe*/(value.substring(WORKSPACE.length())));
		}
		return filenameFor(defaultPrefix + "/" + value);
	}

	private String safe(String filename) {
		while (filename.startsWith("/")) filename = filename.substring(1);
		return filename.replace("/", "_");
	}

	private String makeDataFilenameFor(String defaultSubDirectory, External ex, Box box) {
//		if (ex.textFiles.size() > 0) {
//			return ex.textFiles.values()
//					   .iterator()
//					   .next() + ".box";
//		}
		return makeFilenameFor(defaultSubDirectory, ".box", "", box);
	}

	private String makeFilenameFor(String defaultSubDirectory, Filespec f, Box box) {
		return makeFilenameFor(defaultSubDirectory, f.getDefaultSuffix(box), safe(f.name), box);
	}

	private String makeFilenameFor(String defaultSubDirectory, String defaultSuffix, String defaultName, Box box) {

		String name = box.properties.get(Box.name);
		if (name == null) name = "untitled_box";

		String suffix = defaultName + (defaultSuffix == null ? "" : defaultSuffix);
		name = safe(name + suffix);

		int n = 0;
		if (filenameFor(WORKSPACE, defaultSubDirectory + "/" + name).exists()) {
			String n2 = name;
			while (filenameFor(WORKSPACE, defaultSubDirectory + "/" + n2).exists()) {
				n2 = name.substring(0, name.length() - suffix.length()) + pad(n) + suffix;
				n++;
			}
			name = n2;
		}

		// create it now, so that other calls to makeFilenameFor still create unique names
		try {
			File fn = filenameFor(WORKSPACE, defaultSubDirectory + "/" + name);
			fn.getParentFile().mkdirs();
			fn.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String q = filenameFor(WORKSPACE, defaultSubDirectory + "/" + name).getAbsolutePath();

		return relativize(q);
	}

	private String relativize(String q) {

		if (q.contains("{{")) {
			q = q.substring(q.lastIndexOf("{{"));
		}

		while (q.contains("//")) q = q.replace("//", "/");

		String q2 = q.replace(defaultDirectory, WORKSPACE)
			.replace(templateDirectory, TEMPLATES);
		return q2;


	}

	public String findTemplateCalled(String name) {
		String q = findTemplateCalled(name, templateDirectory);
		if (q != null) return q;
		q = findTemplateCalled(name, defaultDirectory);
		return q;
	}

	public List<String> findTemplateStartingWith(String x) {
		List<String> r = new ArrayList<>();
		r.addAll(findTemplateStartingWith(x, templateDirectory));
		r.addAll(findTemplateStartingWith(x, defaultDirectory));
		return r;
	}


	protected String findTemplateCalled(String name, String dir) {
		{
			File[] n = new File(dir).listFiles((x) -> x.getName()
				.endsWith(".box"));
			if (n != null) {
				for (File f : n) {
					String[] pieces = f.getName()
						.split("[_\\.]");

					if (pieces[0].toLowerCase()
						.equals(name.toLowerCase())) return f.getAbsolutePath();
				}
			}

			if (new File(dir + "/" + name + ".box").exists())
				return new File(dir + "/" + name + ".box").getAbsolutePath();
		}
		// single box directory case
		{
			File[] n = new File(dir).listFiles((x) -> x.isDirectory() && x.getName().endsWith(name) && x.listFiles(y -> y.getName().endsWith(".box")).length == 1);

			if (n != null && n.length > 0) {
				return n[0].listFiles(x -> x.getName().endsWith(".box"))[0].getAbsolutePath();
			}
		}
		// multiple box directory case
		{
			File[] n = new File(dir).listFiles((x) -> x.isDirectory() && x.getName().endsWith(name) && x.listFiles(y -> y.getName().endsWith(".box")).length > 0);

			if (n != null && n.length > 0) {
				return n[0].listFiles(x -> x.getName().endsWith(".field2"))[0].getAbsolutePath();
			}
		}


		return null;
	}

	protected List<String> findTemplateStartingWith(String prefix, String dir) {
		List<String> r = new ArrayList<>();
		{
			File[] n = new File(dir).listFiles((x) -> x.getName()
				.endsWith(".box"));
			if (n != null) {
				for (File f : n) {
					String[] pieces = f.getName()
						.split("[_\\.]");

					if (pieces[0].toLowerCase()
						.startsWith(prefix.toLowerCase())) r.add(pieces[0].toLowerCase());
				}
			}

			if (new File(dir + "/" + prefix + ".box").exists()) r.add(prefix);
		}
		// single box directory case
		{
			File[] n = new File(dir).listFiles((x) -> x.isDirectory() && x.getName().startsWith(prefix) && x.listFiles(y -> y.getName().endsWith(".box")).length == 1);

			if (n != null && n.length > 0) {
				for (File nn : n) {
					r.add(nn.getName());
				}
			}


		}

		return r;
	}


	/**
	 * tag interface for boxes, method will be called after all boxes have been loaded (and all properties set). Boxes that throw exceptions won't be loaded after all.
	 */
	public interface Loaded {
		void loaded();
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
		public Collection<String> parents;
		public Collection<String> children;
		public Map overriddenProperties = new LinkedHashMap<>();
		transient Box box;
	}

	public class Filespec {
		public String name;
		private String defaultSuffix;
		private String language;

		public String getDefaultSuffix(Box box) {

			if (defaultSuffix.equals(EXECUTION)) {
				Execution e = box.first(Execution.execution)
					.orElseThrow(() -> new IllegalArgumentException(" no execution for box " + box));
				return e.support(e, new Dict.Prop<String>(name))
					.getDefaultFileExtension();
			}

			return defaultSuffix;
		}

		public String getLanguage(Box box) {

			if (defaultSuffix.equals(EXECUTION)) {
				Execution e = box.first(Execution.execution)
					.orElseThrow(() -> new IllegalArgumentException(" no execution for box " + box));
				return e.support(e, new Dict.Prop<String>(name))
					.getDefaultFileExtension();
			}

			return language;
		}
	}


}
