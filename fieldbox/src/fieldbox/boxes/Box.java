package fieldbox.boxes;

import field.utility.Dict;
import field.utility.Lazy;
import fieldbox.io.IO;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by marc on 3/18/14.
 */
public class Box {

	static public final Dict.Prop<String> name= new Dict.Prop<>("name").toCannon();

	public Set<Box> parents = new LinkedHashSet<Box>();
	public Set<Box> children = new LinkedHashSet<Box>();
	public Deque<Box> all = new ArrayDeque<>();

	public final Dict properties = new Dict();

	public Box()
	{
		properties.put(IO.id, UUID.randomUUID().toString());
	}

	public Box connect(Box b)
	{
		if (children.add(b))
			all.addLast(b);

		if (b.parents.add(this))
			b.all.addFirst(this);

		return this;
	}

	public Box disconnect(Box b)
	{
		children.remove(b);
		b.parents.remove(this);

		all.remove(b);
		b.all.remove(this);

		return this;
	}

	public Box disconnectFromAll()
	{
		for(Box b : new ArrayList<>(children))
			disconnect(b);
		for(Box b : new ArrayList<>(parents))
			b.disconnect(this);

		all.clear();

		return this;
	}

	public Set<Box> parents()
	{
		return Collections.unmodifiableSet(parents);
	}

	public Set<Box> children()
	{
		return Collections.unmodifiableSet(children);
	}

	protected Set<Box> _parents()
	{
		return parents;
	}

	protected Set<Box> _children()
	{
		return children;
	}

	protected Deque<Box> _all()
	{
		return all;
	}

	public <T> Stream<T> has(Dict.Prop<T> find, Function<Box, Collection<Box>> direction)
	{
		return breadthFirst(direction).map(x -> x.properties.get(find)).filter(x -> x!=null);
	}

	public <T> Stream<Box> whereHas(Dict.Prop<T> find, Function<Box, Collection<Box>> direction)
	{
		return breadthFirst(direction).filter(x -> x.properties.has(find));
	}

	public <T> Stream<T> find(Dict.Prop<T> find, Function<Box, Collection<Box>> direction)
	{
		return breadthFirst(direction).map(x -> x.properties.get(find)).filter(x -> x!=null);
	}

	public <T> Optional<T> first(Dict.Prop<T> find, Function<Box, Collection<Box>> direction)
	{
		if (properties.has(find)) return Optional.of(properties.get(find));
		return breadthFirst(direction).map(x -> x.properties.get(find)).filter(x -> x!=null).findFirst();
	}

	public <T> Optional<T> next(Dict.Prop<T> find, Function<Box, Collection<Box>> direction)
	{
		if (properties.has(find)) return Optional.of(properties.get(find));
		return breadthFirst(direction).map(x -> x.properties.get(find)).filter(x -> x!=null).skip(1).findFirst();
	}

	public <T> Optional<T> first(Dict.Prop<T> find)
	{
		return first(find, Box::_parents);
	}

	public <T> Optional<T> next(Dict.Prop<T> find)
	{
		return next(find, Box::_parents);
	}

	public <T> Optional<Box> where(Dict.Prop<T> find)
	{
		return whereHas(find, upwards()).findFirst();
	}

	public <G, T> Stream<T> call(Function<G, T> f, Class<G> guard, Function<Box, Collection<Box>> direction)
	{
		return breadthFirst(direction).filter( x -> guard.isInstance(x)).map( x -> f.apply((G)x));
	}

	public <G, T> Stream<T> call(Function<G, T> f, Class<G> guard)
	{
		return call(f, guard, upwards());
	}

	/**
	 * returns direction for upwards (parents) (e.g breadthFirst(Box::parents))
	 */
	public Function<Box, Collection<Box>>  upwards()
	{
		return Box::_parents;
	}

	/**
	 * returns direction for downwards (children) (e.g breadthFirst(Box::children))
	 */
	public Function<Box, Collection<Box>>  downwards()
	{
		return Box::_children;
	}


	/**
	 * returns direction for downwards and upwards (children and then parents) (e.g breadthFirst(Box::children))
	 */
	public Function<Box, Collection<Box>>  both()
	{
		return Box::_all;
	}

	/**
	 * returns breadth first Stream given a direction function.
	 */
	public Stream<Box> breadthFirst(Function<Box, Collection<Box>> map)
	{
		return new Lazy<Box>()
		{
			LinkedHashSet<Box> ret = null;
			Set<Box> thisLevel = null;

			protected Iterator<Box> initialize()
			{
				ret = new LinkedHashSet<>();
				ret.add(Box.this);
				thisLevel = ret;
				return ret.iterator();
			}

			@Override
			protected Iterator<Box> pull() {
				if (thisLevel.size()==0) return null;
				Set<Box> nextLevel = new LinkedHashSet<>();
				for(Box b : thisLevel)
					nextLevel.addAll(map.apply(b));
				nextLevel.removeAll(ret);
				ret.addAll(nextLevel);
				thisLevel = nextLevel;
				return thisLevel.iterator();
			}
		}.reset().stream();
	}

	@Override
	public String toString() {
		String name = properties.get(Box.name);
		if (name==null)
			return "bx<"+this.getClass().getSimpleName()+">";
		else return "bx<"+name+">";
	}
}
