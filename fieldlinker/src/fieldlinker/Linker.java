package fieldlinker;

import jdk.internal.dynalink.CallSiteDescriptor;
import jdk.internal.dynalink.linker.*;
import jdk.internal.dynalink.support.Guards;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 *
 */
public class Linker implements GuardingDynamicLinker, GuardingTypeConverterFactory {

	boolean disabled = System.getProperty("noLinker") != null;
	boolean debug = System.getProperty("debugLinker") != null;

	public interface AsMap {
		public boolean asMap_isProperty(String p);

		public Object asMap_call(Object a, Object b);

		public Object asMap_get(String p);

		public Object asMap_set(String p, Object o);

		public Object asMap_new(Object a);
	}


	public Linker() {
		System.err.println(" linker has been instantiated " + disabled+" "+debug);
	}

	@Override
	public GuardedInvocation getGuardedInvocation(LinkRequest linkRequest, LinkerServices linkerServices) throws Exception {
		if (disabled) return null;
		if (debug) {
			System.err.println("LINKER getGuardedInvocation :" + linkRequest + " " + linkerServices);
			System.err.println(" " + Arrays.asList(linkRequest.getArguments()));
			System.err.println(" " + linkRequest.getCallSiteDescriptor());
			System.err.println(" " + linkRequest.getCallSiteToken());
			System.err.println(" " + linkRequest.getLinkCount());
			System.err.println(" " + linkRequest.getReceiver());
		}
		if (linkRequest.getCallSiteDescriptor()
			       .getNameToken(CallSiteDescriptor.OPERATOR)
			       .equals("getProp|getElem|getMethod")) {

			Object rec = linkRequest.getReceiver();
			String propertyName = linkRequest.getCallSiteDescriptor()
							 .getNameToken(CallSiteDescriptor.NAME_OPERAND);

			if (rec instanceof AsMap && ((AsMap) rec).asMap_isProperty(propertyName)) {

				System.err.println(" linking AsMap/get " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_get", MethodType.methodType(Object.class, String.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("call")) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				System.err.println(" linking AsMap/call " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_call", MethodType.methodType(Object.class, Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("new")) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				System.err.println(" linking AsMap/new " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_new", MethodType.methodType(Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
				 .getNameToken(CallSiteDescriptor.OPERATOR)
				 .equals("getMethod|getProp|getElem")) {

			// this is a method lookup, it's possible we'd want to do something different here, for now, it's exactly the same

			Object rec = linkRequest.getReceiver();
			String propertyName = linkRequest.getCallSiteDescriptor()
							 .getNameToken(CallSiteDescriptor.NAME_OPERAND);

			if (rec instanceof AsMap && ((AsMap) rec).asMap_isProperty(propertyName)) {

				System.err.println(" linking AsMap/get " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_get", MethodType.methodType(Object.class, String.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("setProp|setElem")) {
			Object rec = linkRequest.getReceiver();
			String propertyName = linkRequest.getCallSiteDescriptor()
							 .getNameToken(CallSiteDescriptor.NAME_OPERAND);

			if (rec instanceof AsMap && ((AsMap) rec).asMap_isProperty(propertyName)) {

				System.err.println(" linking AsMap/set " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_set", MethodType.methodType(Object.class, String.class, Object.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}

		}

		return null;
	}

	@Override
	public GuardedTypeConversion convertToType(Class<?> aClass, Class<?> aClass2) throws Exception {
//		System.err.println("LINKER convertToType :" + aClass + " " + aClass2);
		return null;
	}
}
