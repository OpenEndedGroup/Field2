package fieldbox.boxes.plugins;

import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.Mouse;
import fielded.RemoteEditor;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;
import java.nio.file.Path;

/**
 * Adds: a command to rename a box
 *
 * TODO: ideally we'd have both a prompt and some placeholder text
 * TODO: specifying chained "parameterized" commands such as this ought to be more straightforward.
 */
public class TestPlugin extends Box {

	public TestPlugin(Box root_unused) {
		properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			Runnable put = m.put(new Pair<>("Test File Write", "Testing writing to a properties file"), new Runnable() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void run() {
					final static Charset ENCODING = StandardCharsets.UTF_8;

					Path properties = FileSystems.getDefault().getPath("fieldbox/resources", "properties.txt");
					try (
						    OutputStream out = Files.newOutputStream(properties);
						    PrintStream printStream = new PrintStream(out)
					) {
						printStream.print("Hello World!\n");
						printStream.close();
					} catch (IOException x) {
						System.err.println("CANNOT OPEN FILE!!!!");
					}

					//Send message to text editor socket to change keyboard shortcut
					find(RemoteEditor.editor, both())
						    .forEach(editor -> editor.sendJavaScript("extraKeys[\"Ctrl-/\"] = function (cm) {\n" +
									    "    goCommands();\n" +
									    "};"));

					try (
						Scanner scanner = new Scanner(properties, ENCODING.name())
					) {
						System.out.println(String.valueof)
					} catch(IOException x) {
						System.err.println("CANNOT OPEN FILE!!!!");
					}
				}
			});


			return m;
	});
}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}


}