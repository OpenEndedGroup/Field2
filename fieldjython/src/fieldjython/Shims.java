package fieldjython;

import fieldlinker.Linker;
import org.python.core.*;

/**
 * Extensions to the Jython/Java interface that help Jython talk about Field and Field's graphics
 */
public class Shims {

	static public void init()
	{
		{
			final PyObject old = PyObject.TYPE.__getattr__("__getattr__");
			PyBuiltinMethod meth = new PyBuiltinMethodNarrow("__getattr__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return old.__call__(self, name);
					} catch (PyException pye) {
						if (!pye.match(Py.AttributeError)) {
							throw pye;
						}
					}
					Linker.AsMap inst = Py.tojava(self, Linker.AsMap.class);
					return Py.java2py(inst.asMap_get(Py.tojava(name, String.class)));
				}
			};
			PyType.fromClass(Linker.AsMap.class)
			      .addMethod(meth);
		}
		{
			final PyObject old = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethod meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject name,PyObject val) {
					try {
						return old.__call__(self, name, val);
					} catch (PyException pye) {
						if (!pye.match(Py.AttributeError)) {
							throw pye;
						}
					}
					Linker.AsMap inst = Py.tojava(self, Linker.AsMap.class);
					return Py.java2py(inst.asMap_set(Py.tojava(name, String.class), Py.tojava(val, Object.class)));
				}
			};
			PyType.fromClass(Linker.AsMap.class)
			      .addMethod(meth);
		}
		{
			final PyObject old = PyObject.TYPE.__getattr__("__call__");
			PyBuiltinMethod meth = new PyBuiltinMethodNarrow("__call__", 2) {
				@Override
				public PyObject __call__(PyObject a1,PyObject a2) {
					try {
						return old.__call__(self, a1, a2);
					} catch (PyException pye) {
						if (!pye.match(Py.AttributeError)) {
							throw pye;
						}
					}
					Linker.AsMap inst = Py.tojava(self, Linker.AsMap.class);
					return Py.java2py(inst.asMap_call(Py.tojava(a1, Object.class), Py.tojava(a2, Object.class)));
				}
			};
			PyType.fromClass(Linker.AsMap.class)
			      .addMethod(meth);
		}
		//TODO: new?
	}
}
