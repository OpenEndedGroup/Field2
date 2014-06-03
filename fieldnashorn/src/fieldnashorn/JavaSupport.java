package fieldnashorn;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.parser.ParseException;
import field.graphics.RunLoop;
import field.utility.Pair;
import fielded.Execution;

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
 *
 * This class also contains our class discovery mechanism for import help
 * <p>
 * Todo: this is useful accross language runtimes, not just Nashorn/Javascript.
 */
public class JavaSupport {

	public interface HandlesCompletion {
		public List<Execution.Completion> getCompletionsFor(String prefix);
	}

	private final JavaProjectBuilder builder;

	Map<String, String> allClassNames = new LinkedHashMap<>();

	public JavaSupport() {
		builder = new JavaProjectBuilder();

		List<Path> all = new ArrayList<>();

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		if (classLoader instanceof URLClassLoader) {
			URL[] paths = ((URLClassLoader) classLoader).getURLs();
			for (URL path : paths) {
				RunLoop.workerPool.submit(() -> {
					Map<String, String> a = indexClasses(path);
					synchronized (allClassNames) {
						allClassNames.putAll(a);
					}
				});
			}

		}

		RunLoop.workerPool.submit(() -> {
			try {
				String root = "/home/marc/fieldwork2/";
				Files.walkFileTree(FileSystems.getDefault().getPath(root), new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						if (dir.endsWith("temp")) return FileVisitResult.SKIP_SUBTREE;
						if (dir.endsWith("src")) {
							System.out.println(" added " + dir + " to source path");
							all.add(dir);
							try {
								builder.addSourceTree(dir.toFile());
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		System.out.println(" all is :" + all);

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
			return ((HandlesCompletion) o).getCompletionsFor(prefix);
		}

		boolean staticsOnly = o instanceof Class;

		Class c = o instanceof Class ? (Class) o : o.getClass();

		JavaClass j = builder.getClassByName(c.getName());

		System.out.println(" java class (for javadoc supported completion) :"+j);

		List<Execution.Completion> r = new ArrayList<>();
		for (JavaField m : j.getFields()) {
			if (m.getName().startsWith(prefix) && m.getModifiers().contains("public") && (!staticsOnly || m.getModifiers().contains("static"))) {
				r.add(new Execution.Completion(-1, -1, m.getName(), "<span class=type>" + compress(m.getName(), m
					    .getDeclarationSignature(true)) + "</span>" + (m.getComment() != null ? "&mdash; <span class=doc>" + m
					    .getComment() + "</span>" : "")));
			}
		}
		for (JavaMethod m : j.getMethods()) {
			if (m.getName().startsWith(prefix) && m.getModifiers().contains("public") && (!staticsOnly || m.getModifiers().contains("static"))) {
				r.add(new Execution.Completion(-1, -1, m.getName(), "<span class=type>" + compress(m.getName(), m
					    .getDeclarationSignature(true)) + "</span>" + (m.getComment() != null ? "&mdash; <span class=doc>" + m
					    .getComment() + "</span>" : "")));
			}
		}


		return r;
	}

	private String compress(String name, String signature) {
		signature = " " + signature;

		Pattern p = Pattern.compile("([A-Za-z]*?)\\.([A-Za-z]*?)");
		Matcher m = p.matcher(signature);

		System.out.println(" matcher ? " + m);

		while (m.find()) {
			signature = m.replaceAll("$2");
			m = p.matcher(signature);
			System.out.println(" matcher2 ? " + m);
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
					rr.add(new Pair<>(e.getKey(), "from "+e.getValue()));
				}
			}
		}

		if (rr.size()>100) rr = rr.subList(0, 100);

		return rr;
	}


}
