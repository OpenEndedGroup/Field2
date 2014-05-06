package field.utility;

import java.util.Map;
import java.util.Optional;
import java.util.function.*;

/**
 * Created by marc on 3/18/14.
 */
public class Curry {

	public interface Function3<A, B, C, D>
	{
		public D apply(A a, B b, C c);
	}

	public interface Function4<A, B, C, D, E>
	{
		public E apply(A a, B b, C c, D d);
	}

	static public <A, B> Supplier<B> partial(Function<A, B> f, A a)
	{
		return () -> f.apply(a);
	}

	static public <A, B, C> Function<B,C> partial(BiFunction<A, B, C> f, A a)
	{
		return (b) -> f.apply(a, b);
	}

	static public <A, B, C> Supplier<C> partial(BiFunction<A, B, C> f, A a, B b)
	{
		return () -> f.apply(a, b);
	}

	static public <A, B, C, D> BiFunction<B,C,D> partial(Function3<A, B, C, D> f, A a)
	{
		return (b,c) -> f.apply(a, b, c);
	}

	static public <A, B, C, D> Function<C,D> partial(Function3<A, B, C, D> f, A a, B b)
	{
		return (c) -> f.apply(a, b, c);
	}

	static public <A, B, C, D> Supplier<D> partial(Function3<A, B, C, D> f, A a, B b, C c)
	{
		return () -> f.apply(a, b, c);
	}

	static public <A, B, C, D,E> Function3<B,C,D,E> partial(Function4<A, B, C, D, E> f, A a)
	{
		return (b,c,d) -> f.apply(a, b, c, d);
	}

	static public <A, B, C, D,E> BiFunction<C,D,E> partial(Function4<A, B, C, D, E> f, A a, B b)
	{
		return (c,d) -> f.apply(a, b, c, d);
	}

	static public <A, B, C, D,E> Function<D,E> partial(Function4<A, B, C, D, E> f, A a, B b, C c)
	{
		return (d) -> f.apply(a, b, c, d);
	}

	static public <A, B, C, D,E> Supplier<E> partial(Function4<A, B, C, D, E> f, A a, B b, C c, D d)
	{
		return () -> f.apply(a, b, c, d);
	}

	static public <A> Function<A, Void> ignore(Consumer<A> a)
	{
		return (x) -> {a.accept(x); return null;};
	}

	static public <A,B> BiFunction<A, B, Void> ignore(BiConsumer<A, B> a)
	{
		return (x,y) -> {a.accept(x,y); return null;};
	}

	static public <K, V> Optional<V> getOptional(Map<K, V> m, K k)
	{
		if (m.containsKey(k)) return Optional.of(m.get(k));
		return Optional.empty();
	}

	static public <T> Function<T, T> log(String prefix)
	{
		return x -> {System.out.println(prefix+" "+x); return x;};
	}

}
