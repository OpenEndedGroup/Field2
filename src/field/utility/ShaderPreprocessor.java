package field.utility;

import fieldbox.boxes.Box;
import fieldbox.boxes.plugins.Exec;

import java.util.List;
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

			System.out.println(" about to eval :"+g);

			Triple<Object, List<String>, List<Pair<Integer, String>>> got = e.apply(inside, g);

			System.out.println(" got :"+got);
			return ""+got.first;
		});

	}

}
