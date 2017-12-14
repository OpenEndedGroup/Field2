package fieldbox.execution;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.directorywalker.DirectoryScanner;
import com.thoughtworks.qdox.directorywalker.SuffixFilter;
import com.thoughtworks.qdox.model.*;
import com.thoughtworks.qdox.model.impl.DefaultJavaParameter;
import com.thoughtworks.qdox.parser.ParseException;
import field.app.RunLoop;
import field.graphics.FLine;
import field.utility.Documentation;
import field.utility.Log;
import field.utility.MarkdownToHTML;
import field.utility.Pair;
import fieldagent.Trampoline;
import fielded.boxbrowser.TransientCommands;
import fieldnashorn.annotations.HiddenInAutocomplete;
import fieldnashorn.annotations.JavaDocOnly;
import fieldnashorn.annotations.SafeToToString;
import fieldnashorn.annotations.StopAutocompleteHere;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * When Nashorn/Javascript completion bumps up against something that's actually a Java object, we can use Java reflection based completion. Better yet, if we have the source code fore that Java we
 * can get very good completion information with parameter names, generics and javadocs. We're using qdox to parse the Java sources.
 * <p>
 * This class also contains our class discovery mechanism for import help
 * <p>
 * Todo: this is useful across language runtimes, not just Nashorn/Javascript.
 */
public class JavaSupport {

	static public JavaSupport javaSupport = null;

	private final JavaProjectBuilder builder;

	Map<String, String> allClassNames = new LinkedHashMap<>();

	static public Set<String> srcZipsDeltWith = new LinkedHashSet<>();

	static public Set<String> packageWhitelist = new LinkedHashSet<>();

