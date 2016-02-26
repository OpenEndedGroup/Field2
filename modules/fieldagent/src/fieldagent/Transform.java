package fieldagent;

import fieldagent.asm.ClassReader;
import fieldagent.asm.ClassWriter;
import fieldagent.asm.tree.AnnotationNode;
import fieldagent.asm.tree.ClassNode;
import fieldagent.asm.tree.MethodNode;
import fieldagent.asm.util.CheckClassAdapter;
import fieldagent.transformations.TransformsMethod;
import fieldagent.transformations.Wrap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by marc on 3/12/14.
 */
public class Transform {
	static public final boolean debug = false;

	List<Class<? extends TransformsMethod>> methodTransformations = Arrays.asList(Wrap.class);
	Map<String, Class<? extends TransformsMethod>> methodAnnotationNames;

	public Transform()
	{
		// doing this causes a native crash on 1.8.0_20-ea-b05 --- is this the first time lambda has been used in a javaagent?
//		methodAnnotationNames = methodTransformations.stream().collect(Collectors.toMap(x -> "L" + x.getName().replace("transformations", "annotations").replace('.', '/') + ";", x -> x));

		methodAnnotationNames = new HashMap<>();
		for(Class<? extends TransformsMethod> c : methodTransformations)
		{
			methodAnnotationNames.put("L"+c.getName().replace("transformations", "annotations").replace('.','/')+";", c);
		}
	}



	public byte[] transform(String className, byte[] classfileBuffer) {

		try {
			ClassNode found = checkWoven(className, classfileBuffer);

			if (found != null) {
				return doTransform(found, className, classfileBuffer);
			} else {
				return classfileBuffer;
			}
		} catch (Throwable t) {
			System.err.println(" problem transforming " + className);
			t.printStackTrace();
			return classfileBuffer;
		}
	}

	private byte[] doTransform(ClassNode classNode, String className, byte[] classfileBuffer) {

		for (MethodNode method : new ArrayList<>(classNode.methods)) {
			if (debug) System.out.println(" method :" + method + " " + method.visibleAnnotations + " " + method.name + " " + method.signature);
			if (method.visibleAnnotations == null) continue;
			for (AnnotationNode visibleAnnotation : method.visibleAnnotations) {
				if (debug) System.out.println(" checking :" + visibleAnnotation.desc + " against " + methodAnnotationNames.keySet());
				Class<? extends TransformsMethod> methodTransformation = methodAnnotationNames.get(visibleAnnotation.desc);
				if (methodTransformation != null) {
					try {
						TransformsMethod m = methodTransformation.newInstance();

						Map<String, Object> parameters = new HashMap<String, Object>();
						for (int i = 0; i < visibleAnnotation.values.size() / 2; i++) {
							parameters.put((String) visibleAnnotation.values.get(2 * i), visibleAnnotation.values.get(2 * i + 1));
						}

						classfileBuffer = m.transform(classNode, method, visibleAnnotation, parameters, classfileBuffer);
						if (classfileBuffer == null) {
							ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
							CheckClassAdapter cca = new CheckClassAdapter(w);
							classNode.accept(debug ? cca : w);
							classfileBuffer = w.toByteArray();
						}
					} catch (Throwable e) {
						System.err.println(" problem transforming method " + method.name + " in class " + className + " with transformer " + methodTransformation);
						e.printStackTrace();
					}

				}
			}
		}

		return classfileBuffer;
	}

	protected ClassNode checkWoven(String className, byte[] classfileBuffer) {

		if (debug) System.out.println(" checking :" + className + " for woven");

		ClassReader reader = new ClassReader(classfileBuffer);
		ClassNode node = new ClassNode();
		reader.accept(node, ClassReader.EXPAND_FRAMES);

		boolean found = false;
		if (node.visibleAnnotations == null) return null;

		for (AnnotationNode annotation : node.visibleAnnotations) {
			if (annotation.desc.equals("Lfieldagent/annotations/Woven;")) {
				found = true;
			}
		}
		return found ? node : null;
	}
}
