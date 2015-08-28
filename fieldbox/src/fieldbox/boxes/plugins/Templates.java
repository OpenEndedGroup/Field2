package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.FieldBox;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.RemoteEditor;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by marc on 3/24/15.
 */
public class Templates extends Box {

	static public final Dict.Prop<Box.BiFunctionOfBoxAnd<String, Box>> templateChild = new Dict.Prop<Box.BiFunctionOfBoxAnd<Class, Box>>("templateChild").toCannon()
																			     .type()
																			     .doc("_.templateChild(\"template\") create a new box that's a child of this one, copied from 'template'");

	static public final Dict.Prop<Box.TriFunctionOfBoxAnd<String, String, Box>> ensureChildTemplated = new Dict.Prop<Box.BiFunctionOfBoxAnd<Class, Box>>("ensureChildTemplated").toCannon()
																						    .type()
																						    .doc("_.ensureChildTemplated(\"template\", \"a\") create a new box that's a child of this one, copied from 'template', called 'a'. If there's already something called 'a', just return that");

	static public final Dict.Prop<Box.BiFunctionOfBoxAnd<String, Box>> saveAsTemplate = new Dict.Prop<>("saveAsTemplate").type()
															     .toCannon()
															     .doc("_.saveAsTemplate('name'). Save this box as a template called 'name'");

	private final Box root;

	public Templates(Box root) {

		this.root = root;

		properties.put(saveAsTemplate, (box, name) -> saveAsTemplate(Collections.singleton(box), name));

		properties.put(templateChild, (of, name) -> {

			String path = fieldbox.FieldBox.fieldBox.io.findTemplateCalled(name);

			System.err.println(" about to load :" + path);

			Box c = loadBox(path, of.properties.get(Box.frame)
							   .convert(0.9, 0.9));

			System.out.println(" loaded box :" + c + " of class " + c.getClass());

			IO.uniqifyIfNecessary(root, c);

			of.connect(c);

			return c;

		});
		properties.put(ensureChildTemplated, (box, template, name) -> {


			Optional<Box> f = box.children()
					     .stream()
					     .filter(x -> x.properties.equals(Box.name, name))
					     .findFirst();

			return f.orElseGet(() -> {

				String path = fieldbox.FieldBox.fieldBox.io.findTemplateCalled(template);

				System.err.println(" about to load :" + path);

				Box c = loadBox(path, box.properties.get(Box.frame)
								    .convert(0.9, 0.9));

				System.out.println(" loaded box :" + c + " of class " + c.getClass());

				c.properties.put(Box.name, name);

				IO.uniqifyIfNecessary(root, c);

				box.connect(c);

				return c;

			});
		});
		properties.put(Commands.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

			long numSelected = selection().count();

			if (numSelected > 0)
				m.put(new Pair<>("Save as template", "Makes this " + (numSelected == 1 ? "box" : "selection of " + numSelected + " boxes") + " a reusable, easily imported template"),
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
							      Consumer<String> feedback;

							      @Override
							      public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
								      altWas = alternativeChosen;
								      this.feedback = feedback;
							      }

							      @Override
							      public void run() {

								      if (altWas != null)

									      saveAsTemplate(selection().collect(Collectors.toSet()), altWas);

								      if (feedback != null) feedback.accept("Renamed to \"" + altWas + "\"");

							      }
						      });
					      }
				      });
			return m;
		});
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

		String path = IO.TEMPLATES+"/"+filename+"/";

		special.put(root, ">>root<<");

		IO.Document doc = FieldBox.fieldBox.io.compileDocument(path, root, x -> b.contains(x), special);

		doc.externalList.forEach(x -> {
			x.dataFile =path+ filename + ".box";
			x.textFiles.entrySet()
				   .forEach(z -> z.setValue(keepPrefix(filename + ".box", z.getValue()
											   .replace(IO.WORKSPACE, path))));
			x.id = Box.newID();
		});

		boolean error = false;
		try {
			FieldBox.fieldBox.io.writeOutDocument(FieldBox.fieldBox.io.TEMPLATES + "/" + filename + "/"+filename+(filename.endsWith(".field2") ? "" : ".field2"), doc);
		} catch (IOException e) {
			e.printStackTrace();
			Drawing.notify("Error saving " + e.getMessage(), b.iterator()
									  .next(), 200);
			error = true;
		}

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
}
