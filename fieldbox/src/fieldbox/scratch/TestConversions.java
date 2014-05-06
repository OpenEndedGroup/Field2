package fieldbox.scratch;

import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Conversions;
import field.utility.Pair;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by marc on 3/26/14.
 */
public class TestConversions {

	static public void main(String[] s)
	{

		Conversions.provideConversion(1, new Function<Supplier<Vec3>, Vec3>(){
			@Override
			public Vec3 apply(Supplier<Vec3> x) {
				return x.get();
			}
		}, "s_vec3 to vec3");

		Conversions.provideConversion(1, new Function<Vec3, Vec2>(){
			@Override
			public Vec2 apply(Vec3 x) {
				return new Vec2(x.x, x.y);
			}
		}, "vec3 to vec2");
//		Conversions.provideConversion(1, new Function<Vec3, Vec4>(){
//			@Override
//			public Vec4 apply(Vec3 x) {
//				return new Vec4(x.x, x.y, x.z, 0);
//			}
//		}, "vec3 to vec4");
		Conversions.provideConversion(1, new Function<Vec4, Vec3>(){
			@Override
			public Vec3 apply(Vec4 x) {
				return new Vec3(x.x, x.y, x.z);
			}
		}, "vec4 to vec3");
		Conversions.provideConversion(1, new Function<Vec4, Vec2>(){
			@Override
			public Vec2 apply(Vec4 x) {
				return new Vec2(x.x, x.y);
			}
		}, "vec4 to vec2");
		Conversions.provideConversion(1, new Function<Vec2, Vec3>(){
			@Override
			public Vec3 apply(Vec2 x) {
				return new Vec3(x.x, x.y,0);
			}
		}, "vec2 to vec3");
		Conversions.provideConversion(1, new Function<Vec2, Vec4>(){
			@Override
			public Vec4 apply(Vec2 x) {
				return new Vec4(x.x, x.y, 0,0);
			}
		}, "vec2 to vec4");
		Conversions.provideConversion(1, new Function<Vec4, String>(){
			@Override
			public String apply(Vec4 x) {
				return x.toString();
			}
		}, "vec4 to String");


		Conversions.provideConversion(1, (Function<Vec4, String>) x -> "", "vec4 to String");

		Supplier<Vec3> vv = new Supplier<Vec3>(){
			@Override
			public Vec3 get() {
				return new Vec3(10,20,30);
			}
		};

		List<Pair<List<Class>,Conversions.Conversion>> c = Conversions.getConversion(vv, Collections.singletonList(String.class));
		System.out.println(""+Conversions.runConversion(c, vv));

		c = Conversions.getConversion((Supplier<Vec3>)() -> new Vec3(1,2,3), Collections.singletonList(String.class));
		System.out.println(""+Conversions.runConversion(c, vv));
	}

}
