package field.utility;

import fieldbox.boxes.Box;
import fieldbox.io.IO;

import java.util.function.BiFunction;

public interface OverloadedMath {

	Dict.Prop<Boolean> withOverloading = new Dict.Prop<>("withOverloading")
		.toCanon().type()
		.doc("set `_.withOverloading=true` to turn on operator overloading (specifically for adding Vectors and transforming lines and the like")
		.set(IO.persistent, true);
	Dict.Prop<Boolean> withLiveNumbers = new Dict.Prop<>("withLiveNumbers")
		.toCanon().type()
		.doc("set `_.withLiveNumbers=true` to turn on live number updating (numbers changed in the text editor are automatically sent to running code)")
		.set(IO.persistent, true);

	Dict.Prop<Boolean> withFunctionRewriting = new Dict.Prop<>("withFunctionRewriting")
		.toCanon().type()
		.doc("set `_.withFunctionRewriting=true` to turn on the option to AST-rewrite identifiers in function invocations. If you don't know what that means, don't turn it on")
		.set(IO.persistent, true);

	Dict.Prop<BiFunction<Box, String, String>> functionRewriteTrap = new Dict.Prop<>("withFunctionRewriting")
		.toCanon().type()
		.doc("set `_.functionRewriteTrap will be called (if _.withFunctionRewriting==true) with (_, identifier) and return a new string that will replace the function identifier (or null) " +
			"to not replace anything")
		.set(IO.persistent, true);



	Object __sub__(Object b);

	Object __rsub__(Object b);

	Object __add__(Object b);

	Object __radd__(Object b);

	Object __mul__(Object b);

	Object __rmul__(Object b);

	Object __div__(Object b);

	Object __rdiv__(Object b);

	Object __xor__(Object b);

	Object __rxor__(Object b);
}
