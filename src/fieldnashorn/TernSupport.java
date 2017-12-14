package fieldnashorn;

import field.utility.Log;
import field.utility.MarkdownToHTML;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.execution.Completion;
import fieldbox.execution.HandlesQuoteCompletion;
import fieldbox.execution.JavaSupport;
import jdk.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tern.js-based completion for Nashorn/Javascript that isn't afraid to drop down into Java/Reflection based completion when it can (see JavaSupport)
 */
public class TernSupport {

	static public JavaSupport javaSupport;
	private ScriptEngine engine;


	public TernSupport() {
		if (javaSupport == null) javaSupport = new JavaSupport();
	}

	public void inject(ScriptEngine engine) {

		this.engine = engine;
		List<String> s = Arrays.asList("acorn.js", "acorn_loose.js", "walk.js", "signal.js", "tern.js", "def.js", "comment.js", "infer.js" /*"condense.js"*/);
//		List<String> s = Arrays.asList("all-tern.js");

		Collection<File> f = s.stream()
//			.map(x -> new File(fieldagent.Main.app + "/modules/fieldcore/resources/tern/" + x))
			.map(x -> new File(fieldagent.Main.app + "/lib/web/tern/" + x))
			.collect(Collectors.toList());

		Log.log("bindings", () -> "global scope bindings :" + engine.getContext()
			.getBindings(ScriptContext.GLOBAL_SCOPE)
			.keySet());
		Log.log("bindings", () -> "engine scope bindings :" + engine.getContext()
			.getBindings(ScriptContext.ENGINE_SCOPE)
			.keySet());

		try {
			engine.eval("__fieldglobal.self = {}");
			engine.eval("__fieldglobal.self.tern = {}");
			engine.eval("__fieldglobal.self.acorn= {}");
			engine.eval("__fieldglobal.self.infer= {}");
			engine.eval("__fieldglobal.tern = {}");
			engine.eval("self = __fieldglobal.self");
			engine.eval("tern = __fieldglobal.tern");
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		new Thread(() -> {

			for (File ff : f) {
				try (FileReader fr = new FileReader(ff.getAbsolutePath())) {
					engine.put("__FILE__", ff.getName());
					engine.eval(fr);
				} catch (ScriptException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
//				engine.put("__ecma5json", readFile(fieldagent.Main.app + "/modules/fieldcore/resources/tern/ecmascript.json"));
				engine.put("__ecma5json", readFile(fieldagent.Main.app + "/lib/web/tern/ecmascript.json"));
				engine.eval("self.ternServer=new self.tern.Server({defs: [JSON.parse(__ecma5json)]})");
				engine.eval("delete __ecma5json");
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public List<Completion> completion(ScriptEngine engine, String boxName, String allText, int line, int ch, boolean explicitlyRequested) {

		List<Completion> r = new ArrayList<>();


		try {
			engine.put("__someFile", allText);
			engine.eval("var __completions = new java.util.ArrayList()");
			try {
				engine.eval("__fieldglobal.self.ternServer.request({query:{type:\"completions\", types:true, docs:true, file:\"#0\", end:{line:" + line + ",ch:" + ch + "}}, \n" +
					"files:[{type:\"full\",name:\"" + boxName + ".js\",text:__someFile}]},\n" +
					"	function (e,r){\n" +
					"		for(var i=0;i<r.completions.length;i++)" +
					"			if (r && r.end)" +
					"			__completions.add(new __fieldglobal.fieldbox.execution.Completion(r.start, r.end, r.completions[i].name, '<span class=type>'+r.completions[i].type+'&nbsp;&mdash;&nbsp;</span><span class=doc>'+(r.completions[i].doc==null ? '' : r.completions[i].doc)+'</span>'))" +
					"	})");
				r.addAll((ArrayList<Completion>) engine.get("__completions"));
			} catch (ScriptException e) {
				e.printStackTrace();
			}

			Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
			Log.log("completion.debug", () -> {
				Log.log("completion.debug", () -> "bindings are...");
				for (Object k : bindings.keySet()) {
					Object t = bindings.get(k);
					if (t == null) continue;
					if (t instanceof String) if (((String) t).split("\n").length > 0)
						t = ((String) t).split("\n")[0] + " ...";
					final Object finalT = t;
					Log.log("completion.debug", () -> "    " + k + " " + finalT);
				}
				return null;
			});

			if (allText.trim()
				.length() == 0) return r;

			String[] lines = allText.split("\n");
			int c = 0;
			for (int i = 0; i < line; i++) {
				c += lines[i].length() + 1;
			}

			c += ch;


			final int finalC = c;
			Log.log("completion.debug", () -> " line :" + line + " -> " + ch + " -> " + finalC + " alltext is <" + allText + ">");

			try {
				int[] ret = expressionRangeForPosition(engine, c);

				if (ret == null) return r;
				if (ret.length == 0) return r;

				Log.log("completion.debug", () -> " expression to analyze is :" + ret[0] + " " + ret[1] + " " + allText.substring(ret[0], ret[1]));


				String s = allText.substring(ret[0], ret[1]);


				if (s.trim().startsWith("\"")) {
					// we have quote completion, do that instead

					String quoteSoFar = s.trim()
						.substring(1);

					boolean customCompleted = false;


					try {
						int[] previously = expressionRangeForPosition(engine, ret[0] - 1);

						Log.log("completion.debug", () -> "previous expression is :" + previously[0] + " " + previously[1] + " " + allText.substring(previously[0], previously[1]));

						String previousS = allText.substring(previously[0], previously[1]);


						if (explicitlyRequested) {
							Object e = engine.eval("var _e=eval('" + previousS.replace("'", "\\'") + "')");
							Log.log("completion.debug", () -> "PREVIOUS e is :" + e + " " + e.getClass() + " computed prefix from <" + s + "> <" + s.lastIndexOf('.') + ">");


							if (e instanceof HandlesQuoteCompletion) {
								r.clear();
								List<Completion> completions = ((HandlesQuoteCompletion) e).getQuoteCompletionsFor(quoteSoFar);
								for (Completion x : completions) {
									if (x.start == -1)
										x.start = c - quoteSoFar.length();
									if (x.end == -1) x.end = c;
								}
								r.addAll(completions);
								Collections.sort(r, (a, b) -> {
									if (a.rank != b.rank)
										return Double.compare(a.rank, b.rank);
									if (a.replacewith.length() != b.replacewith.length())
										return Double.compare(a.replacewith.length(), b.replacewith.length());
									return String.CASE_INSENSITIVE_ORDER.compare(a.replacewith, b.replacewith);
								});
								customCompleted = true;
							}
						}
					} catch (Throwable t) {
						Log.log("completion.error", () -> "quote completion threw an exception, but we'll continue on anyway");
						t.printStackTrace();
					}

					if (!customCompleted) {
						List<Completion> completions = getQuoteCompletionsForFileSystems(quoteSoFar);
						for (Completion x : completions) {
							if (x.start == -1) x.start = c - quoteSoFar.length();
							if (x.end == -1) x.end = c;
						}

						r.addAll(completions);
						Collections.sort(r, (a, b) -> {
							if (a.rank != b.rank) return Double.compare(a.rank, b.rank);
							if (a.replacewith.length() != b.replacewith.length())
								return Double.compare(a.replacewith.length(), b.replacewith.length());
							return String.CASE_INSENSITIVE_ORDER.compare(a.replacewith, b.replacewith);
						});
					}
					return r;
				}


				String left = s, right = "";

				if (s.lastIndexOf('.') != -1) {
					left = s.substring(0, s.lastIndexOf('.'));
					right = s.substring(s.lastIndexOf('.') + 1);
				} else {
					Object directlyBound = engine.get(left);
					if (directlyBound != null && !directlyBound.getClass().getName().toLowerCase().endsWith("staticclass")) {
						Completion direct = new Completion(-1, -1, left + " = " + noSourceForFunctions(directlyBound), "<span class=type>" + suppressScriptObjectMirror(directlyBound.getClass().getName()) + "</span><span class=doc>" + MarkdownToHTML.unwrapFirstParagraph(MarkdownToHTML.convert(docOutOfFunction(directlyBound, "value from this box"))) + "</span>");
						direct.rank = -1000;
						r.add(direct);
					}
				}
//				if (right.trim().length()==0 && !s.trim().endsWith("."))
//				{
//					// don't execute something just because it's a complete expression
//				}
//				else
				if (explicitlyRequested || left.indexOf("(") == -1) {

					Object e = engine.eval("var _e=eval('" + left.replace("'", "\\'") + "'); _e");
					final Object finalE = e;
					Log.log("completion.debug", () -> " e is :" + finalE + " " + finalE.getClass() + " computed prefix from <" + s + "> <" + s.lastIndexOf('.') + ">");

					if (right.trim()
						.length() != right.length()) right = "";

					// down-weight Tern in favor of Java if right has a prefix
					if (s.lastIndexOf('.') != -1)
						r.forEach(x -> x.rank += 1);

					// now if e is an actual java object --- i.e. it's got nothing to do with nashorn, then we could use a more general Field java completion service
					// and just add the dot back in
					if (e instanceof ScriptObjectMirror) {
						Object[] retae = (Object[]) engine.eval("var _v=[]; var _p = {}; Object.bindProperties(_p, _e); for(var _k in _p) _v.push(_k); Java.to(_v)");
						Log.log("completion.debug", () -> " auto eval completion got :" + Arrays.asList(retae));
					} else if (e instanceof Box) {
//					e = new UnderscoreBox((Box) e);
						List<Completion> fromJava = javaSupport.getCompletionsFor(e, right);
						for (Completion x : fromJava) {
							if (x.start == -1) x.start = c - right.length();
							if (x.end == -1) x.end = c;
						}

						r.addAll(fromJava);
					} else if (e instanceof StaticClass) {
						e = ((StaticClass) e).getRepresentedClass();

						final Object finalE1 = e;
						Log.log("completion.debug", () -> " asking java for completions for CLASS " + finalE1);
						List<Completion> fromJava = javaSupport.getCompletionsFor(e, right, s.lastIndexOf('.') == -1);
						Log.log("completion.debug", () -> " got completions :" + fromJava);
						for (Completion x : fromJava) {
							if (x.start == -1) x.start = c - right.length();
							if (x.end == -1) x.end = c;

							if (x.replacewith.toLowerCase().equals(left.toLowerCase())) {
								x.start -= left.length();
							}
						}

						r.addAll(fromJava);
					} else {
						final Object finalE2 = e;
						Log.log("completion.debug", () -> " asking java for completions for " + finalE2);
						List<Completion> fromJava = javaSupport.getCompletionsFor(e, right);
						Log.log("completion.debug", () -> " got completions :" + fromJava);

						for (Completion x : fromJava) {
							if (x.start == -1) x.start = c - right.length();
							if (x.end == -1) x.end = c;
						}

						r.addAll(fromJava);
					}
				}

			} catch (Throwable t) {
				Log.log("completion.error", () -> " suppressed exception in autoevaluating completion <" + t + ">");
				t.printStackTrace();
			}
		} catch (ScriptException e) {
			Log.log("completion.errors", () -> "Completion throw an exception " + e.getMessage());
		}
		Collections.sort(r, (a, b) -> {
			return String.CASE_INSENSITIVE_ORDER.compare(a.replacewith, b.replacewith);
		});

		/*
		for (int i = 1; i < r.size(); i++) {
			Completion a = r.get(i - 1);
			Completion b = r.get(i);
			if (a.replacewith.equals(b.replacewith) && a.type==b.type) {
				if (a.rank > b.rank) {
					r.remove(i );
					i--;
				} else if (a.rank < b.rank) {
					r.remove(i -1);
					i--;
				} else if (a.info.length() > b.info.length()) {
					r.remove(i);
					i--;
				} else {
					r.remove(i - 1);
					i--;
				}
			}
		}
*/
		Collections.sort(r, (a, b) -> {
			if (a.rank != b.rank) return -Double.compare(a.rank, b.rank);
			if (a.replacewith.length() != b.replacewith.length())
				return Double.compare(a.replacewith.length(), b.replacewith.length());
			return String.CASE_INSENSITIVE_ORDER.compare(a.replacewith, b.replacewith);
		});

		return r;
	}

	private String suppressScriptObjectMirror(String name) {
		if (name.toLowerCase().endsWith("ScriptObjectMirror".toLowerCase())) return "";
		return name;
	}

	private String docOutOfFunction(Object directlyBound, String defau) {
		if (directlyBound instanceof ScriptObjectMirror) {
			Object doc = ((ScriptObjectMirror) directlyBound).get("__doc__");
			if (doc != null)
				return "" + doc;
		}
		return defau;
	}

	private String noSourceForFunctions(Object directlyBound) {

		if (directlyBound instanceof ScriptObjectMirror && ((ScriptObjectMirror) directlyBound).isFunction())
			return "[function]";
		return "" + directlyBound;
	}

	private List<Completion> getQuoteCompletionsForFileSystems(String name) {
		File f;
		File[] ff;
		if (name.equals("")) name = ".";

		if (new File(name).exists()) {
			f = new File(name);
			ff = f.listFiles(x -> true);
		} else {
			f = new File(name).getParentFile();
			String finalName = name;
			ff = f.listFiles(x -> x.getName()
				.toLowerCase()
				.startsWith(new File(finalName).getName()
					.toString()
					.toLowerCase()));
		}

		if (ff == null) return Collections.emptyList();

		List<Completion> ret = new ArrayList<>();

		for (File fff : ff) {
			if (fff.isDirectory()) {
				String[] q = fff.list();
				int n = q == null ? 0 : q.length;
				Completion c = new Completion(-1, -1, fff.getAbsolutePath() + "/", "&nbsp;" + n + " file" + (n == 1 ? "" : "s") + "<span class=doc><i>completion from filesystem</i></span>");
				c.rank = -111;
				ret.add(c);
			} else {
				long length = fff.length();
				long time = fff.lastModified();
				Completion c = new Completion(-1, -1, fff.getAbsolutePath(),
					"&nbsp;" + formatSize(length) + " " + formatDate(time) + " ago <span class=doc><i>completion from filesystem</i></span>");
				c.rank = -110;
				ret.add(c);
			}
		}

		return ret;
	}

	private String formatSize(long length) {
		return convertToStringRepresentation(length);
	}

	private String formatDate(long time) {

//		Duration du = Duration.between(date.toInstant().atZone(ZoneId.systemDefault()), LocalTime.now());
		Duration du = Duration.between(Instant.ofEpochMilli(time), ZonedDateTime.now());
		long y = du.toDays() / 365;
		if (y > 0) {
			return y + " year" + (y == 1 ? "" : "s");
		} else {
			y = du.toDays() / 30;
			if (y > 0) {
				return y + " month" + (y == 1 ? "" : "s");
			} else {
				y = du.toDays() / 7;
				if (y > 0) {
					return y + " week" + (y == 1 ? "" : "s");
				} else {
					y = du.toDays();
					if (y > 0) {
						return y + " day" + (y == 1 ? "" : "s");
					} else {

						y = du.toHours();
						if (y > 0) {
							return y + " hour" + (y == 1 ? "" : "s");
						} else {


							y = du.toMinutes();
							if (y > 0) {
								return y + " minute" + (y == 1 ? "" : "s");
							} else {

								y = du.getSeconds();
								if (y > 0) {
									return y + " year" + (y == 1 ? "" : "s");
								} else {
									return Instant.ofEpochMilli(time) + "";
								}
							}
						}
					}
				}
			}
		}

	}


	private static final long K = 1024;
	private static final long M = K * K;
	private static final long G = M * K;
	private static final long T = G * K;

	public static String convertToStringRepresentation(final long value) {
		final long[] dividers = new long[]{T, G, M, K, 1};
		final String[] units = new String[]{"TB", "GB", "MB", "KB", "B"};
		if (value < 0) throw new IllegalArgumentException("Invalid file size: " + value);
		if (value == 0) return "(empty)";
		String result = null;
		for (int i = 0; i < dividers.length; i++) {
			final long divider = dividers[i];
			if (value >= divider) {
				result = format(value, divider, units[i]);
				break;
			}
		}
		return result;
	}

	private static String format(final long value, final long divider, final String unit) {
		final double result = divider > 1 ? (double) value / (double) divider : (double) value;
		return new DecimalFormat("#,##0.#").format(result) + " " + unit;
	}

	private int[] expressionRangeForPosition(ScriptEngine engine, int c) throws ScriptException {
		engine.eval("var __c=new __fieldglobal.self.tern.Context()");
		try {
			Object[] o = (Object[]) engine.eval("__fieldglobal.self.tern.withContext(__c, function(){\n" +
				"\tvar a = __fieldglobal.self.tern.parse(__someFile)\n" +
				"\t__fieldglobal.self.tern.analyze(a)\n" +
				"\tvar n = __fieldglobal.self.tern.findExpressionAround(a, " + c + ", " + c + ")\n" +
				"\tif (n && n.node)\n" +
				"\treturn Java.to([n.node.start, n.node.end]);\n" + "\treturn null;}) ");

			if (o == null) return null;
			if (o.length == 0) return null;

			return new int[]{((Number) o[0]).intValue(), ((Number) o[1]).intValue()};
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<Completion> imports(ScriptEngine engine, String boxName, String allText, int line, int ch) {
		List<Completion> r = new ArrayList<>();

		try {
			engine.put("__someFile", allText);
			engine.eval("var __completions = new java.util.ArrayList()");


			String[] lines = allText.split("\n");
			int c = 0;
			for (int i = 0; i < line; i++) {
				c += lines[i].length() + 1;
			}

			c += ch;


			final int finalC = c;
			Log.log("completion.debug", () -> " line :" + line + " -> " + ch + " -> " + finalC);

			engine.eval("var __c=new __fieldglobal.self.tern.Context()");
			Object[] ret = (Object[]) engine.eval("__fieldglobal.self.tern.withContext(__c, function(){\n" +
				"\tvar a = __fieldglobal.self.tern.parse(__someFile)\n" +
				"\t__fieldglobal.self.tern.analyze(a)\n" +
				"\tvar n = __fieldglobal.self.tern.findExpressionAround(a, " + c + ", " + c + ")\n" +
				"\treturn Java.to([n.node.start, n.node.end])" +
				"})");
			Log.log("completion.debug", () -> " expression to analyze is :" + ret[0] + " " + ret[1] + " " + allText.substring(((Number) ret[0]).intValue(), ((Number) ret[1]).intValue()));


			String s = allText.substring(((Number) ret[0]).intValue(), ((Number) ret[1]).intValue());

			try {

				String left = s, right = "";


				Log.log("completion.debug", () -> " inside import help left is <" + left + ">");

				List<Pair<String, String>> possibleJavaClassesFor = javaSupport.getPossibleJavaClassesFor(left);

				Log.log("completion.debug", () -> " possible javaclasses :");
				possibleJavaClassesFor.forEach(System.out::println);

				for (Pair<String, String> p : possibleJavaClassesFor) {
					int tail = Math.max(p.first.lastIndexOf("."), p.first.lastIndexOf("$"));

					Completion ex = new Completion(c - left.length(), c, p.first.substring(tail + 1), p.second);
					String typeName = p.first.replaceAll("\\$", ".");

					if (typeName.endsWith("."))
						typeName = typeName.substring(0, typeName.length() - 1);

					ex.header = "var " + p.first.substring(tail + 1) + " = Java.type('" + typeName + "')";
					r.add(ex);
				}

			} catch (Throwable t) {
				Log.log("completion.debug", () -> " suppressed exception in auto-evaluating completion <" + t + ">");
			}
		} catch (ScriptException e) {
			e.printStackTrace();
		}


		return r;
	}

	private static String readFile(String s) {
		try (BufferedReader r = new BufferedReader(new FileReader(new File(s)))) {
			String line = "";
			while (r.ready()) {
				line += r.readLine() + "\n";
			}
			return line;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}


}
