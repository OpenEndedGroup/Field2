package field.utility;

import fieldbox.boxes.Box;
import fieldbox.boxes.plugins.Exec;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marc on 7/27/16.
 */
public class ShaderPreprocessor {

	Pattern p = Pattern.compile("(\\$\\{(.*?)\\})");

	public String preprocess(Box inside, String s) {
		return preprocess(inside, s, Collections.emptyMap(), Collections.emptyMap());
	}

	public String preprocess(Box inside, String s, Map<String, Function<String, String>> extra) {
		return preprocess(inside, s, extra, Collections.emptyMap());
	}

	public String preprocess(Box inside, String s, Map<String, Function<String, String>> extra, Map<String, String> extraExtra) {
		Box.BiFunctionOfBoxAnd<String, Triple<Object, List<String>, List<Pair<Integer, String>>>> e = inside == null ? null : inside.find(Exec.exec, inside.upwardsOrDownwards()).findFirst()
			.get();

		Matcher q = p.matcher(s);
		return q.replaceAll(x -> {
			String g = q.group(2);
			Object z = null;
			if (extra.containsKey(g)) {
				z = extra.get(g).apply(g);
			} else if (extraExtra.containsKey(g)) {
				z = extraExtra.get(g);
			} else if (inside!=null)
				z = e.apply(inside, g).first;

			if (z == null || ("" + z).toLowerCase().equals("undefined")) {
				return "";
			} else return "" + z;
		});
	}


	public class Preprocess implements Supplier<String> {
		private final String source;
		private String evaluatedTo;
		private final Box inside;
		private Supplier<Map<String, String>> extra = () -> Collections.emptyMap();
		private Map<String, String> was = null;

		public Preprocess(Box inside, String source) {
			this.inside = inside;
			this.source = source;
		}

		public Preprocess(Box inside, String source, Supplier<Map<String, String>> m) {
			this.inside = inside;
			this.source = source;
			this.extra = m;
		}

		public String getSource() {
			return source;
		}

		@Override
		public String get() {
			if (was == null || !was.equals(extra.get())) {
				was = extra.get();
				return evaluatedTo = preprocess(inside, source, Collections.emptyMap(), was);
			} else return evaluatedTo;
		}
	}


}