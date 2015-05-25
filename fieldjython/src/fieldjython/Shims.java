package fieldjython;

import fieldlinker.Linker;
import org.python.core.Py;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyObject;
import org.python.core.PyType;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Extensions to the Jython/Java interface that help Jython talk about Field and Field's graphics
 */
public class Shims {

	static public void init() {
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject key) {
					Linker.AsMap program = Py.tojava(self, Linker.AsMap.class);
					return Py.java2py(program.asMap_get(key.asString()));
				}
			};
			PyType.fromClass(Linker.AsMap.class)
			      .addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject key, PyObject v) {
					Linker.AsMap program = Py.tojava(self, Linker.AsMap.class);
					return Py.java2py(program.asMap_set(key.asString(), Py.tojava(v, Object.class)));
				}
			};
			PyType.fromClass(Linker.AsMap.class)
			      .addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__call__", 0) {
				@Override
				public PyObject __call__() {
					Supplier program = Py.tojava(self, Supplier.class);
					return Py.java2py(program.get());
				}
			};
			PyType.fromClass(Supplier.class)
			      .addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__call__", 1) {
				@Override
				public PyObject __call__(PyObject v) {
					Function program = Py.tojava(self, Function.class);
					return Py.java2py(program.apply(Py.tojava(v, Object.class)));
				}
			};
			PyType.fromClass(Function.class)
			      .addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__call__", 2) {
				@Override
				public PyObject __call__(PyObject v, PyObject v2) {
					BiFunction program = Py.tojava(self, BiFunction.class);
					return Py.java2py(program.apply(Py.tojava(v, Object.class), Py.tojava(v2, Object.class)));
				}
			};
			PyType.fromClass(BiFunction.class)
			      .addMethod(meth);
		}

	}
}
