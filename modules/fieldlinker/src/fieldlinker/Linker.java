package fieldlinker;


import field.dynalink.CallSiteDescriptor;
import field.dynalink.linker.*;
import field.dynalink.support.Guards;
import field.nashorn.api.scripting.extensions.CustomDelete;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 */
public class Linker implements GuardingDynamicLinker, GuardingTypeConverterFactory {

	boolean disabled = System.getProperty("noLinker") != null;
	boolean debug = System.getProperty("debugLinker") != null;

	public interface AsMap extends CustomDelete {
		boolean asMap_isProperty(String p);

		Object asMap_call(Object a, Object b);

		Object asMap_get(String p);

		Object asMap_set(String p, Object o);

		Object asMap_new(Object a);

		Object asMap_new(Object a, Object b);

		Object asMap_getElement(int element);

		default Object asMap_getElement(Object element) { throw new NotImplementedException(); }

		Object asMap_setElement(int element, Object o);

		default  Object asMap_setElement(Object element, Object o) {return asMap_set(""+element, o);}

		default Object asMap_call(Object o) {
			return asMap_call(o, Collections.EMPTY_MAP);
		}
	}


	public Linker() {
		System.err.println(" linker has been instantiated " + disabled + " " + debug);
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

			if (debug)
			{
				System.err.println(" rec is of type "+(rec==null ? null : rec.getClass())+" / "+(rec instanceof AsMap));
				if (rec instanceof AsMap)
					System.err.println(" rec admits to having property :"+((AsMap) rec).asMap_isProperty(propertyName));
			}

			if (rec instanceof AsMap && ((AsMap) rec).asMap_isProperty(propertyName)) {

				System.err.println(" linking AsMap/get 2" + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_get", MethodType.methodType(Object.class, String.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("call") && linkRequest.getArguments().length == 3) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				System.err.println(" linking AsMap/call " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_call", MethodType.methodType(Object.class, Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("call") && linkRequest.getArguments().length == 2) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				System.err.println(" linking AsMap/call " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_call", MethodType.methodType(Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("new") && linkRequest.getArguments().length == 2) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				System.err.println(" linking AsMap/new " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_new", MethodType.methodType(Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("new") && linkRequest.getArguments().length == 3) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				System.err.println(" linking AsMap/new " + rec);
				MethodHandle get = MethodHandles.lookup()
								.findVirtual(rec.getClass(), "asMap_new", MethodType.methodType(Object.class, Object.class, Object.class));
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

				System.err.println(" linking AsMap/get 1" + rec+" admits to property "+propertyName);
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
								.findVirtual(implementingClassFor(rec.getClass()), "asMap_set", MethodType.methodType(Object.class, String.class, Object.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}

		} else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("setElem|setProp")) {
			Object rec = linkRequest.getReceiver();

			if (linkRequest.getArguments().length==3) {

				if (rec instanceof AsMap) {

					System.err.println(" linking AsMap/setElement " + rec);
					MethodHandle get = MethodHandles.lookup()
									.findVirtual(implementingClassFor(rec.getClass()), "asMap_setElement", MethodType.methodType(Object.class, Object.class, Object.class));

					return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
				}
			}
			else
			{
				String propertyName = linkRequest.getCallSiteDescriptor()
								 .getNameToken(CallSiteDescriptor.NAME_OPERAND);

				if (rec instanceof AsMap) {

					System.err.println(" linking AsMap/setElement " + rec);
					MethodHandle get = MethodHandles.lookup()
									.findVirtual(implementingClassFor(rec.getClass()), "asMap_setElement", MethodType.methodType(Object.class, Object.class, Object.class));

					get = MethodHandles.insertArguments(get, 1, propertyName);

					return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
				}
			}


		}else if (linkRequest.getCallSiteDescriptor()
				      .getNameToken(CallSiteDescriptor.OPERATOR)
				      .equals("getElem|getProp|getMethod")) {
			Object rec = linkRequest.getReceiver();

			if (linkRequest.getArguments().length==2) {

				if (linkRequest.getArguments()[1].getClass().isPrimitive())
				{

					if (rec instanceof AsMap) {
						System.err.println(" linking AsMap/property get " + rec);
						MethodHandle get = MethodHandles.lookup()
										.findVirtual(rec.getClass(), "asMap_getElement", MethodType.methodType(Object.class, Integer.TYPE));

						return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));

					}
				}
				else
				{
					if (rec instanceof AsMap) {
						System.err.println(" linking AsMap/property get " + rec);
						MethodHandle get = MethodHandles.lookup()
										.findVirtual(rec.getClass(), "asMap_getElement", MethodType.methodType(Object.class, Object.class));


						return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));

					}

				}
			}
			else
			{
				String propertyName = linkRequest.getCallSiteDescriptor()
								 .getNameToken(CallSiteDescriptor.NAME_OPERAND);
				if (rec instanceof AsMap) {
					System.err.println(" linking AsMap/property get " + rec);
					MethodHandle get = MethodHandles.lookup()
									.findVirtual(rec.getClass(), "asMap_getElement", MethodType.methodType(Object.class, Object.class));

					get = MethodHandles.insertArguments(get, 1, propertyName);

					return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));

				}
			}

		}

		return null;
	}

	private Class<?> implementingClassFor(Class<? extends Object> aClass) {
		if (aClass==null) return null;

		Class<?>[] ii = aClass.getInterfaces();
		for(Class c : ii)
			if (c==AsMap.class) return aClass;

		return implementingClassFor(aClass.getSuperclass());
	}

	@Override
	public GuardedTypeConversion convertToType(Class<?> aClass, Class<?> aClass2) throws Exception {
//		System.err.println("LINKER convertToType :" + aClass + " " + aClass2);
		return null;
	}
}
