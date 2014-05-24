package fieldnashorn;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.parser.ParseException;
import field.utility.Pair;
import fielded.Execution;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * When Nashorn/Javascript completion bumps up against something that's actually a Java object, we can use Java reflection based completion. Better
 * yet, if we have the source code fore that Java we can get very good completion information with parameter names, generics and javadocs. We're using
 * qdox to parse the Java sources.
 *
 * Todo: this is useful accross language runtimes, not just Nashorn/Javascript.
 */
public class JavaSupport {


	public interface HandlesCompletion {
		public List<Execution.Completion> getCompletionsFor(String prefix);
	}


	private final JavaProjectBuilder builder;

	public JavaSupport() {
		builder = new JavaProjectBuilder();


		System.out.println(" inside java support constructor ");

		List<Path> all = new ArrayList<>();

		new Thread(() -> {
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
		}).start();
		System.out.println(" all is :" + all);

	}

	public List<Execution.Completion> getCompletionsFor(Object o, String prefix) {
		if (o instanceof HandlesCompletion) {
			return ((HandlesCompletion) o).getCompletionsFor(prefix);
		}

		Class c = o instanceof Class ? (Class) o : o.getClass();

		JavaClass j = builder.getClassByName(c.getName());

		List<Execution.Completion> r = new ArrayList<>();
		for (JavaField m : j.getFields()) {
			if (m.getName().startsWith(prefix) && m.getModifiers().contains("public")) {
				r.add(new Execution.Completion(-1, -1, m.getName(), "<span class=type>" + compress(m.getName(), m
					    .getDeclarationSignature(true)) + "</span>" + (m.getComment() != null ? "&mdash; <span class=doc>" + m
					    .getComment() + "</span>" : "")));
			}
		}
		for (JavaMethod m : j.getMethods()) {
			if (m.getName().startsWith(prefix) && m.getModifiers().contains("public")) {
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
		for (JavaClass c : builder.getClasses()) {
			System.out.println(" javaclass :" + c.getFullyQualifiedName());
			if (c.getName().contains(left)) {
				rr.add(new Pair<>(c.getFullyQualifiedName(), c.getComment()));
			}
		}
		return rr;
	}


}
