package fieldagent.transformations;

import fieldagent.Transform;
import fieldagent.asm.Label;
import fieldagent.asm.Opcodes;
import fieldagent.asm.Type;
import fieldagent.asm.commons.AdviceAdapter;
import fieldagent.asm.commons.Method;
import fieldagent.asm.tree.AnnotationNode;
import fieldagent.asm.tree.ClassNode;
import fieldagent.asm.tree.MethodNode;
import fieldagent.asm.util.CheckMethodAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Wrap implements TransformsMethod {

	public interface Wrapper<T, R> {
		public void begin(T source, Object[] args);

		public R end(T source, R returning);

		public Throwable abnormal(T source, Throwable returning);
	}

	static public class Cancel extends RuntimeException {
		public final Object ret;

		public Cancel(Object ret) {
			this.ret = ret;
		}
	}

	static Map<String, Map<String, Object>> parameters = new WeakHashMap<>();
	static Map<String, EntryHandler> entryHandlers = new WeakHashMap<>();
	static Map<String, ExitHandler> exitHandlers = new WeakHashMap<>();
	static Map<String, AbnormalHandler> abnormalHandlers = new WeakHashMap<>();

	public interface EntryHandler {
		public Cancel handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameters, Object[] argArray);
	}

	public interface ExitHandler {
		public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameters, String methodReturnName);
	}

	public interface AbnormalHandler {
		public Object handle(Throwable throwingThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameters, String methodReturnName);
	}

	public interface Handler extends EntryHandler, ExitHandler, AbnormalHandler
	{
	}

	private static final Type Type_Object = Type.getType(Object.class);
	private static final Type Type_String = Type.getType(String.class);

	private static final Type[] Type_enter_sig = new Type[]{Type_String, Type_Object, Type_String, Type_String, Type.getType(Object[].class)};

	static public Cancel enter(String fromName, Object fromThis, String methodName, String parameterName, Object[] argArray) {
		return entryHandlers.get(fromName).handle(fromName, fromThis, methodName, parameters.get(parameterName), argArray);
	}

	private static final Type[] Type_exit_sig = new Type[]{Type_Object, Type_String, Type_Object, Type_String, Type_String, Type_String};

	static public Object exit(Object returningThis, String fromName, Object fromThis, String methodName, String parameterName, String methodReturnName) {
		return exitHandlers.get(fromName).handle(returningThis, fromName, fromThis, methodName, parameters.get(parameterName), methodReturnName);
	}

	private static final Type[] Type_abnormal_sig = new Type[]{Type.getType(Throwable.class), Type_String, Type_Object, Type_String, Type_String, Type_String};

	static public Object abnormal(Throwable returningThis, String fromName, Object fromThis, String methodName, String parameterName, String methodReturnName) {
		return abnormalHandlers.get(fromName).handle(returningThis, fromName, fromThis, methodName, parameters.get(parameterName), methodReturnName);
	}

	static public int uniq_parameter;

	public byte[] transform(ClassNode node, MethodNode method, AnnotationNode annotation, Map<String, Object> parameters, byte[] classfileBuffer) {
		try {
			Type handlert = (Type) parameters.get("value");

			final Class handler = (Class) Thread.currentThread().getContextClassLoader().loadClass(handlert.getClassName());
			java.lang.reflect.Method[] m = handler.getMethods();
			java.lang.reflect.Method begin = null;
			java.lang.reflect.Method end = null;
			java.lang.reflect.Method abnormal = null;
			for (java.lang.reflect.Method mm : m) {
				if (mm.getName().equals("begin")) begin = mm;
				if (mm.getName().equals("end")) end = mm;
				if (mm.getName().equals("abnormal")) abnormal= mm;
			}
			final java.lang.reflect.Method fbegin = begin;
			final java.lang.reflect.Method fend = end;
			final java.lang.reflect.Method fabnormal = abnormal;

			MethodNode transformed = new MethodNode(Opcodes.ASM5, method.access, method.name, method.desc, method.signature, method.exceptions.toArray(new String[0]));
			final String newMethodName = method.name + "_original$fieldagent" + (uniq_parameter++);
			final String name = node.name + "/" + method.desc + "/" + (uniq_parameter++);
			parameters.put(name, parameters);

			Handler h = new Handler()
			{
				// wrappers are instantiated per-method, not per-instance
				java.lang.reflect.Method originalMethod;
				Object wrapper = null;

				@Override
				public Cancel handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray) {
					if (fbegin != null) try {
						if (originalMethod == null) {
							originalMethod = findMethod(fromThis.getClass(), newMethodName);
							wrapper = handler.getConstructor(java.lang.reflect.Method.class).newInstance(originalMethod);
						}
						fbegin.invoke(wrapper, fromThis, argArray);
					} catch (IllegalAccessException e) {
						System.out.println(" unexpected exception thrown in begin " + e.getClass() + " " + Cancel.class);
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						if (e.getCause() instanceof Cancel) {
							if (Transform.debug) System.out.println(" -- returning cancel -- with :" + ((Cancel) e.getCause()).ret);
							return (Cancel) e.getCause();
						}
						e.printStackTrace();
						RuntimeException r = new RuntimeException(" Exception inside wrapper begin method " + wrapper + " " + fromThis + " " + methodName);
						r.initCause(e.getCause());
						throw r;
					} catch (Throwable t) {
						System.out.println(" exception thrown in begin " + t.getClass() + " " + Cancel.class);
						t.printStackTrace();
						RuntimeException r = new RuntimeException(" Exception inside wrapper begin method " + wrapper + " " + fromThis + " " + methodName);
						r.initCause(t);
						throw r;
					}
					return null;
				}

				private java.lang.reflect.Method findMethod(Class<? extends Object> aClass, String newMethodName) {
					if (aClass == null) return null;
					for (java.lang.reflect.Method mm : aClass.getDeclaredMethods()) {
						if (mm.getName().equals(newMethodName)) return mm;
					}
					java.lang.reflect.Method m = findMethod(aClass.getSuperclass(), newMethodName);
					if (m != null) return m;
					Class[] inter = aClass.getInterfaces();
					for (Class ii : inter) {
						m = findMethod(ii, newMethodName);
						if (m != null) return m;
					}
					return null;
				}

				@Override
				public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
					if (fend != null) try {
						if (originalMethod == null) {
							originalMethod = findMethod(fromThis.getClass(), newMethodName);
							wrapper = handler.getConstructor(java.lang.reflect.Method.class).newInstance(originalMethod);
						}
						if (Transform.debug) System.out.println(" invoking end :"+fend+" "+wrapper+" "+fromThis+" "+returningThis);

						Object o = fend.invoke(wrapper, fromThis, returningThis);
						return o;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						if (e.getCause() instanceof Cancel) {
							if (Transform.debug) System.out.println(" -- returning cancel -- with :" + ((Cancel) e.getCause()).ret);
							return (Cancel) e.getCause();
						}
						e.printStackTrace();
						RuntimeException r = new RuntimeException(" Exception inside wrapper begin method " + wrapper + " " + fromThis + " " + methodName);
						r.initCause(e.getCause());
						throw r;
					} catch (Throwable t) {
						System.out.println(" exception thrown in begin " + t.getClass() + " " + Cancel.class);
						t.printStackTrace();
						RuntimeException r = new RuntimeException(" Exception inside wrapper begin method " + wrapper + " " + fromThis + " " + methodName);
						r.initCause(t);
						throw r;
					}
					return returningThis;
				}

				@Override
				public Object handle(Throwable throwingThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
					if (fabnormal != null) try {
						if (originalMethod == null) {
							originalMethod = findMethod(fromThis.getClass(), newMethodName);
							wrapper = handler.getConstructor(java.lang.reflect.Method.class).newInstance(originalMethod);
						}
						if (Transform.debug) System.out.println(" invoking abnormal :"+fend+" "+wrapper+" "+fromThis+" "+throwingThis);

						Object o = fabnormal.invoke(wrapper, fromThis, throwingThis);
						return o;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						if (e.getCause() instanceof Cancel) {
							System.out.println(" -- returning cancel -- with :" + ((Cancel) e.getCause()).ret);
							return (Cancel) e.getCause();
						}
						e.printStackTrace();
						RuntimeException r = new RuntimeException(" Exception inside wrapper begin method " + wrapper + " " + fromThis + " " + methodName);
						r.initCause(e.getCause());
						throw r;
					} catch (Throwable t) {
						System.out.println(" exception thrown in begin " + t.getClass() + " " + Cancel.class);
						t.printStackTrace();
						RuntimeException r = new RuntimeException(" Exception inside wrapper begin method " + wrapper + " " + fromThis + " " + methodName);
						r.initCause(t);
						throw r;
					}
					return throwingThis;
				}
			};

			entryHandlers.put(name, h);
			exitHandlers.put(name, h);
			abnormalHandlers.put(name, h);

			AdviceAdapter aa = new AdviceAdapter(Opcodes.ASM5, transformed, method.access, method.name, method.desc) {

				Set<Type> primitives = new LinkedHashSet<>(Arrays.asList(Type.VOID_TYPE, Type.BOOLEAN_TYPE, Type.BYTE_TYPE, Type.CHAR_TYPE, Type.DOUBLE_TYPE, Type.FLOAT_TYPE, Type.INT_TYPE, Type.SHORT_TYPE, Type.LONG_TYPE));

				protected void onMethodEnter() {
					push(name);
					loadThis();
					push(node.name);
					push(name);
					loadArgArray();
					invokeStatic(Type.getType(Wrap.class), new Method("enter", Type.getType(Cancel.class), Type_enter_sig));
					Label end_preamble_unwrap = this.newLabel();

					dup();
					ifNull(end_preamble_unwrap);
					getField(Type.getType(Cancel.class), "ret", Type_Object);
					Type returnType = new Method(method.name, method.desc).getReturnType();
					if (Transform.debug) System.out.println(" return type is :"+returnType);
					if (primitives.contains(returnType))
					{
						unbox(returnType);
					}
					else
					{
						checkCast(returnType);
					}
					returnValue();
					visitLabel(end_preamble_unwrap);
				}

				int ret = 0;
				protected void onMethodExit(int opcode) {
					if (Transform.debug) System.out.println(" return is opcode :"+opcode);
					if (opcode == ATHROW)
					{
						push(name);
						loadThis();
						push(node.name);
						push(name);
						push("return"+(ret++));
						invokeStatic(Type.getType(Wrap.class), new Method("abnormal", Type.getType(Object.class), Type_abnormal_sig));
						checkCast(Type.getType(Throwable.class));
						return;
					}

					if (opcode != RETURN && opcode !=ARETURN)
						box(Type.getReturnType(this.methodDesc));

					push(name);
					loadThis();
					push(node.name);
					push(name);
					push("return"+(ret++));
					invokeStatic(Type.getType(Wrap.class), new Method("exit", Type.getType(Object.class), Type_exit_sig));
					if (opcode == RETURN) {
					} else if (opcode == ARETURN)
					{
						checkCast(Type.getReturnType(this.methodDesc));
					} else if (opcode == ATHROW) {
						checkCast(Type.getType(Throwable.class));

					} else {
						if (opcode == LRETURN || opcode == DRETURN) {
						} else {
						}
						unbox(Type.getReturnType(this.methodDesc));
					}
				}

				@Override
				public void visitEnd() {
					super.visitMaxs(100, 100);
					super.visitEnd();
				}
			};

			CheckMethodAdapter cma = new CheckMethodAdapter(aa);

			method.accept(cma);

			method.name = newMethodName;

			node.methods.add(transformed);

			return null;

		} catch (Throwable t) {
			t.printStackTrace();
			;
			return classfileBuffer;
		}
	}

}
