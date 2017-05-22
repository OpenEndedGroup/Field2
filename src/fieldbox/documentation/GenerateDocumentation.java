package fieldbox.documentation;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaSource;
import field.utility.Dict;
import field.utility.Options;
import field.utility.Pair;
import fieldbox.Run;
import fielded.boxbrowser.BoxBrowser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by marc on 3/4/17.
 */
public class GenerateDocumentation implements Runnable {
	@Override
	public void run() {

		String in = Options.dict().get(new Dict.Prop<String>("in"));
		String out = Options.dict().get(new Dict.Prop<String>("out"));

		if (new File(in).isDirectory() && new File(out).isDirectory()) {
			for (File f : new File(in).listFiles()) {
				try {
					process(f, out + "/" + f.getName() + ".html"); // incorrect, doesn't keep directory names, files with the same name overwrite
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if (new File(in).isDirectory() && !new File(out).isDirectory()) {
			throw new IllegalArgumentException("can't convert directory into a single file <" + out + ">");
		} else {
			try {
				process(new File(in), out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void process(File input, String out) throws IOException {
		System.out.println(":: " + input + " -> " + out);

		String c = Files.readAllLines(input.toPath(), Charset.defaultCharset()).stream().reduce((a, b) -> a + "\n" + b).orElse("");

		if (input.getName().endsWith(".java")) {
			JavaProjectBuilder bu = new JavaProjectBuilder();
			JavaSource source = bu.addSource(input);

			List<Pair<String, String>> ret = Documentation.renderStaticJava(source);
			String m = BoxBrowser.preamble.replace("/field/filesystem/", "/")+ret.stream()
				.map(x -> "<div class='grouped'><h1 id='section_" + x.first + "'>" + x.first + "</h1><div id='section_" + x.first + "''>" + x.second + "</div></div>")
				.reduce((a, b) -> a + "<BR>" + b).orElse("")+BoxBrowser.postamble;

			Files.write(new File(out).toPath(), m.getBytes());

		} else {
			String ret = Documentation.renderStatic(c);

			Files.write(new File(out).toPath(), ret.getBytes());
		}
	}
}
