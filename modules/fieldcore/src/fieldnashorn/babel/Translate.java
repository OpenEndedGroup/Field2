package fieldnashorn.babel;

import field.utility.Pair;
import fieldnashorn.sourcemap.SourceMapConsumerV3;
import fieldnashorn.sourcemap.SourceMapParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Tools for caching
 */
public class Translate implements SourceTransformer {

	static public final String command = "babel --stage 0 # --out-file #.5.js --source-maps";

	LinkedHashMap<String, String> codeCache = new LinkedHashMap<String, String>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return this.size() > 200;
		}
	};

	LinkedHashMap<String, Function<Integer, Integer>> mapCache = new LinkedHashMap<String, Function<Integer, Integer>>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return this.size() > 200;
		}
	};

	boolean first = false;

	public Translate()
	{
		first = true;
	}

	static public  String preamble;

	{
		try{
			preamble = Files.readAllLines(new File("/Users/marc/fieldwork2/out/production/fieldnashorn/polyfill.js").toPath())
					.stream()
					.reduce("", (a, b) -> a + "\n" + b)+"\n\n";

			preamble += Files.readAllLines(new File("/Users/marc/fieldwork2/out/production/fieldnashorn/regenerator.js").toPath())
					.stream()
					.reduce("", (a, b) -> a + "\n" + b)+"\n\n";

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	@Override
	public Pair<String, Function<Integer, Integer>> transform(String c) throws TranslationFailedException {
		String t = codeCache.get(c);
		Function<Integer, Integer> f = mapCache.get(c);
		if (t == null || f == null) {
			Pair<String, Function<Integer, Integer>> q = _transform(c);
			first = false;
			return q;
		}
		return new Pair<>(t, f);
	}

	private Pair<String, Function<Integer, Integer>> _transform(String c) throws TranslationFailedException {

		try {
			File f = File.createTempFile("field", ".js");
			f.deleteOnExit();
			File error = File.createTempFile("field", ".error");
			error.deleteOnExit();

			FileWriter fw = new FileWriter(f);
			fw.append(c);
			fw.close();


			int success = new ProcessBuilder().command(command.replace("#", f.getAbsolutePath())
									  .split(" "))
							  .redirectError(error)
							  .start()
							  .waitFor();

			if (success == 0) {
				String code = new String(Files.readAllBytes(new File(f.getAbsolutePath() + ".5.js").toPath()));
				String mapping = new String(Files.readAllBytes(new File(f.getAbsolutePath() + ".5.js.map").toPath()));


				SourceMapConsumerV3 sm = new SourceMapConsumerV3();
				sm.parse(mapping);

				codeCache.put(c, code);

				Function<Integer, Integer> fn = x -> x;
				try {
					fn = x -> sm.getMappingForLine(x, 1)
									       .getLineNumber();
					mapCache.put(c, fn);
				} catch (NullPointerException e)
				{
					e.printStackTrace();
				}

				return new Pair<>((first ? preamble : "")+code, fn);
			} else {
				String code = new String(Files.readAllBytes(error.toPath()));
				throw new TranslationFailedException(code);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SourceMapParseException e) {
			e.printStackTrace();
		}
		return null;
	}

}
