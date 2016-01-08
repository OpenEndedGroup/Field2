package field.utility;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.function.Consumer;

public class SimpleCommand {

	static public Triple<String, String, Integer> go(File dir, String command, String... args) throws IOException, InterruptedException {
		File t1 = File.createTempFile("field", ".out.txt");
		t1.deleteOnExit();
		File t2 = File.createTempFile("field", ".out.txt");
		t2.deleteOnExit();

		ArrayList<String> all = new ArrayList<>(args.length + 1);
		all.add(command);
		for (String s : args)
			all.add(s);

		ProcessBuilder p = new ProcessBuilder(all);
		p.directory(dir);
		p.redirectOutput(ProcessBuilder.Redirect.to(t1));
		p.redirectError(ProcessBuilder.Redirect.to(t2));

		System.out.println(" starting process :" + p + " " + command + " " + String.join(" ", args));

		int ret = p.start()
			   .waitFor();

		return new Triple<>(Files.lines(t1.toPath())
					 .reduce("", (a, b) -> a + "\n" + b), Files.lines(t2.toPath())
										   .reduce("", (a, b) -> a + "\n" + b), ret);
	}

	static public Integer go(File dir, Consumer<String> output, String command, String... args) throws IOException, InterruptedException {

		ArrayList<String> all = new ArrayList<>(args.length + 1);
		all.add(command);
		for (String s : args)
			all.add(s);

		ProcessBuilder p = new ProcessBuilder(all);
		p.directory(dir);

		System.out.println(" starting process :" + p + " " + command + " " + String.join(" ", args));

		Process proc = p.start();
		InputStream is = proc.getInputStream();
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		InputStream ise = proc.getErrorStream();
		BufferedReader re = new BufferedReader(new InputStreamReader(ise));

		new Thread(() -> {
			try {
				while ((r.ready() || re.ready())) {
					if (r.ready())
						output.accept(r.readLine());
					if (re.ready())
						output.accept(re.readLine());
					Thread.sleep(10);


					if (!r.ready() && !re.ready() && !proc.isAlive())
						break;

				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
		int ret = proc.waitFor();

		return ret;
	}


}
