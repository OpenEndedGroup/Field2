package field.utility;

import fieldbox.boxes.Box;
import fieldbox.boxes.plugins.Exec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marc on 7/27/16.
 */
public class ShaderPreprocessor {

	Pattern p = Pattern.compile("(\\$\\{(.*)\\})");

	public String preprocess(Box inside, String s) {
		Box.BiFunctionOfBoxAnd<String, Triple<Object, List<String>, List<Pair<Integer, String>>>> e = inside.find(Exec.exec, inside.upwardsOrDownwards()).findFirst().get();

		System.out.println(" preprocess :"+s);

		Matcher q = p.matcher(s);
		return q.replaceAll(x -> {
			String g = q.group(2);
			Object z = e.apply(inside, g).first;
			if (z == null || ("" + z).toLowerCase().equals("undefined")) {
				return "";
			} else return "" + z;
		});
	}


	public class Preprocess implements Supplier<String> {
		private final String source;
		private String evaluatedTo;
		private final Box inside;

		public Preprocess(Box inside, String source) {
			this.inside = inside;
			this.source = source;
		}

		public String getSource() {
			return source;
		}

		@Override
		public String get() {
			if (evaluatedTo == null)
				return evaluatedTo = preprocess(inside, source);
			else return evaluatedTo;
		}
	}


}