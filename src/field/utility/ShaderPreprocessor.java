package field.utility;

import fieldbox.boxes.Box;
import fieldbox.boxes.plugins.Exec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marc on 7/27/16.
 */
public class ShaderPreprocessor {

	Pattern p = Pattern.compile("(\\$\\{(.*)\\})");

	public String preprocess(Box inside, String s) {
		Box.BiFunctionOfBoxAnd<String, Triple<Object, List<String>, List<Pair<Integer, String>>>> e = inside.find(Exec.exec, inside.upwards()).findFirst().get();
		Matcher q = p.matcher(s);
		return q.replaceAll(x -> {
			String g = q.group(2);
			return ""+e.apply(inside, g).first;
		});

	}

}
