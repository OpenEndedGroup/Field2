package field.utility;

import fieldbox.io.IO;

public interface OverloadedMath {

	Dict.Prop<Boolean> withOverloading = new Dict.Prop<>("withOverloading")
		.toCanon().type()
		.doc("set `_.withOverloading=true` to turn on operator overloading (specifically for adding Vectors and transforming lines and the like")
		.set(IO.persistent, true);
	Dict.Prop<Boolean> withLiveNumbers = new Dict.Prop<>("withLiveNumbers")
		.toCanon().type()
		.doc("set `_.withLiveNumbers=true` to turn on live number updating (numbers changed in the text editor are automatically sent to running code)")
		.set(IO.persistent, true);

	Object __sub__(Object b);

	Object __rsub__(Object b);

	Object __add__(Object b);

	Object __radd__(Object b);

	Object __mul__(Object b);

	Object __rmul__(Object b);
}
