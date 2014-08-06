package fieldnashorn;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.parser.ParseException;
import field.graphics.RunLoop;
import field.utility.Log;
import field.utility.Pair;
import fielded.Execution;
import fieldnashorn.annotations.HiddenInAutocomplete;
import fieldnashorn.annotations.JavaDocOnly;
import fieldnashorn.annotations.StopAutocompleteHere;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * When Nashorn/Javascript completion bumps up against something that's actually a Java object, we can use Java reflection based completion. Better
 * yet, if we have the source code fore that Java we can get very good completion information with parameter names, generics and javadocs. We're using
 * qdox to parse the Java sources.
 * <p>
 * This class also contains our class discovery mechanism for import help
 * <p>
 * Todo: this is useful accross language runtimes, not just Nashorn/Javascript.
 */
public class JavaSupport {

	static public JavaSupport javaSupport = null;

	public interface HandlesCompletion {
		public List<Execution.Completion> getCompletionsFor(String prefix);
	}

	private final JavaProjectBuilder builder;

	Map<String, String> allClassNames = new LinkedHashMap<>();

	public JavaSupport() {
		javaSupport = this;

		builder = new JavaProjectBuilder();

		List<Path> all = new ArrayList<>();

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		RunLoop.workerPool.submit(() -> {
			try {
				URL[] paths = ((URLClassLoader) classLoader).getURLs();
				for (URL path : paths) {
					RunLoop.workerPool.submit(() -> {
						Map<String, String> a = indexClasses(path);
						synchronized (allClassNames) {
							allClassNames.putAll(a);
						}
					});
				}

				builder.setErrorHandler(e -> Log
					    .log("completion.general", " problem parsing Java source file for completion, will skip this file and continue on "));
				builder.addClassLoader(classLoader);

				String root = fieldagent.Main.app;
				Files.walkFileTree(FileSystems.getDefault().getPath(root), new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						if (dir.endsWith("temp")) return FileVisitResult.SKIP_SUBTREE;
						if (dir.endsWith("src")) {
							Log.log("completion.debug", " added " + dir + " to source path");
							all.add(dir);
							try {
								builder.addSourceTree(dir.toFile(), f -> Log
									    .log("completion.error", " error parsing file " + f + ", but we'll continue on anyway..."));
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});
				Log.log("completion.debug", " all is :" + all);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

	}

	private Map<String, String> indexClasses(URL path) {
		String f = path.getFile();
		if (f.endsWith(".jar")) return indexClasses_jar(f);
		else if (new File(f).exists()) return indexClasses_tree(f);
		return Collections.emptyMap();
	}

	// TODO: not implemented yet
	private Map<String, String> indexClasses_tree(String f) {
		return Collections.emptyMap();

	}

	private Map<String, String> indexClasses_jar(String f) {
		try {
			return new JarFile(f).stream().filter(e -> e.getName().endsWith(".class"))
				    .map(e -> e.getName().replace('/', '.').replace(".class", "")).collect(Collectors.toMap(x -> x, x -> f));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyMap();
	}

	public List<Execution.Completion> getCompletionsFor(Object o, String prefix) {
		if (o instanceof HandlesCompletion) {

			Log.log("completion.debug", " object :" + o + " is a completion handler ");

			return ((HandlesCompletion) o).getCompletionsFor(prefix);
		} else Log.log("completion.debug", " object :" + o + " is not a completion handler ");

		boolean staticsOnly = o instanceof Class;

		Class c = o instanceof Class ? (Class) o : o.getClass();

		boolean wasJava = c.getName().startsWith("java");

		JavaClass j = builder.getClassByName(c.getName());

		Log.log("completion.debug", " java class (for javadoc supported completion) :" + j + " prefix is <" + prefix + ">");

		List<Execution.Completion> r = new ArrayList<>();
		try {
			while (j != null) {

				boolean docOnly = c.getAnnotation(JavaDocOnly.class) != null;
				if (c.getAnnotation(StopAutocompleteHere.class) != null) break;

				if (c.getAnnotation(HiddenInAutocomplete.class) == null) {

					for (JavaField m : j.getFields()) {
						if (hasAnnotation(m.getAnnotations(), HiddenInAutocomplete.class)) continue;
						if (docOnly && m.getComment().trim().length() < 1) continue;
						if ((prefix.equals("") || m.getName().startsWith(prefix)) && m.getModifiers()
							    .contains("public") && (!staticsOnly || m.getModifiers().contains("static"))) {
							r.add(new Execution.Completion(-1, -1, m.getName(), "<span class=type>" + compress(m
								    .getName(), m.getDeclarationSignature(true)) + "</span>" + (m
								    .getComment() != null ? "<span class=type>&nbsp;&mdash;</span> <span class=doc>" + m
								    .getComment() + "</span>" : "")));
						}
					}
					for (JavaMethod m : j.getMethods()) {
						if (hasAnnotation(m.getAnnotations(), HiddenInAutocomplete.class)) continue;
						if (docOnly && m.getComment().trim().length() < 1) continue;
						if ((prefix.equals("") || m.getName().startsWith(prefix)) && m.getModifiers()
							    .contains("public") && (!staticsOnly || m.getModifiers().contains("static"))) {
							r.add(new Execution.Completion(-1, -1, m.getName(), "<span class=type>" + compress(m
								    .getName(), m.getDeclarationSignature(true)) + "</span>" + (m
								    .getComment() != null ? "<span class=type>&nbsp;&mdash;</span> <span class=doc>" + m
								    .getComment() + "</span>" : "")));
						}
					}
				}
				c = c.getSuperclass();
				if (c == null) break;
				j = builder.getClassByName(c.getName());

				//TODO: should we stop when we get into java.* classes if we started from something that wasnt?
				// PApplet's completion is heavily polluted by this stuff.
				// let's try that now.

				boolean isJava = c.getName().startsWith("java") || c.getName().startsWith("jdk");
				if (!wasJava && isJava) break;

			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return r;
	}

	private boolean hasAnnotation(List<JavaAnnotation> annotations, Class hiddenInAutocompleteClass) {
		return annotations.stream().filter(x -> x.getType().getName().equals(hiddenInAutocompleteClass.getName().substring(1+hiddenInAutocompleteClass.getName().lastIndexOf('.')))).findFirst().isPresent();
	}

	static public String compress(String name, String signature) {
		signature = " " + signature;

		Pattern p = Pattern.compile("([A-Za-z]*?)\\.([A-Za-z]*?)");
		Matcher m = p.matcher(signature);

		while (m.find()) {
			signature = m.replaceAll("$2");
			m = p.matcher(signature);
		}

		signature = signature.replace(" public ", " ");
		signature = signature.replace(" final ", " ");
		signature = signature.replace(" void ", " ");

		p = Pattern.compile(" " + name + "[ \\(]");
		m = p.matcher(signature);
		if (m.find()) {
			signature = m.replaceAll("&larr;(");
		}

		signature = signature.replace("  ", " ");
		signature = signature.replace("  ", " ");

		return signature.trim();
	}

	public List<Pair<String, String>> getPossibleJavaClassesFor(String left) {
		List<Pair<String, String>> rr = new ArrayList<>();

		Set<String> seen = new LinkedHashSet<>();

		for (JavaClass c : builder.getClasses()) {
			if (c.getName().contains(left) && !seen.contains(c.getFullyQualifiedName())) {
				seen.add(c.getFullyQualifiedName());
				rr.add(new Pair<>(c.getFullyQualifiedName(), c.getComment()));
			}
		}

		synchronized (allClassNames) {
			for (Map.Entry<String, String> e : allClassNames.entrySet()) {
				if (e.getKey().contains(left) && !seen.contains(e.getKey())) {
					seen.add(e.getKey());
					rr.add(new Pair<>(e.getKey(), "from " + e.getValue()));
				}
			}
		}

		if (rr.size() > 100) rr = rr.subList(0, 100);

		return rr;
	}


}
