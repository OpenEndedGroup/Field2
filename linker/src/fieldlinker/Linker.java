package fieldlinker;


//import jdk.dynalink.CallSiteDescriptor;

import jdk.dynalink.beans.StaticClass;
import jdk.dynalink.linker.*;
//import jdk.dynalink.support.Guards;
//import jdk.nashorn.api.scripting.extensions.CustomDelete;
import jdk.dynalink.linker.support.Guards;
;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 */
public class Linker extends GuardingDynamicLinkerExporter implements GuardingDynamicLinker/*, GuardingTypeConverterFactory*/ {

	@Override
	public List<GuardingDynamicLinker> get() {
		return Collections.singletonList(this);
	}

	private boolean disabled = System.getProperty("noLinker") != null;
	private boolean debug = System.getProperty("debugLinker") != null;

	public Linker() {
		if (debug)
			System.err.println(" linker has been constructed ");
	}


	@Override
	public GuardedInvocation getGuardedInvocation(LinkRequest linkRequest, LinkerServices linkerServices) throws Exception {
		if (disabled) return null;
		if (debug) {
			System.err.println("LINKER getGuardedInvocation :" + linkRequest + " " + linkerServices);
			System.err.println(" " + Arrays.asList(linkRequest.getArguments()) + " .length =" + linkRequest.getArguments().length);
			System.err.println(" " + linkRequest.getCallSiteDescriptor());
			System.err.println(" " + linkRequest.getCallSiteDescriptor().getOperation());
			System.err.println(" " + linkRequest.getReceiver());
		}
		if (linkRequest.getCallSiteDescriptor().getOperation()
			.toString().startsWith("GET:PROPERTY|ELEMENT|METHOD:")) {

			Object rec = linkRequest.getReceiver();
			String propertyName = linkRequest.getCallSiteDescriptor().getOperation().toString().replace("GET:PROPERTY|ELEMENT|METHOD:", "");

			if (debug) {
				System.err.println(" rec is of type " + (rec == null ? null : rec.getClass()) + " / " + (rec instanceof AsMap));
				if (rec instanceof AsMap)
					System.err.println(" rec admits to having property :" + ((AsMap) rec).asMap_isProperty(propertyName));
			}

			if (rec instanceof AsMap && ((AsMap) rec).asMap_isProperty(propertyName)) {

				if (debug)
					System.err.println(" linking AsMap.java/get 2" + rec);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_get", MethodType.methodType(Object.class, String.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
			.getOperation().toString().startsWith("CALL") && linkRequest.getArguments().length == 3) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				if (debug)
					System.err.println(" linking AsMap.java/call " + rec);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_call", MethodType.methodType(Object.class, Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			} else if (rec instanceof AsMap_callable) {
				if (debug)
					System.err.println(" linking AsMap_callable/call " + rec);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_call", MethodType.methodType(Object.class, Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));

			}
		} else if (linkRequest.getCallSiteDescriptor()
			.getOperation().toString().startsWith("CALL") && linkRequest.getArguments().length == 2) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				if (debug)
					System.err.println(" linking AsMap.java/call " + rec);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_call", MethodType.methodType(Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			} else if (rec instanceof AsMap_callable) {

