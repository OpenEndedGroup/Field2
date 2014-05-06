package fieldagent.transformations;

import fieldagent.asm.tree.AnnotationNode;
import fieldagent.asm.tree.ClassNode;
import fieldagent.asm.tree.MethodNode;

import java.util.Map;

public interface TransformsMethod {
	public byte[] transform(ClassNode node, MethodNode method, AnnotationNode annotation, Map<String, Object> parameters, byte[] classfileBuffer);
}
