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

		System.out.println(" parsed disabled ranges, got :"+r);

		return r;
	}

	static public String rewriteWithDisabledRanges(String code, String commentStartCharacter, String commentEndCharacter, List<Pair<Integer, Integer>> disabled)
	{
		String[] lines = code.split("\n");
		if (lines.length==0) return "";

		ArrayList<String> m = new ArrayList<>(Arrays.asList(lines));

		List<Pair<Integer, Boolean>> kinds = new ArrayList<>();
		disabled.forEach(x -> {
			kinds.add(new Pair<>(x.first, true));
			kinds.add(new Pair<>(x.second, false));
		});

		Collections.sort(kinds, (a, b) -> Integer.compare(a.first, b.first));

		for(int i=kinds.size()-1;i>=0;i--)
		{
			m.add(kinds.get(i).first + (kinds.get(i).second ? 0 : 1), kinds.get(i).second ? commentStartCharacter : commentEndCharacter);
		}

		String q = m.stream()
			    .reduce((a, b) -> a + "\n" + b)
			    .get();


		System.out.println(" inserted "+disabled+" into\n"+code+"\n got \n"+q);
		return q;
	}

	static public String getStringWithDisabledRanges(Box from, Dict.Prop<String> stringProp, String start, String end)
	{
		String code = from.properties.get(stringProp);
		List<Pair<Integer, Integer>> dis = from.properties.get(new Dict.Prop<List<Pair<Integer, Integer>>>(stringProp.getName() + "_disabled"));
		if (dis==null) return code;

		return rewriteWithDisabledRanges(code, start, end, dis);
	}

}
