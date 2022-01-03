package fielded;

import field.utility.Dict;
import field.utility.Pair;
import fieldbox.boxes.Box;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 */
public class DisabledRangeHelper {

	static public List<Pair<Integer, Integer>> parseDisabledRanges(String fromEditor) {
		if (fromEditor.endsWith(", ")) fromEditor = fromEditor.substring(0, fromEditor.length() - 2);
		JSONArray a = new JSONArray(fromEditor);
		List<Pair<Integer, Integer>> r = new ArrayList<>();
		for (int i = 0; i < a.length(); i++) {
			JSONArray a1 = a.getJSONArray(i);

			r.add(new Pair<>(a1.getInt(0), a1.getInt(1)));
		}

		return r;
	}

	static public String rewriteWithDisabledRanges(String code, String commentStartCharacter, String commentEndCharacter, List<Pair<Integer, Integer>> disabled) {
		return rewriteWithDisabledRanges(code, commentStartCharacter, commentEndCharacter, disabled, 0);
	}

	static public String rewriteWithDisabledRanges(String code, String commentStartCharacter, String commentEndCharacter, List<Pair<Integer, Integer>> disabled, int lineoffset) {
		String[] lines = code.split("\n");
		if (lines.length == 0) return "";

		ArrayList<String> m = new ArrayList<>(Arrays.asList(lines));

		List<Pair<Integer, Boolean>> kinds = new ArrayList<>();
		disabled.forEach(x -> {
			// not >=0, if we select exactly this block, we want to be able to execute it
			if (x.first - lineoffset > 0 && x.first - lineoffset < lines.length && x.second - lineoffset >=0 && x.second - lineoffset < lines.length) {
				kinds.add(new Pair<>(x.first - lineoffset, true));
				kinds.add(new Pair<>(x.second - lineoffset, false));
			}
		});

		Collections.sort(kinds, (a, b) -> Integer.compare(a.first, b.first));

		for (int i = kinds.size() - 1; i >= 0; i--) {
			m.add(kinds.get(i).first + (kinds.get(i).second ? 0 : 1), kinds.get(i).second ? commentStartCharacter : commentEndCharacter);
		}

		String q = m.stream()
			.reduce((a, b) -> {
				if (a.endsWith(commentStartCharacter)) return a + "" + b;
				if (b.startsWith(commentEndCharacter)) return a + "" + b;
				return a + "\n" + b;
			})
			.get();


		return q;
	}

	static public String getStringWithDisabledRanges(Box from, Dict.Prop<String> stringProp, String start, String end) {
		String code = from.properties.get(stringProp);
		List<Pair<Integer, Integer>> dis = from.properties.get(new Dict.Prop<List<Pair<Integer, Integer>>>(stringProp.getName() + "_disabled"));
		if (dis == null) return code;

		return rewriteWithDisabledRanges(code, start, end, dis);
	}

}
