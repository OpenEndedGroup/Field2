package fieldbox.documentation;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.thoughtworks.qdox.model.*;
import field.graphics.FLine;
import field.utility.Dict;
import field.utility.MarkdownToHTML;
import field.utility.Pair;
import fieldbox.boxes.Box;

import java.util.*;

/**
 * ##
 * Plugin that makes documentation out of the source tree
 */
public class Documentation extends Box {

	/**
	 * ##
	 * This property is a field that we are going to document so well
	 */
	static public final Dict.Prop<Documentation> documentation = new Dict.Prop<>("_documentation").type().toCanon();

	static public final String docprefix = "##";

	private final Box root;

	public Documentation(Box root) {
		this.root = root;
		this.properties.put(documentation, this);
	}

	/**
	 * ##
	 * This is some method
	 */
	public FLine someMethod() {
		return null;
	}

	static public final String renderStatic(String contents) {
		return renderStatic(contents, Collections.emptyMap());
	}

	static public final String renderStatic(String contents, Map extra) {
		Template tmpl = Mustache.compiler().compile(contents);

		Map<String, Object> context = new LinkedHashMap<>();
		context.put("live", false);
		context.putAll(extra);

		String result = tmpl.execute(context);
		return MarkdownToHTML.convert(result);
	}

	public static List<Pair<String, String>> renderStaticJava(JavaSource source) {
		List<JavaClass> jj = source.getClasses();
		List<Pair<String, String>> p = new ArrayList<>();

		for (JavaClass jjj : jj) {
			String comment = jjj.getComment();

			System.out.println(" comment is :" + comment);

			if (comment != null && comment.trim().startsWith(docprefix)) {
				String c = comment.trim().substring(docprefix.length()).trim();
				String h = MarkdownToHTML.unwrapFirstParagraph(headingForClass(jjj));
				String s = renderStatic(c);
				p.add(new Pair<>(h.trim(), s.trim()));
			}
			for (JavaField f : sortFields(jjj.getFields())) {
				if (f.getComment() == null) continue;
				System.out.println(" comment is :" + f.getComment());
				if (f.getComment().trim().startsWith(docprefix)) {
					String c = f.getComment().trim().substring(docprefix.length()).trim();
					String h = MarkdownToHTML.unwrapFirstParagraph(headingForField(f));
					String s = renderStatic(c);

					p.add(new Pair<>(h.trim(), s.trim()));
				}
			}
			for (JavaMethod f : sortMethods(jjj.getMethods())) {
				if (f.getComment() == null) continue;
				System.out.println(" comment is :" + f.getComment());
				if (f.getComment().trim().startsWith(docprefix)) {
					String c = f.getComment().trim().substring(docprefix.length()).trim();
					String h = MarkdownToHTML.unwrapFirstParagraph(headingForMethod(f));
					String s = renderStatic(c);

					p.add(new Pair<>(h.trim(), s.trim()));
				}
			}
		}
		return p;
	}

	private static List<JavaField> sortFields(List<JavaField> fields) {

		ArrayList<JavaField> j = new ArrayList<>(fields);
		j.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
		return j;
	}

	private static List<JavaMethod> sortMethods(List<JavaMethod> fields) {

		ArrayList<JavaMethod> j = new ArrayList<>(fields);
		j.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
		return j;
	}


	private static String headingForField(JavaField f) {
		return renderStatic("*" + f.getName() + "* &rarr; " + f.getType());
	}

	private static String headingForMethod(JavaMethod f) {
		return renderStatic("*" + f.getCallSignature() + "* &rarr; " + f.getReturnType());
	}

	private static String headingForClass(JavaClass f) {
		return renderStatic("*" + f.getName() + "*");
	}
}
