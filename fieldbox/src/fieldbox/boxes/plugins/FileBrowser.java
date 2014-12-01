package fieldbox.boxes.plugins;

import field.graphics.RunLoop;
import field.linalg.Vec2;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.FieldBox;
import fieldbox.Open;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Drawing;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fielded.RemoteEditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Load files from inside Field/
 * <p>
 * For speed, rather than parsing the EDN for our Box and Field2 files, we simply regex them into the correct spot
 */
public class FileBrowser extends Box implements IO.Loaded {


	private final Box root;

	public class FieldFile {
		String name;
		String id;

		Set<String> boxes = new LinkedHashSet<>();

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
			return id.hashCode();
		}

	}

	public class FieldBox {
		String id;
		String name;
		String comment;
		String principleText;
		File filename;


		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof FieldBox)) return false;

			FieldBox fieldBox = (FieldBox) o;

			if (id != null ? !id.equals(fieldBox.id) : fieldBox.id != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}

		public Set<FieldFile> usedIn() {
			Log.log("insertbox", "used in :"+sheetsInFlight.get()+" / "+id);
			if (sheetsInFlight.get() > 0) return null;

			return files.values()
				    .stream()
				    .filter(x -> x.boxes.contains(id))
				    .collect(Collectors.toSet());

		}
	}

	LinkedHashMap<String, FieldFile> files = new LinkedHashMap<>();
	LinkedHashMap<String, FieldBox> boxes = new LinkedHashMap<>();

	AtomicInteger sheetsInFlight = new AtomicInteger();

	@Override
	public void loaded() {
		parse();
	}

	public FileBrowser(Box root) {

		this.root = root;

		properties.put(RemoteEditor.commands, () -> {

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
					     .stream().filter(f -> f.name!=null)
					     .forEach(f -> {

						     Log.log("insertbox", "file: " + f.name);

						     m.put(new Pair<>(f.name, (f.boxes.size() + " box" + (f.boxes.size() == 1 ? "" : "es"))), () -> {

							     // doit

							     Log.log("insertbox", "would insert file :" + f.name);
							     Open.doOpen(root, f.name).forEach(IO::uniqify);

						     });
					     });

					boxes.values()
					     .stream().filter(f -> f.name != null)
					     .forEach(f -> {

						     Log.log("insertbox", "box: "+f.name);

						     Set<FieldFile> ui = f.usedIn();
						     m.put(new Pair<>(f.name, ("used in " + ui.size() + " file" + (ui.size() == 1 ? "" : "s"))), () -> {

							     // doit
							     Log.log("insertbox", "would insert box :"+f.name);


							     FieldBoxWindow w = first(Boxes.window, both()).get();
							     Vec2 p = w.getCurrentMouseState()
											.position().get();


							     Vec2 position = first(Drawing.drawing, both()).get()
												       .windowSystemToDrawingSystem(p);


							     IO.uniqify(loadBox(f.filename.getAbsolutePath(), position));


						     });
					     });

					p.prompt("Search workspace...", m, null);
				}
			});
			return m;
		});
	}


	private Box loadBox(String f, Vec2 position) {

		Box b = fieldbox.FieldBox.fieldBox.io.loadSingleBox(f);

		Rect fr = b.properties.get(Box.frame);
		fr.x = (float) (position.x-fr.w/2);
		fr.y = (float) (position.y-fr.h/2);

		root.connect(b);
		Drawing.dirty(b);

		return b;
	}

	public void parse() {
		Log.log("insertbox", "parsing");
		String dir = fieldbox.FieldBox.fieldBox.io.getDefaultDirectory();

		File[] boxes = new File(dir).listFiles(x -> x.getName()
							     .endsWith(".box"));
		if (boxes != null) {

			for (File from : boxes)
				RunLoop.workerPool.submit(() -> {

					FieldBox ff = newFieldBox(from);

					synchronized (files) {
						Log.log("insertbox", "parsed "+from+" got "+ff.name);
						FileBrowser.this.boxes.put(ff.id, ff);
					}
				});
		}

		File[] sheets = new File(dir).listFiles(x -> x.getName()
							      .endsWith(".field2"));

		if (sheets != null) {
			for (File from : sheets) {
				sheetsInFlight.incrementAndGet();
				RunLoop.workerPool.submit(() -> {

					try {
						FieldFile ff = newFieldFile(from);

						synchronized (files) {
							Log.log("insertbox", "parsed "+from+" got "+ff.name);
							files.put(ff.id, ff);
						}
					} finally {
						Log.log("insertbox", "sif" + sheetsInFlight.decrementAndGet());
					}
				});
			}

		}
	}


	private FieldBox newFieldBox(File from) {
		FieldBox f = new FieldBox();

		List<String> all = readCompletely(from);
		if (all == null) return null;

		f.filename = from;

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
		}

		return f;
	}

	private FieldFile newFieldFile(File from) {
		FieldFile f = new FieldFile();

		List<String> all = readCompletely(from);
		if (all == null) return null;

		f.name = from.getName();

		for (String s : all) {
			if (s.trim()
			     .startsWith("\":id\"")) {
				String z = s.replace("\":id\"", "")
					    .trim();
				z = z.substring(1, z.length());
				f.boxes.add(z);
			}
		}

		return f;
	}

	private List<String> readCompletely(File from) {
		try {
			return Files.readAllLines(from.toPath());
		} catch (IOException e) {
			return null;
		}
	}


}
