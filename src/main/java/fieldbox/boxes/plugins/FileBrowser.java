package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.Open;
import fieldbox.boxes.*;
import fieldbox.io.EDN;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fielded.Commands;
import fielded.RemoteEditor;
import fielded.TextUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load files from inside Field/
 * <p>
 * For speed, rather than parsing the EDN for our Box files, we simply regex them into the correct spot
 */
public class FileBrowser extends Box implements IO.Loaded {

	static public final Dict.Prop<Boolean> isLinked = new Dict.Prop<Boolean>("isLinked").doc(
		"property is set to true if this box is used in other sheets, and you are editing all of them at the same time")
		.type()
		.toCanon();

	static public final Dict.Prop<BiFunction<String, Vec2, Set<Box>>> copyFromFileCalled = new Dict.Prop<>("copyFromFileCalled").toCanon()
		.type()
		.doc("`_.copyFromFileCalled('banana', new Vec2(0,0))` will copy into this document any file (.field2 file or box or template) called 'banana', centering all the boxes loaded on the point `Vec2(0,0)`");
	static public final Dict.Prop<BiFunction<String, Vec2, Set<Box>>> insertFromFileCalled = new Dict.Prop<>("insertFromFileCalled").toCanon()
		.type()
		.doc("`_.copyFromFileCalled('banana', new Vec2(0,0))` will insert a live reference into this document any file (.field2 file or box or template) called 'banana', centering all the boxes loaded on the point `Vec2(0,0)`");

	static AtomicInteger sheetsInFlight = new AtomicInteger();
	private final Box root;
	LinkedHashMap<String, FieldFile> files = new LinkedHashMap<>();
	LinkedHashMap<String, FieldBox> boxes = new LinkedHashMap<>();

	long allFrameHashSalt = 0;

	public FileBrowser(Box root) {

		this.root = root;

		properties.put(copyFromFileCalled, this::copyFromFileCalled);
		properties.put(insertFromFileCalled, this::insertFromFileCalled);

		properties.put(Commands.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			m.put(new Pair<>("New", "Copies templates, individual boxes, or whole files from the workspace into this document"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					this.p = prompt;
				}

				@Override
				public void run() {

					Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					files.values()
						.stream()
						.filter(f -> f.name != null)
						.forEach(f -> {
							String cm = f.getComment(FileBrowser.this);
							m.put(new Pair<>(quote(f.name) + " <i>&mdash; document</i>",
								(cm == null ? "" : (cm + "<br>")) + ("contains " + f.boxes.size() + " box" + (f.boxes.size() == 1 ? "" : "es")) + "    <br>"), () -> {

								// doit

								Open.doOpen(root, f.name)
									.forEach(IO::uniqify);

							});
						});

					boxes.values()
						.stream()
						.filter(f -> f.name != null)
						.forEach(f -> {

//						     Log.log("insertbox", "box: " + f.name);

							Set<FieldFile> ui = f.usedIn(files);

							List<String> dc = directChildrenOf(f.id);


							m.put(new Pair<>(quote(f.name) + " " + (f.customClass != null ? "<b>custom</b>" : "") + " " + (f.missingPlugin ? "<b>missing plugin?</b>" :
								"") + " " + ((dc != null && dc.size() > 0) ? ("+ " + dc.size() + " child" + (dc.size() == 1 ? "" : "ren")) : ""),
								("used in " + ui.size() + "&nbsp;file" + (ui.size() == 1 ? "" : "s")) + (f.comment == null ? "" : f.comment)), () -> {


								FieldBoxWindow w = first(Boxes.window, both()).get();
								Vec2 p = w.getCurrentMouseState()
									.position()
									.get();


								Vec2 position = first(Drawing.drawing, both()).get()
									.windowSystemToDrawingSystem(p);


								Vec2[] was = {null};
								Box loaded = loadBox(f.filename.getAbsolutePath(), k -> {
									was[0] = k;
									return position;
								});
								List<Box> dcLoaded = (dc == null || dc.size() == 0) ? Collections.emptyList() : dc.stream().map(x -> boxes.get(x)).filter(x -> x != null).map(f2 -> loadBox(f2.filename.getAbsolutePath(), k -> new Vec2(k).sub(was[0]).add(position))).collect(Collectors.toList());

								IO.uniqify(loaded);
								dcLoaded.forEach(IO::uniqify);

								dcLoaded.forEach(x -> loaded.connect(x));


							});
						});

					p.prompt("Search workspace...", m, null);
				}
			});

			m.put(new Pair<>("Insert from Workspace", "Links boxes or whole files from the workspace into this document"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					this.p = prompt;
				}

				@Override
				public void run() {

					Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					files.values()
						.stream()
						.filter(f -> f.name != null)
						.forEach(f -> {

//						     Log.log("insertbox", "file: " + f.name);

							m.put(new Pair<>(quote(f.name), (f.boxes.size() + " box" + (f.boxes.size() == 1 ? "" : "es") + " " + (f.copyOnly ? "<i>(Template)</i>" : ""))),
								() -> {

								// doit

//							     Log.log("insertbox", "would insert file :" + f.name);
								Set<Box> loaded = Open.doOpen(root, f.id);

								if (f.copyOnly) for (Box b : loaded)
									IO.uniqify(b);
								else for (Box b : loaded)
									IO.uniqifyIfNecessary(root, b);

							});
						});

					boxes.values()
						.stream()
						.filter(f -> f.name != null)
						.forEach(f -> {

//						     Log.log("insertbox", "box: " + f.name);

							Set<FieldFile> ui = f.usedIn(files);
							m.put(new Pair<>(quote(f.name) + " " + (f.customClass != null ? "<b>custom</b>" : "") + " " + (f.missingPlugin ? "<b>missing plugin?</b>" : ""),
								("used in " + ui.size() + " file" + (ui.size() == 1 ? "" : "s") + " " + (f.copyOnly ? "<i>(Template)</i>" : ""))), () -> {

								// doit
//								   Log.log("insertbox", "would insert box :" + f.name);


								FieldBoxWindow w = first(Boxes.window, both()).get();
								Vec2 p = w.getCurrentMouseState()
									.position()
									.get();


								Vec2 position = first(Drawing.drawing, both()).get()
									.windowSystemToDrawingSystem(p);


								if (f.copyOnly) {
									IO.uniqify(loadBox(f.filename.getAbsolutePath(), k -> position));
								} else
									IO.uniqifyIfNecessary(root, loadBox(f.filename.getAbsolutePath(), k -> position));


							});
						});

					p.prompt("Search workspace...", m, null);
				}
			});