				if (debug)
					System.err.println(" linking AsMap.java/call " + rec);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_call", MethodType.methodType(Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}


		}  else if (linkRequest.getCallSiteDescriptor()
			.getOperation().toString().startsWith("NEW") && linkRequest.getArguments().length == 2) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				if (debug)
					System.err.println(" linking AsMap.java/new " + rec);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_new", MethodType.methodType(Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
			.getOperation().toString().startsWith("CALL") && linkRequest.getArguments().length == 3) {

			Object rec = linkRequest.getReceiver();

			if (rec instanceof AsMap) {

				if (debug)
					System.err.println(" linking AsMap.java/new " + rec);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_new", MethodType.methodType(Object.class, Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			} else if (rec instanceof AsMap_callable) {
				if (debug)
					System.err.println(" linking AsMap_callable/call " + rec);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_call", MethodType.methodType(Object.class, Object.class, Object.class));
				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));

			}
		} else if (linkRequest.getCallSiteDescriptor()
			.getOperation().toString().startsWith("GET:METHOD|PROPERTY|ELEMENT:")) {

			// this is a method lookup, it's possible we'd want to do something different here, for now, it's exactly the same

			Object rec = linkRequest.getReceiver();
			String propertyName = linkRequest.getCallSiteDescriptor().getOperation().toString().replace("GET:METHOD|PROPERTY|ELEMENT:", "");

			if (rec instanceof AsMap && ((AsMap) rec).asMap_isProperty(propertyName)) {

				if (debug)
					System.err.println(" linking AsMap.java/get 1" + rec + " admits to property " + propertyName);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_get", MethodType.methodType(Object.class, String.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor()
			.getOperation().toString().startsWith("GET:METHOD:apply")) {

			// this is a method lookup, it's possible we'd want to do something different here, for now, it's exactly the same

			Object rec = linkRequest.getReceiver();
			String propertyName = linkRequest.getCallSiteDescriptor().getOperation().toString().replace("GET:METHOD:apply", "");

			if (rec instanceof AsMap && ((AsMap) rec).asMap_isProperty(propertyName)) {

				if (debug)
					System.err.println(" linking AsMap.java/get 1" + rec + " admits to property " + propertyName);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(rec.getClass(), "asMap_get", MethodType.methodType(Object.class, String.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}
		} else if (linkRequest.getCallSiteDescriptor().getOperation()
			.toString().startsWith("SET:PROPERTY|ELEMENT")) {
			Object rec = linkRequest.getReceiver();
			String propertyName = linkRequest.getCallSiteDescriptor().getOperation().toString().replace("SET:PROPERTY|ELEMENT:", "");

			if (rec instanceof AsMap && ((AsMap) rec).asMap_isProperty(propertyName)) {

				if (debug)
					System.err.println(" linking AsMap.java/set " + rec + " " + propertyName);
				MethodHandle get = MethodHandles.lookup()
					.findVirtual(implementingClassFor(rec.getClass()), "asMap_set", MethodType.methodType(Object.class, String.class, Object.class));

				get = MethodHandles.insertArguments(get, 1, propertyName);

				return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
			}

		} else if (linkRequest.getCallSiteDescriptor().getOperation()
			.toString().startsWith("SET:ELEMENT|PROPERTY")) {
			Object rec = linkRequest.getReceiver();

			if (linkRequest.getArguments().length == 3) {

				if (rec instanceof AsMap) {

					if (debug)
						System.err.println(" linking AsMap.java/setElement " + rec);
					MethodHandle get = MethodHandles.lookup()
						.findVirtual(implementingClassFor(rec.getClass()), "asMap_setElement", MethodType.methodType(Object.class, Object.class, Object.class));

					return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
				}
			} else {
				String[] p = linkRequest.getCallSiteDescriptor().getOperation().toString().split(":");
				String propertyName = p[p.length - 1];


				if (rec instanceof AsMap) {

					if (debug)
						System.err.println(" linking AsMap.java/setElement " + rec);
					MethodHandle get = MethodHandles.lookup()
						.findVirtual(implementingClassFor(rec.getClass()), "asMap_setElement", MethodType.methodType(Object.class, Object.class, Object.class));

					get = MethodHandles.insertArguments(get, 1, propertyName);

					return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));
				}
			}
		} else if (linkRequest.getCallSiteDescriptor().getOperation()
			.toString().startsWith("GET:ELEMENT|PROPERTY|METHOD")) {
			Object rec = linkRequest.getReceiver();

			if (linkRequest.getArguments().length == 2) {

				if (linkRequest.getArguments()[1].getClass().isPrimitive()) {

					if (rec instanceof AsMap) {
						if (debug)
							System.err.println(" linking AsMap.java/property get " + rec);
						MethodHandle get = MethodHandles.lookup()
							.findVirtual(rec.getClass(), "asMap_getElement", MethodType.methodType(Object.class, Integer.TYPE));

						return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));

					}
				} else {
					if (rec instanceof AsMap) {
						if (debug)
							System.err.println(" linking AsMap.java/property get " + rec);
						MethodHandle get = MethodHandles.lookup()
							.findVirtual(rec.getClass(), "asMap_getElement", MethodType.methodType(Object.class, Object.class));


						return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));

					}

				}
			} else {
				String[] p = linkRequest.getCallSiteDescriptor().getOperation().toString().split(":");
				String propertyName = p[p.length - 1];

				if (rec instanceof AsMap) {
					if (debug)
						System.err.println(" linking AsMap.java/property get " + rec);
					MethodHandle get = MethodHandles.lookup()
						.findVirtual(rec.getClass(), "asMap_getElement", MethodType.methodType(Object.class, Object.class));

					get = MethodHandles.insertArguments(get, 1, propertyName);

					return new GuardedInvocation(get, Guards.isInstance(rec.getClass(), MethodType.methodType(Boolean.TYPE, Object.class)));

				}
			}
		} else {
			if (debug) {
				System.out.println(" don't know what to do with that :" + linkRequest.getCallSiteDescriptor().getOperation()
					.toString()+" "+linkRequest.getReceiver());
			}
		}
		return null;
	}

	private Class<?> implementingClassFor(Class<? extends Object> aClass) {
		if (aClass == null) return null;

		Class<?>[] ii = aClass.getInterfaces();
		for (Class c : ii)
			if (c == AsMap.class) return aClass;

		return implementingClassFor(aClass.getSuperclass());
	}

//	@Override
//	public GuardedInvocation convertToType(Class<?> sourceType, Class<?> targetType, Supplier<MethodHandles.Lookup> lookupSupplier) throws Exception {
//		if (debug) System.err.println("LINKER convertToType :" + sourceType + " " + targetType);
//		return null;
//	}

}
