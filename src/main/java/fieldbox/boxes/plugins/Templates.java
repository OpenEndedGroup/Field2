package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.FieldBox;
import fieldbox.boxes.*;
import fieldbox.execution.Completion;
import fieldbox.execution.QuoteCompletionHelpers;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.RemoteEditor;
import fielded.plugins.Launch;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by marc on 3/24/15.
 */
public class Templates extends Box implements IO.Loaded {

	static public final Dict.Prop<Box.BiFunctionOfBoxAnd<String, Box>> templateChild = new Dict.Prop<>("templateChild").toCanon()
		.type()
		.doc("`_.templateChild('template')` create a new box that's a child of this one, copied from 'template'");

	static public final Dict.Prop<Box.TriFunctionOfBoxAnd<String, String, Box>> ensureChildTemplated = new Dict.Prop<>("ensureChildTemplated").toCanon()
		.type()
		.doc("`_.ensureChildTemplated('template', 'a')` create a new box that's a child of this one, copied from `template`, called `a`. If there's already something called `a`, just return that");

	static public final Dict.Prop<Box.BiFunctionOfBoxAnd<String, Box>> saveAsTemplate = new Dict.Prop<>("saveAsTemplate").type()
		.toCanon()
		.doc("`_.saveAsTemplate('name')`. Save this box as a template called `name`");

	private final Box root;
	FileBrowser fileBrowser;

	public Templates(Box root) {

		this.root = root;

		properties.put(saveAsTemplate, (box, name) -> saveAsTemplate(Collections.singleton(box), name));

		properties.put(templateChild, (of, name) -> {

			String path = fieldbox.FieldBox.fieldBox.io.findTemplateCalled(name);

			Set<Box> c = loadBox(path, of.properties.get(Box.frame)
				.convert(0.9, 0.9));

			c.forEach(cc -> IO.uniqifyIfNecessary(root, cc));

			c.forEach(cc -> of.connect(cc));

			return c.iterator().next();

		});

		QuoteCompletionHelpers.wrap(properties, templateChild, Box.BiFunctionOfBoxAnd.class, x -> {

			List<String> matches = fieldbox.FieldBox.fieldBox.io.findTemplateStartingWith(x);

			return matches.stream().map(y -> new Completion(-1, -1, y, "")).collect(Collectors.toList());

		});

		properties.put(ensureChildTemplated, (box, template, name) -> {

			Optional<Box> f = box.children()
				.stream()
				.filter(x -> x.properties.equals(Box.name, name))
				.findFirst();

			return f.orElseGet(() -> {

				String path = fieldbox.FieldBox.fieldBox.io.findTemplateCalled(template);

				Set<Box> c = loadBox(path, box.properties.get(Box.frame)
					.convert(0.9, 0.9));

				if (c.size()==0) return null;

				c.iterator().next().properties.put(Box.name, name);

				c.forEach(cc -> IO.uniqifyIfNecessary(root, cc));

				c.forEach(cc -> box.connect(cc));

				// todo, select a "head" element in a multi-box template
				return c.iterator().next();

			});
		});
		properties.put(Commands.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

			long numSelected = selection().count();

			if (numSelected > 0)
				m.put(new Pair<>("Save as template...", "Makes this " + (numSelected == 1 ? "box" : "selection of " + numSelected + " boxes") + " a reusable, easily imported template"),
					new RemoteEditor.ExtendedCommand() {

						public RemoteEditor.SupportsPrompt p;

						@Override
						public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
							this.p = prompt;
						}

						@Override
						public void run() {
							Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

							p.prompt("name of template...", m, new RemoteEditor.ExtendedCommand() {
								String altWas = null;
								RemoteEditor.SupportsPrompt p;


								@Override
								public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
									altWas = alternativeChosen;
									p = prompt;
								}

								@Override
								public void run() {
									if (altWas != null) {
										saveAsTemplate(selection().collect(Collectors.toSet()), altWas);
									}
								}
							});
						}
					});




			return m;
		});
	}

	@Override
	public void loaded() {
		fileBrowser = (FileBrowser) breadthFirst(both()).filter(x -> x instanceof FileBrowser)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("can't find filebrowser"));

	}

	static public Box saveAsTemplate(Set<Box> b, String filename) {
		if (b.size() == 0) return null;

		Map<Box, String> special = new LinkedHashMap<>();

		Box root = b.iterator()
			.next()
			.find(Boxes.root, b.iterator()
				.next()
				.upwards())
			.findFirst()
			.get();

		String path = IO.TEMPLATES + "/" + filename + "/";

		special.put(root, ">>root<<");

		List<Runnable> undo = new LinkedList<>();

		IO.Document doc = FieldBox.fieldBox.io.compileDocument(path, root, x -> {
			undo.add(IO.uniqify(x));
			return b.contains(x);
		}, special);

		Map<String, String> remap = new LinkedHashMap<String, String>();

		doc.externalList.forEach(x -> {
			String nid = Box.newID();
			remap.put(x.id, nid);
			x.id = nid;
		});

		doc.externalList.forEach(x -> {
			x.children = x.children.stream().map(y -> remap.getOrDefault(y, y)).filter(y -> y != null).collect(Collectors.toList());
			x.parents = x.parents.stream().map(y -> remap.getOrDefault(y, y)).filter(y -> y != null).collect(Collectors.toList());
		});


		boolean error = false;
		try {
			FieldBox.fieldBox.io.writeOutDocument(IO.TEMPLATES + "/" + filename + "/" + filename + (filename.endsWith(".field2") ? "" : ".field2"), doc);
		} catch (IOException e) {
			e.printStackTrace();
			Drawing.notify("Error saving " + e.getMessage(), b.iterator()
				.next(), 200);
			error = true;
		}

		for (Runnable r : undo)
			r.run();

		return b.iterator()
			.next();
	}



	private static String keepPrefix(String filename, String was) {
		String[] parts = was.split("\\.");

		String lastParts = "";
		for (int i = 1; i < parts.length; i++) {
			lastParts = lastParts + "." + parts[i];
		}

		return filename + lastParts;
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

	private Set<Box> loadBox(String f, Vec2 position) {

		if (f==null) return Collections.EMPTY_SET;

		if (f.endsWith(".box")) {

			Box b = fieldbox.FieldBox.fieldBox.io.loadSingleBox(f, root);

			IO.uniqify(b);

			Rect fr = b.properties.get(Box.frame);
			fr.x = (float) (position.x - fr.w / 2);
			fr.y = (float) (position.y - fr.h / 2);

			root.connect(b);
			if (b instanceof IO.Loaded) {
				((IO.Loaded) b).loaded();
			}
			Callbacks.load(b);

			Drawing.dirty(b);

			return Collections.singleton(b);
		}
		else if (f.endsWith(".field2"))
		{
			return find(FileBrowser.copyFromFileCalled, this.both()).findFirst().map(x -> x.apply(f, position)).get();
		}

		throw new IllegalArgumentException("can't find this template");
	}
}
