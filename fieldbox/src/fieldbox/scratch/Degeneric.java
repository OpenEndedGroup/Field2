package fieldbox.scratch;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.utility.Conversions;
import field.utility.Dict;
import field.utility.Dijkstra;
import field.utility.Pair;
import sun.reflect.CallerSensitive;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by marc on 3/26/14.
 */

public class Degeneric {


	static public final Dict.Prop<Vec3> target = new Dict.Prop<Vec3>("target").type();
	static public final Dict.Prop<Function<Vec3, Vec2>> target2 = new Dict.Prop<>("target").type();

	static SetMultimap<Node, Edge> inputs = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();
	static SetMultimap<Node, Edge> outputs = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();

	static public class Node
	{
		String a;
		public Node(String a)
		{
			this.a = a;
		}

		@Override
		public String toString() {
			return a;
		}
	}

	static public class Edge
	{
		private final Node output;
		float length;
		String a;
		public Edge(String a, float length, Node n1, Node n2)
		{
			this.a = a;
			this.length = length;

			inputs.put(n1, this);
			outputs.put(n2, this);
			output = n2;
		}

		public String toString()
		{
			return a+":"+length;
		}
	}

	static public void main(String[] aadf) {

		Set<List<Class>> c = Conversions.genericAlternativesFor(new Function<Supplier<Vec3>, Vec2>() {
			@Override
			public Vec2 apply(Supplier<Vec3> vec3) {
				return null;
			}
		}.getClass());

		System.out.println(" alternatives :"+c);
		for(List<Class> cc : c)
		{
			System.out.println("   "+cc);
		}

	}
	static public void main0(String[] aadf) {

		Function<Supplier<Vec3>, Vec2> banana = new Function<Supplier<Vec3>, Vec2>()
		{
			@Override
			public Vec2 apply(Supplier<Vec3> vec3) {
				return null;
			}
		};

		System.out.println(" banana "+Conversions.function(banana));

		Node a = new Node("a");
		Node b = new Node("b");
		Node c = new Node("c");
		Node d = new Node("d");
		Node e = new Node("e");
		Node f = new Node("f");

		new Edge("ab", 1, a, b);
		new Edge("bc", 1, b, c);
		new Edge("cd", 1, c, d);
		new Edge("de", 1, d, e);

		new Edge("be", 1, b, e);

		Dijkstra<Node, Edge> di = new Dijkstra<>(x -> x.length, x -> x.output, x -> inputs.get(x));
		di.computePaths(a);
		System.out.println(di.getShortestPathTo(f));




	}

}
