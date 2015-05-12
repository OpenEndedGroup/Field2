package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.Open;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fielded.Commands;
import fielded.RemoteEditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load files from inside Field/
 * <p>
 * For speed, rather than parsing the EDN for our Box and Field2 files, we simply regex them into the correct spot
 */
public class FileBrowser extends Box implements IO.Loaded {

	static public final Dict.Prop<Boolean> isLinked = new Dict.Prop<Boolean>("isLinked").doc(
		    "property is set to true if this box is used in other sheets, and you are editing all of them at the same time")
											    .type()
											    .toCannon();




	private final Box root;
	LinkedHashMap<String, FieldFile> files = new LinkedHashMap<>();
	LinkedHashMap<String, FieldBox> boxes = new LinkedHashMap<>();
	static AtomicInteger sheetsInFlight = new AtomicInteger();
	long allFrameHashSalt = 0;

	public FileBrowser(Box root) {

		this.root = root;

		Log.log("INSERT", "FileBrowser is alive");

		properties.put(Commands.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			m.put(new Pair<>("Copy from Workspace", "Copies boxes or whole files from the workspace into this document"), new RemoteEditor.ExtendedCommand() {

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

						     m.put(new Pair<>(f.name, (f.boxes.size() + " box" + (f.boxes.size() == 1 ? "" : "es"))), () -> {

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
						     m.put(new Pair<>(f.name+ " " + (f.customClass != null ? "<b>custom</b>" : ""), ("used in " + ui.size() + " file" + (ui.size() == 1 ? "" : "s"))), () -> {

							     // doit
//							     Log.log("insertbox", "would insert box :" + f.name);


							     FieldBoxWindow w = first(Boxes.window, both()).get();
							     Vec2 p = w.getCurrentMouseState()
								       .position()
								       .get();


							     Vec2 position = first(Drawing.drawing, both()).get()
													   .windowSystemToDrawingSystem(p);


							     IO.uniqify(loadBox(f.filename.getAbsolutePath(), position));


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

						     m.put(new Pair<>(f.name, (f.boxes.size() + " box" + (f.boxes.size() == 1 ? "" : "es") + " " + (f.copyOnly ? "<i>(Template)</i>" : ""))), () -> {

							     // doit

//							     Log.log("insertbox", "would insert file :" + f.name);
							     Set<Box> loaded = Open.doOpen(root, f.name);

							     if (f.copyOnly)
								     for (Box b : loaded)
									     IO.uniqify(b);
							     else
							     for (Box b : loaded)
								     IO.uniqifyIfNecessary(root, b);

						     });
					     });

					boxes.values()
					     .stream()
					     .filter(f -> f.name != null)
					     .forEach(f -> {

//						     Log.log("insertbox", "box: " + f.name);

						     Set<FieldFile> ui = f.usedIn(files);
						     m.put(new Pair<>(f.name + " " + (f.customClass != null ? "<b>custom</b>" : ""), ("used in " + ui.size() + " file" + (ui.size() == 1 ? "" : "s")+" "+(f.copyOnly ? "<i>(Template)</i>" : ""))),
							   () -> {

								   // doit
//								   Log.log("insertbox", "would insert box :" + f.name);


								   FieldBoxWindow w = first(Boxes.window, both()).get();
								   Vec2 p = w.getCurrentMouseState()
									     .position()
									     .get();


								   Vec2 position = first(Drawing.drawing, both()).get()
														 .windowSystemToDrawingSystem(p);


								   if (f.copyOnly) {
									   IO.uniqify(loadBox(f.filename.getAbsolutePath(), position));
								   }
								   else
									   IO.uniqifyIfNecessary(root, loadBox(f.filename.getAbsolutePath(), position));


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

				    Log.log("updatebox", "looked for " + x.properties.get(IO.id) + " in " + boxes.keySet() + " got " + q);

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

			Log.log("updatebox", "badge geometry is " + badges.nodes);

			return badges;

		}, () -> allFrameHashSalt));
	}

	@Override
	public void loaded() {
		Log.log("INSERT", "has loaded");
		parse();
	}

	private Box loadBox(String f, Vec2 position) {

		Box b = fieldbox.FieldBox.fieldBox.io.loadSingleBox(f, root);

		Rect fr = b.properties.get(Box.frame);
		fr.x = (float) (position.x - fr.w / 2);
		fr.y = (float) (position.y - fr.h / 2);

		root.connect(b);
		if (b instanceof IO.Loaded) {
			((IO.Loaded) b).loaded();
		}
		Callbacks.load(b);

		Drawing.dirty(b);

		return b;
	}

	public void parse() {

		Log.log("INSERT", "has loaded");
		String dir = fieldbox.FieldBox.fieldBox.io.getDefaultDirectory();
		parse(dir, false);
		dir = fieldbox.FieldBox.fieldBox.io.getTemplateDirectory();
		parse(dir, true);
	}
	public void parse(String dir, boolean copyOnly)
	{
		Log.log("INSERT", "parsing directory :"+dir);

		File[] boxes = new File(dir).listFiles(x -> x.getName()
							     .endsWith(".box"));
		if (boxes != null) {

			Log.log("INSERT", "boxes are :"+Arrays.asList(boxes));

			for (File from : boxes)
				RunLoop.workerPool.submit(() -> {

					Log.log("INSERT", from);
					FieldBox ff = newFieldBox(from);
					ff.copyOnly = copyOnly;
					synchronized (files) {
						FieldBox displaced = FileBrowser.this.boxes.put(ff.id, ff);
					}
				});
		}

		File[] sheets = new File(dir).listFiles(x -> x.getName()
							      .endsWith(".field2") || x.getName()
										       .equals(".field"));

		if (sheets != null) {
			Log.log("INSERT", "sheets are :"+Arrays.asList(sheets));
			for (File from : sheets) {
				sheetsInFlight.incrementAndGet();
				RunLoop.workerPool.submit(() -> {
					Log.log("INSERT", from);

					try {
						FieldFile ff = newFieldFile(from);
						ff.copyOnly = copyOnly;

						synchronized (files) {
							files.put(ff.id, ff);
						}
					} finally {
					}
				});
			}

		}
	}

	static public FieldBox newFieldBox(File from) {
		return newFieldBox(from, false);
	}

	static public FieldBox newFieldBox(File from, boolean retainText) {
		FieldBox f = new FieldBox();

		List<String> all = readCompletely(from);
		if (all == null) return null;

		f.filename = from;
		if (retainText)
		{
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

					}

				}

			}
		}

		return f;
	}

	static public FieldFile newFieldFile(File from) {
		FieldFile f = new FieldFile();

		List<String> all = readCompletely(from);
		if (all == null) {
			return null;
		}

		f.name = from.getName();
		f.id = from.getAbsolutePath();

		for (String s : all) {
			if (s.trim()
			     .startsWith(":id")) {
				String z = s.replace(":id", "")
					    .trim();
				z = z.substring(1, z.length() - 1);
				Log.log("inserbox.readFieldFile", "got id " + z);
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

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

	static public class FieldFile {
		public String name;
		public String id;
		public boolean copyOnly = false;

		public Set<String> boxes = new LinkedHashSet<>();

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof FieldFile)) return false;

			FieldFile fieldFile = (FieldFile) o;

			if (!id.equals(fieldFile.id)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return id == null ? 0 : id.hashCode();
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


		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof FieldBox)) return false;

			FieldBox fieldBox = (FieldBox) o;

			if (id != null ? !id.equals(fieldBox.id) : fieldBox.id != null) return false;

			return true;
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
//			Log.log("insertbox", "used in :" + sheetsInFlight.get() + " / " + id);
			if (sheetsInFlight.get() > 0) return Collections.emptySet();

			return files.values()
				    .stream()
				    .filter(x -> {
					    System.out.println(" looking for :" + id + " in " + x.boxes);
					    return x.boxes.contains(id);
				    })
				    .collect(Collectors.toSet());

		}
	}


}
