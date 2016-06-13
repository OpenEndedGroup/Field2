package fieldbox.execution;

import field.utility.AutoPersist;
import field.utility.Triple;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keeps stats on what completions tend to get completed
 */
public class CompletionStats {

	public static CompletionStats stats = new CompletionStats();

	protected CompletionStats() {
	}

	LinkedHashMap<String, Double> counts = AutoPersist.persist("completion_statistics", () -> new LinkedHashMap<>(), x -> {
		x.entrySet().forEach(y -> {
			y.setValue(y.getValue() * 0.5f);
		});
		return x;
	});

	LinkedHashMap<String, String> uuidToCompletions = new LinkedHashMap<String, String>() {

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> entry) {
			return this.size() > 500;
		}
	};

	public void autosuggestCommands(List<Triple<String, String, Runnable>> m) {
		m.forEach(x -> {
			uuidToCompletions.put(x.first, x.first);
		});

		m.sort((a, b) -> {
			double cA = counts.getOrDefault(a.first, 0d);
			double cB = counts.getOrDefault(b.first, 0d);
			if (cA == cB) {
				return String.CASE_INSENSITIVE_ORDER.compare(a.first, b.first);
			} else {
				return -Double.compare(cA, cB);
			}
		});

		double[] n = {0};
		int[] num = {0};
		m.forEach(x -> {
			Double cc = counts.get(x.first);
			if (cc != null) {
				num[0]++;
				n[0] += cc;
			}
		});

		if (num[0] == 0) {

		} else {
			List<Triple<String, String, Runnable>> out = m.stream().map(x -> {
				Double cc = counts.get(x.first);
				if (cc != null) {
					double score = Math.max(1, Math.min(3, 3 * cc / (n[0] / num[0])));
					String second = x.second + "</span><span class=\"stars\">" + num(score, "★") + " ";

					Triple<String, String, Runnable> t = new Triple<>(x.first, second, x.third);

					return t;
				}
				return x;
			}).collect(Collectors.toList());
			m.clear();
			m.addAll(out);
		}



	}

	private String num(double score, String s) {
		String qq = "";
		while (qq.length() < score)
			qq += s;
		return qq;
	}

	public void autosuggest(List<Completion> m) {

		m.forEach(x -> {
			uuidToCompletions.put(x.uuid, x.replacewith);
		});

		m.sort((a, b) -> {
			double cA = counts.getOrDefault(a.replacewith, 0d);
			double cB = counts.getOrDefault(b.replacewith, 0d);
			if (cA == cB) {
				if (a.rank != b.rank) return Double.compare(a.rank, b.rank);
				if (a.replacewith.length() != b.replacewith.length())
					return -Double.compare(a.replacewith.length(), b.replacewith.length());
				return String.CASE_INSENSITIVE_ORDER.compare(a.replacewith, b.replacewith);
			} else {
				return -Double.compare(cA, cB);
			}
		});

		double[] n = {0};
		int[] num = {0};
		m.forEach(x -> {
			Double cc = counts.get(x.replacewith);
			if (cc != null) {
				num[0]++;
				n[0] += cc;
			}
		});

		if (num[0] == 0) {

		} else {
			m.stream().forEach(x -> {
				Double cc = counts.get(x.replacewith);
				if (cc != null) {
					double score = Math.max(1, Math.min(3, 3 * cc / (n[0] / num[0])));
					String second = x.info + "</span><span class=\"stars\">" + num(score, "★") + " ";

					x.info = second;
				}
			});

		}


	}

	public void notify(String uuid) {

		String replaceWith = uuidToCompletions.get(uuid);

		counts.put(replaceWith, counts.computeIfAbsent(replaceWith, k -> 0d) + 1);

	}

}
