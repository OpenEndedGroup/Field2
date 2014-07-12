package fieldbox.boxes.plugins;

import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fielded.RemoteEditor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.io.PrintStream;
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
			m.put(new Pair<>("Test File Write", "Testing writing to a properties file"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
					Path properties = FileSystems.getDefault().getPath("fieldbox/resources", "properties.txt");

					try (
						    OutputStream out = Files.newOutputStream(properties);
						    PrintStream printStream = new PrintStream(out)
					) {
						printStream.print("Hello World!\n");
						printStream.close();
					} catch(IOException x){
						System.err.println("CANNOT OPEN FILE!!!!");
					}
				}
				/*
				//This is what we will do to read the file
				try(InputStream in = Files.newInputStream(properties);
				BufferedReader reader =  new BufferedReader(new InputStreamReader(in))){
					String line=null;
					while ((line=reader.readline()) != null) {
						//we are reading
					}
				}
				catch (IOException x){
					System.err.println("CANNOT OPEN FILE!!!!");
				}
				*/

			@Override
			public void run() {
				if(false){
					System.out.println("Don't Run Me");
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