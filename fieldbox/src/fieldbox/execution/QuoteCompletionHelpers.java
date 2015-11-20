package fieldbox.execution;

import field.utility.Dict;
import fieldbox.boxes.Box;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by marc on 9/2/15.
 */
public class QuoteCompletionHelpers {

	public static <T> T wrap(T f, Class<? super T> also, Function<String, List<Completion>> completor) {
		return (T) Proxy.newProxyInstance(Thread.currentThread()
							.getContextClassLoader(), new Class[]{also, HandlesQuoteCompletion.class}, (proxy, method, args) -> {
			if (method.getName()
				  .equals("getQuoteCompletionsFor")) return completor.apply((String) args[0]);
			return method.invoke(f, args);

		});
	}

	public static <T> void wrap(Dict d, Dict.Prop<T> p, Class<? super T> base, Function<String, List<Completion>> completor) {
		d.put(p, wrap(d.get(p), base, completor));
	}


	/*
	 * you might ask: surely this isn't needed in Java 8 when we can just write x -> f.apply(x, y.get()) ?
	 *
	 * Sure, but that will curry a BiFunction into a Function, but the resulting Function doesn't implement any of the other interfaces the original BiFunction implemented
	 *
	 * FP, meet OOP.
	 */
	public static <T, R, U> Function<T, R> curry(BiFunction<U, T, R> f, Supplier<U> u) {
		Set<Class> all = new LinkedHashSet<>();
		Class fc = f.getClass();

		Method[] fm = Function.class.getDeclaredMethods();
		Set<String> methodNames = new LinkedHashSet<>();
		for(Method mm : fm) methodNames.add(mm.getName());

		while (fc != null) {
			Class[] inter = fc.getInterfaces();
			for (Class c : inter)
				if (!checkOverlapNames(c, methodNames))
					all.add(c);
			fc = fc.getSuperclass();
		}

		all.add(Function.class);

		Class[] a2 = new Class[all.size()];
		a2 = all.toArray(a2);

		return (Function<T, R>) Proxy.newProxyInstance(Thread.currentThread()
								     .getContextClassLoader(), a2, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName()
					  .equals("apply")) return f.apply(u.get(), (T) args[0]);
				return method.invoke(f, args);
			}
		});
	}

	private static boolean checkOverlapNames(Class c, Set<String> other) {
		Method[] fm = c.getDeclaredMethods();
		Set<String> methodNames = new LinkedHashSet<>();
		for(Method mm : fm) methodNames.add(mm.getName());

		fm = c.getMethods();
		for(Method mm : fm) methodNames.add(mm.getName());

		other.retainAll(methodNames);

		return other.size()>0;
	}

	public static <T, R> Supplier<R> curry(Function<T, R> f, Supplier<T> u) {
		Set<Class> all = new LinkedHashSet<>();
		Class fc = f.getClass();
		while (fc != null) {
			Class[] inter = fc.getInterfaces();
			for (Class c : inter)
				if (!c.equals(Function.class)) all.add(c);
			fc = fc.getSuperclass();
		}

		all.add(Supplier.class);

		Class[] a2 = new Class[all.size()];
		a2 = all.toArray(a2);

		return (Supplier<R>) Proxy.newProxyInstance(Thread.currentThread()
								     .getContextClassLoader(), a2, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName()
					  .equals("get")) return f.apply(u.get());
				return method.invoke(f);
			}
		});
	}

	/*
	Special case, since there's no super-interface called TriFunction
	 */
	public static <T1, T2, R> BiFunction<T1, T2, R> curry(Box.TriFunctionOfBoxAnd<T1, T2, R> f, Supplier<Box> u) {
		Set<Class> all = new LinkedHashSet<>();
		Class fc = f.getClass();
		while (fc != null) {
			Class[] inter = fc.getInterfaces();
			for (Class c : inter)
				if (!c.equals(Box.TriFunctionOfBoxAnd.class)) all.add(c);
			fc = fc.getSuperclass();
		}

		all.add(BiFunction.class);

		Class[] a2 = new Class[all.size()];
		a2 = all.toArray(a2);

		return (BiFunction<T1, T2, R>) Proxy.newProxyInstance(Thread.currentThread()
								     .getContextClassLoader(), a2, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName()
					  .equals("apply")) return f.apply(u.get(), (T1) args[0], (T2)args[1]);
				return method.invoke(f);
			}
		});
	}


}