	public JavaSupport() {
		javaSupport = this;

		builder = new JavaProjectBuilder();

		Set<Path> all = new LinkedHashSet<>();

		ClassLoader classLoader = Thread.currentThread()
			.getContextClassLoader();

		packageWhitelist.add("java.lang");
		packageWhitelist.add("java.io");
		packageWhitelist.add("org.lwjgl.opengl");

		// we defer this to the main loop (and _then_ punt it off to a separate thread) in order for the majority of Field internal classes to be loaded (for example, the graphics system)
		RunLoop.main.once(() -> {
			RunLoop.workerPool.submit(() -> {
				Log.log("jar.indexer", () -> "has started up");
				try {
					List<URL> paths = ((Trampoline.ExtensibleClassloader) classLoader).collectURLS();

					String appDir = System.getProperty("appDir");
					if (new File(appDir, "src").exists() && new File(appDir, "src").isDirectory()) {
						Log.log("jar.indexer", () -> "found a src directory in appDir");
						if (indexSrcTree(appDir + "/src")) ;
					}

					Log.log("jar.indexer", () -> "will index paths:" + paths + " from classloader " + classLoader);
					for (URL path : paths) {
						Log.log("jar.indexer", () -> "will index path " + path);
						RunLoop.workerPool.submit(() -> {
							Map<String, String> a = indexClasses(path);

							Log.log("jar.indexer", () -> "indexed path " + path + " and got " + a.size() + " classes");

							synchronized (allClassNames) {
								allClassNames.putAll(a);
							}

							File f = new File(path.getFile());
							while (f != null) {

								File finalF2 = f;
								Log.log("jar.indexer", () -> "recursing upwards to :" + finalF2.getAbsolutePath());


								if (f.isDirectory()) {
									if (new File(f, "src.zip").exists()) {
										final File finalF = f;
										Log.log("jar.indexer", () -> "found a src.zip in classpath <" + finalF + ">");

										String p = new File(f, "src.zip").getAbsolutePath();
										synchronized (srcZipsDeltWith) {
											if (srcZipsDeltWith.contains(p))
												break;
											srcZipsDeltWith.add(p);
											try {
												indexSrcZip(p);
											} catch (IOException e) {
												e.printStackTrace();
											}
											break;
										}
									} else if (new File(f, "srcjars").exists()) {
										for (File ff : new File(f, "srcjars").listFiles(
											x -> x.getName().endsWith(".jar"))) {
											String p = ff.getAbsolutePath();
											synchronized (srcZipsDeltWith) {
												if (srcZipsDeltWith.contains(p))
													break;
												srcZipsDeltWith.add(p);
												try {
													indexSrcZip(p);
												} catch (IOException e) {
													e.printStackTrace();
												}
											}
										}
									}
								}
								if (new File(f, "src").exists() && new File(f, "src").isDirectory()) {
									File finalF1 = f;
									Log.log("jar.indexer",
										() -> "found a src directory in classpath <" + finalF1 + "/src>");
									if (indexSrcTree(f + "/src")) break;
								}


								f = f.getParentFile();
							}
						});
					}

					allClassNames.putAll(indexJigsaw());

					builder.setErrorHandler(e -> Log.log("completion.general",
						() -> " problem parsing Java source file for completion, will skip this file and continue on "));
					builder.addClassLoader(classLoader);


					String root = fieldagent.Main.app;
					Files.walkFileTree(FileSystems.getDefault()
						.getPath(root), new FileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							if (dir.endsWith("temp")) return FileVisitResult.SKIP_SUBTREE;
							if (dir.endsWith("src")) {
								Log.log("jar.indexer", () -> " added " + dir + " to source path");
								all.add(dir);
								try {
									builder.addSourceTree(dir.toFile(),
										f -> Log.log("jar.indexer",
											() -> " error parsing file " + f + ", but we'll continue on anyway..."));
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
					Log.log("completion.debug", () -> " all is :" + all);
					if (failedToParse.size() > 0) {
						Log.log("completion.error",
							() -> "The following source files failed to parse correctly (this is likely a problem with the parser, not the code):" + failedToParse);

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		});
	}

	private boolean indexSrcTree(String p) {
		if (srcZipsDeltWith.contains(p)) {
			Log.log("jar.indexer", () -> " we've already delt with path " + p);
			return true;
		}
		srcZipsDeltWith.add(p);
		Log.log("jar.indexer", () -> " added " + p + " to source path given classpath");
//									builder.addSourceTree(new File(p));

		DirectoryScanner scanner = new DirectoryScanner(new File(p));
		scanner.addFilter(new SuffixFilter(".java"));

		scanner.scan(new com.thoughtworks.qdox.directorywalker.FileVisitor() {
			public void visitFile(File currentFile) {
				try {

					testName(currentFile.getAbsolutePath().replace(".java", ""));

					builder.addSource(currentFile);
				} catch (com.thoughtworks.qdox.parser.ParseException e) {
					failedToParse.add(p);
				} catch (Throwable t) {
					t.printStackTrace();
				}

			}
		});
		return false;
	}

	private boolean testName(String currentFile) {

		if (currentFile.endsWith(".java"))
			currentFile = currentFile.substring(0, currentFile.length()-".java".length());
		if (currentFile.endsWith(".class"))
			currentFile = currentFile.substring(0, currentFile.length()-".class".length());

		List<String> pieces = Arrays.asList(currentFile.replace("$", ".").split("[/\\\\]"));
		String ts = String.join(".", pieces);
//		System.out.println(" check '"+ts+"' -> ");
		for (String p : packageWhitelist) {
//			System.out.println("    "+p);
			if (ts.contains(p)) {
//				System.out.println(" yes");
				return true;
			}
		}

		return false;
//		if (packageWhitelist.size() > 0) {
//			List<String> pieces = Arrays.asList(currentFile.replaceAll(".java", "").split(File.separator));
//			Log.log("jar.indexer", () -> " testing " + pieces);
//			boolean found = false;
//			for (int i = pieces.size() - 1; i >= 0; i--) {
//				String test = String.join(".", pieces.subList(i, pieces.size()));
//				Log.log("jar.indexer", () -> " test " + test);
//				if (packageWhitelist.contains(test)) {
//					return true;
//				}
//			}
//		}
//		return false;
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
		signature = signature.replace(" static ", " ");
		signature = signature.replace("double ", "");
		signature = signature.replace("float ", "");

		//		signature = signature.replace(" void ", " ");

		signature = signature.trim();
		String[] leader = signature.split(" ");
		if (signature.contains(name)) {
			signature = signature.replaceFirst(leader[0], "") + " -> " + leader[0];
			signature = signature.replaceFirst(name, "");
		}

//		p = Pattern.compile(" " + name + "[ \\(]");
//		m = p.matcher(signature);
//		if (m.find()) {
//			signature = m.replaceAll("&rarr;(");
//		}

		signature = signature.replaceAll(" +", " ");

		return signature.trim();
	}

	private Set<String> failedToParse = new LinkedHashSet<>();

	private void indexSrcZip(String filename) throws IOException {
		try {
			ZipFile zipFile = new ZipFile(filename);
			Enumeration entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry) entries.nextElement();
				try {
					if (zipEntry.getName().endsWith(".java") && testName(zipEntry.getName())) {
						String u = "jar:" + new File(filename).toURI().toURL() + "!/" + zipEntry.getName();
						Log.log("jar.indexer", () -> "will index a source file from a jar:" + u);
						builder.setEncoding(Charset.defaultCharset().name());
						builder.addSource(new URL(u));
					}
				} catch (com.thoughtworks.qdox.parser.ParseException t) {
					failedToParse.add(zipEntry.getName());
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		} catch (FileSystemException e) {
//			System.out.println(" FSE " + filename);
		}
	}

	private Map<String, String> indexJigsaw() {
		Log.log("jar.indexer", () -> "will index jigsaw");
		try {
			FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
			HashMap<String, String> r = new HashMap<>();
			for (Path p : fs.getRootDirectories()) {
				Log.log("jar.indexer", () -> "jigsaw root is :" + p);
				Files.walkFileTree(p, new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//						System.out.println(" visit :" + file);
						if (file.toString()
							.endsWith(".class")) {

							if (!testName(file.toString().replace(".class", "")))
								return FileVisitResult.CONTINUE;

//							System.out.println(" pass ");
							String q = file.toString();

							String[] split = q.split("/");
							String name = "";
							for (int i = 3; i < split.length; i++) {
								name = name + "." + split[i];
							}
							name = name.substring(1);
							name = name.replace("//", ".");
							name = name.replace("/", ".");
							name = name.replace("\\", ".");
							name = name.replace(".class", "");
							name = name.replace("$", ".");

//							System.out.println(" -> " + name);

							r.put(name, split[2]);
						}
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
			}
			Log.log("jar.indexer", () -> "indexed " + r.size());

			return r;
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return Collections.emptyMap();
	}

	private Map<String, String> indexClasses(URL path) {
		String f = path.getFile();
		if (f.endsWith(".jar")) return indexClasses_jar(f);
		else if (new File(f).exists()) {
			return indexClasses_tree(f);
		} else Log.log("indexer", () -> "path " + path + " " + f + " does not exist");
		return Collections.emptyMap();
	}

	private Map<String, String> indexClasses_tree(String f) {

		Map<String, String> ret = new LinkedHashMap<>();

		try {
			Files.walkFileTree(FileSystems.getDefault()
				.getPath(f), new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String name = file.toString();

					if (name.endsWith(".class")) {
						if (!testName(name.replace(".class", "")))
							return FileVisitResult.CONTINUE;

						String tail = name.substring(f.length() + (f.endsWith("/") ? 0 : 1), name.length());
						tail = tail.replace("//", "/");
						tail = tail.replace("//", "/");
						tail = tail.replace('/', '.');
						tail = tail.replace('\\', '.');
						tail = tail.replace(".class", "");
						tail = tail.replace("$", ".");
						ret.put(tail, f);
					}

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
		return ret;

	}

	private Map<String, String> indexClasses_jar(String f) {
		if (!new File(f).exists()) return Collections.emptyMap();
		try {
			return new JarFile(f).stream()
				.filter(e -> e.getName()
					.endsWith(".class"))
				.filter(e -> testName(e.getName().replace(".class", "")))
				.map(e -> e.getName()
					.replace('/', '.')
					.replace(".class", ""))
				.collect(Collectors.toMap(x -> x, x -> f));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyMap();
	}

	public List<Completion> getOptionCompletionsFor(Object o, String prefix) {
		return getOptionCompletionsFor(o, prefix, false);
	}

	public List<Completion> getCompletionsFor(Object o, String prefix) {
		if (o instanceof HandlesCompletion) {

			Log.log("completion.debug", () -> " object :" + o + " is a completion handler ");

			return ((HandlesCompletion) o).getCompletionsFor(prefix);
		} else Log.log("completion.debug", () -> " object :" + o + " is not a completion handler ");

		List<Completion> r = getOptionCompletionsFor(o, prefix, false);

		return r;
	}

	public List<Completion> getCompletionsFor(Object o, String prefix, boolean includeConstructors) {
		if (o instanceof HandlesCompletion) {

			Log.log("completion.debug", () -> " object :" + o + " is a completion handler ");

			return ((HandlesCompletion) o).getCompletionsFor(prefix);
		} else Log.log("completion.debug", () -> " object :" + o + " is not a completion handler ");

		List<Completion> r = getOptionCompletionsFor(o, prefix, includeConstructors);

		return r;
	}

	public List<Completion> getOptionCompletionsFor(Object o, String prefix, boolean includeConstructors) {


		Log.log("completion.debug", () -> "getOptionCompletionsFor " + o + " / " + prefix);

		boolean staticsOnly = o instanceof Class;

		Class c = o instanceof Class ? (Class) o : o.getClass();

		boolean wasJava = c.getName()
			.startsWith("java");

		JavaClass j = builder.getClassByName(c.getName());
		List<Completion> r = new ArrayList<>();

		final JavaClass finalJ = j;
		boolean finalIncludeConstructors = includeConstructors;
		Log.log("completion.debug",
			() -> " java class (for javadoc supported completion) :" + finalJ + " prefix is <" + prefix + "> " + finalIncludeConstructors + " " + staticsOnly);

		try {
			while (j != null) {

				boolean docOnly = c.getAnnotation(JavaDocOnly.class) != null;
				if (c.getAnnotation(StopAutocompleteHere.class) != null) break;

				if (c.getAnnotation(HiddenInAutocomplete.class) == null) {

					if (staticsOnly && includeConstructors) {
						for (JavaConstructor m : j.getConstructors()) {
							Log.log("completion.debug", () -> "looking at constructor " + m);

							if (hasAnnotation(m.getAnnotations(), HiddenInAutocomplete.class))
								continue;
							if (docOnly && m.getComment()
								.trim()
								.length() < 1) continue;

							String val = "";
							boolean tostring = false;
							if (hasAnnotation(m.getAnnotations(), SafeToToString.class)) {
								try {
									val = "= <b>" + access(c.getDeclaredMethod(m.getName())).invoke(o) + "</b> &nbsp;";
									tostring = true;
								} catch (Throwable t) {
									t.printStackTrace();
								}
							}

							if ((prefix.equals("") || m.getName()
								.startsWith(prefix)) && m.getModifiers()
								.contains("public")) {

								String classComment = m.getDeclaringClass().getComment();
								String constructorComment = m.getComment();
								String comment = ((classComment == null ? "" : classComment) + " " + (constructorComment == null ? "" : constructorComment))
									.trim();

								comment = MarkdownToHTML.convert(comment);

								Completion cc = new Completion(-1, -1, m.getName(),
									val + "<span class=type>" + compress(m.getName(),
										"(" + m.getParameters()
											.stream()
											.map(x -> x.getType() + " " + x
												.getName())
											.reduce("",
												(a, b) -> a + ", " + b)
											.substring(
												m.getParameters()
													.size() > 0 ? 2 : 0)) + ")</span>" + (comment
										.length() > 0 ? "<span class=type>&nbsp;&mdash;</span> <span class=doc>" + comment + "</span>" : ""));
								add(val.length() > 0, r, cc);
								cc.rank += tostring ? 100 : 0;
								cc.rank += m.getComment() != null ? 10 : 0;
							}
						}
					}

					for (JavaField m : j.getFields()) {
						if (hasAnnotation(m.getAnnotations(), HiddenInAutocomplete.class))
							continue;

						if (m.isStatic() && isTypeProp(m))
							continue;

						if (docOnly && m.getComment()
							.trim()
							.length() < 1) continue;

						String annotationDoc = getDocumentationFromAnnotation(c, m);

						String val = "";
						boolean tostring = false;
						if (!staticsOnly && (hasAnnotation(m.getAnnotations(), SafeToToString.class) || m.getType()
							.isPrimitive())
							|| hasAnnotation(
							builder.getClassByName(m.getType().getCanonicalName()).getAnnotations(),
							SafeToToString.class)) {
							try {
								val = "= <b>" + access(c.getDeclaredField(m.getName())).get(o) + "</b> &nbsp;";
								tostring = true;
							} catch (Throwable t) {

//								t.printStackTrace();
							}
						}

						if ((prefix.equals("") || m.getName()
							.startsWith(prefix)) && m.getModifiers()
							.contains("public") && (!staticsOnly || m.getModifiers()
							.contains("static"))) {
							Completion cc = new Completion(-1, -1, m.getName(),
								val + "<span class=type>" + compress(m.getName(),
									m.getDeclarationSignature(
										true)) + "</span>" + (annotationDoc != null ? ("<span class=type>&nbsp;&mdash;</span> <span class=doc>" + MarkdownToHTML.unwrapFirstParagraph(MarkdownToHTML.convert(annotationDoc)) + "</span>") : (m
									.getComment() != null ? "<span class=type>&nbsp;&mdash;</span> <span class=doc>" + MarkdownToHTML.unwrapFirstParagraph(MarkdownToHTML.convert(m.getComment())) + "</span>" : "")));
							add(val.length() > 0, r, cc);
							cc.rank += tostring ? 100 : 0;
							cc.rank += m.getComment() != null ? 10 : 0;
							cc.type = 1; // dont' merge with methods
						}
					}
					for (JavaMethod m : j.getMethods()) {
						if (hasAnnotation(m.getAnnotations(), HiddenInAutocomplete.class))
							continue;

						if (m.getName().contains("$")) continue;

						if (m.getName().equals("toString")) continue;
						if (m.getName().equals("equals")) continue;
						if (m.getName().equals("hashCode")) continue;

						if (docOnly && m.getComment()
							.trim()
							.length() < 1) continue;


						String annotationDoc = getDocumentationFromAnnotation(c, m);

						String val = "";
						boolean tostring = false;
						if (!staticsOnly && hasAnnotation(m.getAnnotations(), SafeToToString.class)) {
							try {
								val = "= <b>" + access(c.getDeclaredMethod(m.getName())).invoke(o) + "</b> &nbsp;";
								tostring = true;
							} catch (Throwable t) {
								t.printStackTrace();
							}
						}

						if ((prefix.equals("") || m.getName()
							.startsWith(prefix)) && m.getModifiers()
							.contains("public") && (!staticsOnly || m.getModifiers()
							.contains("static"))) {
							Completion cc = new Completion(-1, -1, m.getName(),
								val + "<span class=type>" + getDeclarationSignature(c,
									m) + "</span>" + (annotationDoc != null ? ("<span class=type>&nbsp;&mdash;</span> <span class=doc>" + MarkdownToHTML.unwrapFirstParagraph(MarkdownToHTML.convert(annotationDoc)) + "</span>") : (m
									.getComment() != null ? "<span class=type>&nbsp;&mdash;</span> <span class=doc>" + MarkdownToHTML.unwrapFirstParagraph(MarkdownToHTML.convert(m.getComment())) + "</span>" : "")));
							add(val.length() > 0, r, cc);
							cc.rank += tostring ? 100 : 0;
							cc.rank += m.getComment() != null ? 10 : 0;
						}
					}
				}


				c = c.getSuperclass();
				if (c == null) break;
				j = builder.getClassByName(c.getName());

				//TODO: should we stop when we get into java.* classes if we started from something that wasn't?
				// PApplet's completion is heavily polluted by this stuff.
				// let's try that now.

				boolean isJava = c.getName()
					.startsWith("java") || c.getName()
					.startsWith("jdk");
				if (!wasJava && isJava) break;

				includeConstructors = false; // only include constructors from the initial class
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		Collections.sort(r, (a, b) -> -Double.compare(a.rank, b.rank));

		return r;
	}


	private String getDeclarationSignature(Class c, JavaMethod m) throws ClassNotFoundException {

		// try to dig out better names from the class itself if qdox has missed them
		try {
			List<JavaType> t = m.getParameterTypes();
			Class[] tt = new Class[t.size()];
			for (int i = 0; i < t.size(); i++) {
				tt[i] = classFor(t.get(i).getFullyQualifiedName());
			}
			Method meth = c.getDeclaredMethod(m.getName(), tt);
			for (int i = 0; i < meth.getParameters().length; i++) {
				if (((DefaultJavaParameter) m.getParameters().get(i)).getName().startsWith("p"))
					((DefaultJavaParameter) m.getParameters().get(i)).setName(meth.getParameters()[i].getName());
			}
		} catch (NoSuchMethodException e) {

		}

		String sig = compress(m.getName(), m.getDeclarationSignature(true));
//		System.out.println(" sig for " + m.getClass() + " is " + sig);

		// if sig has no names, qdox doesn't seem to introspect it at all

		return sig;
	}

	private String getDocumentationFromAnnotation(Class c, JavaField m) {
		try {
			return c.getDeclaredField(m.getName()).getAnnotation(Documentation.class).value();
		} catch (Throwable e) {
			return null;
		}
	}

	private String getDocumentationFromAnnotation(Class c, JavaMethod m) {
		try {
			List<JavaType> t = m.getParameterTypes();
			Class[] tt = new Class[t.size()];
			for (int i = 0; i < t.size(); i++) {
				tt[i] = classFor(t.get(i).getFullyQualifiedName());
			}
			Method meth = c.getDeclaredMethod(m.getName(), tt);
			Documentation d = meth.getAnnotation(Documentation.class);
			return d == null ? null : d.value();
		} catch (Throwable e) {
			return null;
		}
	}

	private Class classFor(String fullyQualifiedName) throws ClassNotFoundException {
		if (fullyQualifiedName.equals("int")) return Integer.TYPE;
		if (fullyQualifiedName.equals("long")) return Long.TYPE;
		if (fullyQualifiedName.equals("double")) return Double.TYPE;
		if (fullyQualifiedName.equals("float")) return Float.TYPE;
		if (fullyQualifiedName.equals("char")) return Character.TYPE;
		if (fullyQualifiedName.equals("byte")) return Byte.TYPE;
		if (fullyQualifiedName.equals("short")) return Short.TYPE;
		if (fullyQualifiedName.equals("boolean")) return Boolean.TYPE;
		return this.getClass().getClassLoader().loadClass(fullyQualifiedName);
	}

	// qdox can throw an NPE during isA when, presumably, it isN't
	private boolean isTypeProp(JavaField m) {
		try {
			return m.getType().isA("field.utility.Dict$Prop");
		} catch (NullPointerException e) {
			return false;
		}
	}

	private <T extends AccessibleObject> T access(T object) {
		try {
			object.setAccessible(true);
		} catch (java.lang.reflect.InaccessibleObjectException e) {
			// well, hello jigsaw...
		}
		return object;
	}

	private <T> void add(boolean first, List<T> l, T x) {

		if (first) l.add(0, x);
		else l.add(x);
	}

	private boolean hasAnnotation(List<JavaAnnotation> annotations, Class hiddenInAutocompleteClass) {
		return (annotations.stream()
			.filter(x -> x.getType()
				.getName()
				.equals(hiddenInAutocompleteClass.getName()
					.substring(1 + hiddenInAutocompleteClass.getName()
						.lastIndexOf('.'))))
			.findFirst()
			.isPresent());

	}

	public List<Pair<String, String>> getPossibleJavaClassesFor(String left) {
		try {
			List<Pair<String, String>> rr = new ArrayList<>();

			Set<String> seen = new LinkedHashSet<>();

			for (JavaClass c : builder.getClasses()) {
				if (c.getName()
					.contains(left) && !seen.contains(c.getFullyQualifiedName())) {
					seen.add(c.getFullyQualifiedName());
					rr.add(new Pair<>(c.getFullyQualifiedName(),
						"<br><span class=doc>" + MarkdownToHTML.unwrapFirstParagraph(
							MarkdownToHTML.convert(c.getComment())) + "</span>"));
				}
			}

			synchronized (allClassNames) {
				for (Map.Entry<String, String> e : allClassNames.entrySet()) {
					if (e.getKey()
						.contains(left) && !seen.contains(e.getKey())) {
						seen.add(e.getKey());
						rr.add(new Pair<>(e.getKey(), "<br><span class=doc>from " + e.getValue() + "</span>"));
					}
				}
			}

			if (rr.size() > 100) rr = rr.subList(0, 100);


			Collections.sort(rr, (a, b) -> {
				String[] pa = a.first.split("\\.");
				String[] pb = b.first.split("\\.");

				return Double.compare(pa[pa.length - 1].length(), pb[pb.length - 1].length());

			});

			return rr;
		} catch (Throwable t) {
			t.printStackTrace();
			return Collections.emptyList();
		}

	}


	public JavaClass sourceForClass(Class<?> of) {
		return builder.getClassByName(of.getName());
	}
}
