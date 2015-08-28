package field.utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class SimpleCommand {

	static public Triple<String, String, Integer> go(File dir, String command, String... args) throws IOException, InterruptedException {
		File t1 = File.createTempFile("field", ".out.txt");
		t1.deleteOnExit();
		File t2 = File.createTempFile("field", ".out.txt");
		t2.deleteOnExit();

		ArrayList<String> all = new ArrayList<>(args.length + 1);
		all.add(command);
		for(String s : args)
			all.add(s);

		ProcessBuilder p = new ProcessBuilder(all);
		p.directory(dir);
		p.redirectOutput(ProcessBuilder.Redirect.to(t1));
		p.redirectError(ProcessBuilder.Redirect.to(t2));

		System.out.println(" starting process :"+p+" "+ command+" "+String.join(" ", args));

		int ret = p.start()
			   .waitFor();

		return new Triple<>(Files.lines(t1.toPath())
					 .reduce("", (a, b) -> a + "\n" + b), Files.lines(t2.toPath())
										   .reduce("", (a, b) -> a + "\n" + b), ret);
	}
}
