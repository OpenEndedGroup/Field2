package fieldbox;

import field.utility.Options;
import fieldbox.boxes.plugins.FileBrowser;
import fieldbox.io.IO;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by marc on 5/12/15.
 */
public class Copy implements Runnable {

	public final IO io = new IO(Options.getDirectory("workspace", () -> System.getProperty("user.home") + "/Documents/FirstNewFieldWorkspace/"));

	@Override
	public void run() {

		String from = Options.getString("file", () -> null);
		if (from == null) throw new IllegalArgumentException("-file option can't be null");

		String to = Options.getString("destination", () -> null);
		if (from == null) throw new IllegalArgumentException("-destination option can't be null");


		// the file to copy
		FileBrowser.FieldFile file = FileBrowser.newFieldFile(io.filenameFor(IO.WORKSPACE+from));

		// should be recursive!
		File[] files = new File(io.getDefaultDirectory()).listFiles(x -> x.getName()
										  .endsWith(".box"));

		List<FileBrowser.FieldBox> implicated = Arrays.asList(files)
							      .stream()
							      .map(x -> FileBrowser.newFieldBox(x, true))
							      .filter(x -> x != null)
							      .filter(x -> file.boxes.contains(x.id))
							      .collect(Collectors.toList());

		List<File> sub = implicated.stream()
					   .flatMap(x -> x.allText.stream()
								  .filter(y -> y.contains(IO.WORKSPACE)))
					   .map(x -> {
						   String[] pieces = x.trim().split(" ");
						   String last = "";
						   for(int i=1;i<pieces.length;i++)
						   {
							   last = last+" "+pieces[i];
						   }
						   last = last.trim();
						   last = last.substring(1, last.length() - 1);
						   return io.filenameFor(last);
					   })
					   .collect(Collectors.toList());

		LinkedHashSet<File> all = new LinkedHashSet<File>(sub);
		all.addAll(implicated.stream()
				     .map(x -> x.filename)
				     .collect(Collectors.toList()));
		all.add(io.filenameFor(IO.WORKSPACE+from));

		all.forEach(x -> {
			if (!x.exists()) throw new IllegalArgumentException(" box references a file that doesn't exist :"+x);
		});

		if (new File(to).exists())
			throw new IllegalArgumentException(" won't copy into an existing directory");

		new File(to).mkdirs();

		all.forEach(x -> {
			try {
				Path a = x.toPath();
				Path b = new File(new File(to), x.getName()).toPath();

				Files.copy(a, b);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});


	}
}