			long linked = selection().filter(x -> x.properties.isTrue(isLinked, false))
				.count();
			long selected = selection().count();

			String desc = null;
			String plural = null;

			if (linked == selected) {
				if (linked == 1) {
					desc = "This";
					plural = "";
				} else if (linked > 1) {
					desc = "These";
					plural = "es";
				} else {
					desc = null;
				}
			} else {
				if (linked == 1) {
					desc = "One of these";
					plural = "es";
				} else if (linked > 1) {
					desc = "Some of these";
					plural = "es";
				} else {
					desc = null;
				}

			}

			if (desc != null)

				m.put(new Pair<>("Make unique", desc + " box" + plural + " is used in other sheets; select this option to make independent"), () -> {
					selection().filter(x -> x.properties.isTrue(isLinked, false))
						.forEach(x -> {
							IO.uniqify(x);
							x.properties.put(isLinked, false);
							Drawing.dirty(x);
							allFrameHashSalt++;
						});
				});
			return m;
		});

		properties.putToMap(FLineDrawing.frameDrawing, "__linkbadges__", FrameChangedHash.getCached(this, (b, was) -> {

			FLine badges = new FLine();

			String filename = root.properties.get(Open.fieldFilename);

			root.breadthFirst(root.downwards())
				.filter(x -> x.properties.has(Box.frame) && x.properties.has(Box.name) && x.properties.has(IO.id))
				.forEach(x -> {

					FieldBox q = boxes.get(x.properties.get(IO.id));

					Log.log("updatebox", () -> "looked for " + x.properties.get(IO.id) + " in " + boxes.keySet() + " got " + q);

					if (q == null) return;
					Set<FieldFile> uses = q.usedIn(files);

//				    Log.log("updatebox", "box " + x.properties.get(Box.name) + " is used in " + in.size());

					if (uses == null) {
						if (x.properties.isTrue(isLinked, false)) {
							x.properties.put(isLinked, false);
							Drawing.dirty(x);
						}
						return;
					}
					if (uses.size() == 0) {
						if (x.properties.isTrue(isLinked, false)) {
							x.properties.put(isLinked, false);
							Drawing.dirty(x);
						}
						return;
					}
					if (uses.size() == 1) {
						if (uses.iterator()
							.next().name.equals(filename)) {
							if (x.properties.isTrue(isLinked, false)) {
								x.properties.put(isLinked, false);
								Drawing.dirty(x);
							}
							return;
						}
					}
					Rect r = x.properties.get(Box.frame);

					float inset = 10f;

					FLinesAndJavaShapes.drawRoundedRectInto(badges, r.x, r.y, r.w, r.h, 19);

					if (x.properties.isTrue(isLinked, true)) {
						x.properties.put(isLinked, true);
						Drawing.dirty(x);
					}
				});

			badges.attributes.put(StandardFLineDrawing.fillColor, new Vec4(0, 0, 0.2, 1.0f));
			badges.attributes.put(StandardFLineDrawing.filled, true);

			Log.log("updatebox", () -> "badge geometry is " + badges.nodes);

			return badges;

		}, () -> allFrameHashSalt));
	}

	private String quote(String name) {
		String v = TextUtils.quoteNoOuter(name).replaceAll("'", "&apos;");

		System.out.println(" quoted "+name+" -> "+v);

		return v;
	}

	public Set<Box> copyFromFileCalled(String name, Vec2 centeredOn) {
		return copyFromFileCalled(root, name, centeredOn);
	}

	public Set<Box> copyFromFileCalled(Box root, String name, Vec2 centeredOn) {

		Set<Box> m = new LinkedHashSet<>();

		files.values()
			.stream()
			.filter(f -> f.name != null)
			.filter(f -> matchNoSuffix(f.name, name))
			.forEach(f -> {
				Open.doOpen(root, f.id)
					.forEach(x -> {
						IO.uniqify(x);
						m.add(x);
					});
			});

		boxes.values()
			.stream()
			.filter(f -> f.name != null)
			.filter(f -> matchNoSuffix(f.name, name))
			.forEach(f -> {


				FieldBoxWindow w = first(Boxes.window, both()).get();

				Box b = loadBox(f.filename.getAbsolutePath(), k -> centeredOn);
				IO.uniqify(b);
				m.add(b);

			});

		Vec3 c = new Vec3();
		m.stream()
			.map(x -> x.properties.get(Box.frame))
			.filter(x -> x != null)
			.forEach(x -> {
				c.add(new Vec3(x.x + x.w / 2, x.y + x.h / 2, 1));
			});

		c.x /= c.z;
		c.y /= c.z;

		m.stream()
			.map(x -> x.properties.get(Box.frame))
			.filter(x -> x != null)
			.forEach(x -> {
				x.x += -c.x + centeredOn.x;
				x.y += -c.y + centeredOn.y;
			});

		return m;

	}

	private boolean matchNoSuffix(String n1, String n2) {
		if (n1.contains(".")) n1 = n1.split("\\.")[0];
		if (n2.contains(".")) n2 = n2.split("\\.")[0];
		/*if (n1.contains(File.pathSeparator))*/
		n1 = new File(n1).getName();
		/*if (n2.contains(File.pathSeparator))*/
		n2 = new File(n2).getName();

		return n1.toLowerCase()
			.matches(n2.toLowerCase());
	}

	public Set<Box> insertFromFileCalled(String name, Vec2 centeredOn) {

		Set<Box> m = new LinkedHashSet<>();

		files.values()
			.stream()
			.filter(f -> f.name != null)
			.filter(f -> f.name.equals(name))
			.forEach(f -> {

				Open.doOpen(root, f.name)
					.forEach(x -> {
						IO.uniqify(x);
						m.add(x);
					});
			});

		boxes.values()
			.stream()
			.filter(f -> f.name != null)
			.filter(f -> f.name.equals(name))
			.forEach(f -> {


				FieldBoxWindow w = first(Boxes.window, both()).get();
				Vec2 p = w.getCurrentMouseState()
					.position()
					.get();

				Box b = loadBox(f.filename.getAbsolutePath(), k -> centeredOn);
				IO.uniqify(b);
				m.add(b);

			});

		return m;

	}


	static public FieldBox newFieldBox(File from) {
		return newFieldBox(from, false);
	}

	static public FieldBox newFieldBox(File from, boolean retainText) {
		FieldBox f = new FieldBox();

		List<String> all = readCompletely(from);
		if (all == null) return null;

		f.filename = from;
		if (retainText) {
			f.allText = all;
		}

		for (String s : all) {
			if (s.trim()
				.startsWith("\"name\"")) {
				f.name = s.replace("\"name\"", "")
					.trim();
				f.name = f.name.substring(1, f.name.length() - 1);
			}
			if (s.trim()
				.startsWith("\"comment\"")) {
				f.comment = s.replace("\"comment\"", "")
					.trim();
				f.comment = f.comment.substring(1, f.comment.length() - 1);

				// well, this is fun
				f.comment = f.comment.replaceAll("\n", "<br>");
				f.comment = f.comment.replaceAll("\\\\n", "<br>");
				f.comment = f.comment.replaceAll("\\n", "<br>");
				f.comment = f.comment.replaceAll("'", "&rsquo;");
				f.comment = f.comment.replaceAll("\"", "&rdquo;");

				System.out.println("COMMENT IS <" + f.comment + ">");
//				f.comment = "";
			}
			if (s.trim()
				.startsWith("\"__id__\"")) {
				f.id = s.replace("\"__id__\"", "")
					.trim();
				f.id = f.id.substring(1, f.id.length() - 1);
			}
			if (s.trim()
				.startsWith("\"__boxclass__\"")) {
				String c = s.replace("\"__boxclass__\"", "")
					.trim();
				c = c.substring(1, c.length() - 1);
				if (!c.equals("fieldbox.boxes.Box")) {
					f.setCustomClass(c);

					try {
						Class loaded = Thread.currentThread()
							.getContextClassLoader()
							.loadClass(c);
						try {
							if (loaded.getDeclaredField("notForInsert") != null) {
								return null;
							}
						} catch (NoSuchFieldException e) {
						}
					} catch (ClassNotFoundException e) {
						f.missingPlugin = true;
					}

				}

			}
		}

		return f;
	}

	static EDN edn = new EDN();

	static public FieldFile newFieldFile(File from) {
		FieldFile f = new FieldFile();
		List<String> all = readCompletely(from);
		if (all == null) {
			return null;
		}

		IO.Document read = (IO.Document) edn.read(String.join(" ", all));

		read.externalList.forEach(x -> {
			f.boxes.add(x.id);
			f.knownChildren.computeIfAbsent(x.id, k -> new ArrayList<>()).addAll(x.children);
		});

		f.name = from.getName();
		f.id = from.getAbsolutePath();

		for (String s : all) {
			if (s.trim()
				.startsWith(":id")) {
				String z = s.replace(":id", "")
					.trim();
				z = z.substring(1, z.length() - 1);
				final String finalZ = z;
				f.boxes.add(z);
			}
		}

		return f;
	}

	static private List<String> readCompletely(File from) {
		try {
			return Files.readAllLines(from.toPath());
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public void loaded() {
		Log.log("INSERT", () -> "has loaded");
		parse();
	}

	private Box loadBox(String f, Function<Vec2, Vec2> position) {

		Box b = fieldbox.FieldBox.fieldBox.io.loadSingleBox(f, root);

		Rect fr = b.properties.get(Box.frame);

		Vec2 p2 = position.apply(new Vec2(fr.x - fr.w / 2, fr.y - fr.h / 2));

		fr.x = (float) (p2.x - fr.w / 2);
		fr.y = (float) (p2.y - fr.h / 2);

		root.connect(b);
		if (b instanceof IO.Loaded) {
			((IO.Loaded) b).loaded();
		}
		Callbacks.load(b);

		Drawing.dirty(b);

		return b;
	}

	public void parse() {

		Log.log("INSERT", () -> "has loaded");
		String dir = fieldbox.FieldBox.fieldBox.io.getDefaultDirectory();
		parse(dir, false);
		dir = fieldbox.FieldBox.fieldBox.io.getTemplateDirectory();
		parse(dir, true);
	}

	public void parse(String dir, boolean copyOnly) {
		Log.log("INSERT", () -> "parsing directory :" + dir);

		List<String> added = new ArrayList<>();

		try {
			WatchService w = FileSystems.getDefault()
				.newWatchService();
			WatchKey r = new File(dir).toPath()
				.register(w, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
			new Thread(() -> {
				try {
					WatchKey k = w.take();

					RunLoop.main.once(() -> {
						Log.log("INSERT", () -> "reparsing directory <" + dir + ">");
						files.keySet()
							.removeAll(added);
						FileBrowser.this.boxes.keySet()
							.removeAll(added);
						parse(dir, copyOnly);
					});

					w.close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).start();
		} catch (IOException e) {
			e.printStackTrace();
		}


		try {
			List<Path> boxes = Files.walk(new File(dir).toPath())
				.filter(x -> x.toString()
					.endsWith(".box"))
				.collect(Collectors.toList());

			if (boxes != null) {

				Log.log("INSERT", () -> "boxes are :" + Arrays.asList(boxes));

				for (Path from : boxes)
					RunLoop.workerPool.submit(() -> {

						Log.log("INSERT", () -> from);
						FieldBox ff = newFieldBox(from.toFile());
						ff.copyOnly = copyOnly;
						synchronized (files) {
							FieldBox displaced = FileBrowser.this.boxes.put(ff.id, ff);
							added.add(ff.id);
						}
					});
			}

		} catch (IOException e) {
			e.printStackTrace();
		}


		try {

			List<Path> sheets = Files.walk(new File(dir).toPath())
				.filter(x -> x.toString()
					.endsWith(".field") || x.toString()
					.endsWith(".field2"))
				.collect(Collectors.toList());
			if (sheets != null) {


				Log.log("INSERT", () -> "sheets are :" + Arrays.asList(sheets));
				for (Path from : sheets) {
					sheetsInFlight.incrementAndGet();
					RunLoop.workerPool.submit(() -> {
						Log.log("INSERT", () -> from);

						try {
							FieldFile ff = newFieldFile(from.toFile());
							ff.copyOnly = copyOnly;

							synchronized (files) {
								files.put(ff.id, ff);
								added.add(ff.id);
							}
						} finally {
							sheetsInFlight.decrementAndGet();
						}
					});
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

	static public class FieldFile {
		public String name;
		public String id;
		public boolean copyOnly = false;

		public Set<String> boxes = new LinkedHashSet<>();

		public Map<String, List<String>> knownChildren = new LinkedHashMap<>();

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof FieldFile)) return false;

			FieldFile fieldFile = (FieldFile) o;

			return id.equals(fieldFile.id);
		}

		@Override
		public int hashCode() {
			return id == null ? 0 : id.hashCode();
		}

		public String getComment(FileBrowser inside) {
//			return "";

			String cm = "";
			for (String s : boxes) {
				FieldBox bx = inside.boxes.get(s);
				if (bx == null)
					System.out.println(" couldn't find content :" + s + " " + inside.boxes.keySet());
				else
					System.out.println(" found box "+bx.comment);

				if (bx != null && bx.comment != null && bx.comment.length() > 0) {
					System.out.println(" -- "+bx.comment);
					cm += "\n\n" + bx.comment;
				}
			}
			cm = cm.trim();

			return cm.length() == 0 ? null : cm.trim();
		}

	}

	static public class FieldBox {
		public String id;
		public String name;
		public String comment;
		public String principleText;
		public List<String> allText;
		public File filename;

		public String customClass = null;
		boolean copyOnly = false;
		public boolean missingPlugin = false;


		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof FieldBox)) return false;

			FieldBox fieldBox = (FieldBox) o;

			return !(id != null ? !id.equals(fieldBox.id) : fieldBox.id != null);

		}

		public FieldBox setCustomClass(String customClass) {
			this.customClass = customClass;
			return this;
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}

		public Set<FieldFile> usedIn(LinkedHashMap<String, FieldFile> files) {
			if (sheetsInFlight.get() > 0) {
				return Collections.emptySet();
			}

			return files.values()
				.stream()
				.filter(x -> {
					return x.boxes.contains(id);
				})
				.collect(Collectors.toSet());

		}
	}

	public List<String> directChildrenOf(String id) {
		synchronized (files) {
			for (FieldFile ff : files.values()) {
				List<String> o = ff.knownChildren.get(id);
				if (o != null)
					return o;
			}
		}
		return null;
	}


}
