package fieldipython;

import field.app.RunLoop;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldlinker.Linker;
import fieldnashorn.annotations.HiddenInAutocomplete;
import org.jpy.PyLib;
import org.jpy.PyModule;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Created by marc on 5/17/15.
 */
public class JpySystem implements Linker.AsMap {

	private final Thread thread;
	private final Box readFrom;
	private final Box writeTo;
	public HashMap<String, Buffer> named = new HashMap<>();

	public JpySystem(Box readFrom, Box writeTo) throws ExecutionException, InterruptedException {
		this.readFrom = readFrom;
		this.writeTo = writeTo;

		String foundJpyLib = Thread.currentThread()
				    .getContextClassLoader()
				    .getResource("jpy.so")
				    .getFile();
		System.setProperty("jpy.jpyLib", foundJpyLib);
		String foundJdlLib = Thread.currentThread()
					   .getContextClassLoader()
					   .getResource("jdl.so")
					   .getFile();
		System.setProperty("jpy.jdlLib", foundJdlLib);

		Log.log("jpy", () -> " found libraries at :" + foundJdlLib + " / " + foundJpyLib);

		CompletableFuture f = new CompletableFuture();
		thread = new Thread(() -> {
			PyLib.startPython();
			PyLib.execScript("import IPython");
			PyLib.execScript("from traitlets.config.application import get_config");
			PyLib.execScript("c=get_config()");
			PyLib.execScript("c.Session.key=b''");
			PyLib.execScript("c.debug=True");

			PyModule mainModule = PyModule.importModule("__main__");
			mainModule.setAttribute("__field__", JpySystem.this);

			System.out.println(" -- init jpy/ipython -- ");
			f.complete(true);
			PyLib.execScript("IPython.start_kernel(user_ns=globals(), config=c)");
			System.out.println(" -- launched jpy/ipython  -- ");
		});
		thread.start();
		f.get();

		// start_kernel never returns, but we do need it to run
		Thread.sleep(1000);

		RunLoop.main.onExit(thread::stop);
	}


	@HiddenInAutocomplete
	public ByteBuffer name(String name, long ptr, int elements, String typedesc) {
		try {

			int len = Integer.parseInt("" + typedesc.charAt(typedesc.length() - 1));
			ByteBuffer res = UnsafeBuffers.direct(ptr, elements * len);
			res.order(typedesc.charAt(0) == '<' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

			Buffer o = null;
			switch (typedesc.charAt(1)) {
				case 'f':
					if (len == 8) o = res.asDoubleBuffer();
					else if (len == 4) o = res.asFloatBuffer();
					break;
				case 'i':
					if (len == 8) o = res.asLongBuffer();
					else if (len == 4) o = res.asIntBuffer();
					else if (len == 2) o = res.asShortBuffer();
					break;
			}

			if (o==null)
				throw new IllegalArgumentException(" can't parse __array_interface__ desc :"+typedesc);

			named.put(name, o);
			writeTo.asMap_set(name, o);
			return res;
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}


	@Override
	public boolean asMap_isProperty(String p) {
		return named.containsKey(p);
	}

	@Override
	public Object asMap_call(Object a, Object b) {
		return null;
	}

	@Override
	public Object asMap_get(String p) {

		Buffer q = named.get(p);
		if (q==null)
			return readFrom.asMap_get(p);
		else
			return q;
	}

	@Override
	public Object asMap_set(String p, Object o) {
		return writeTo.asMap_set(p, o);
	}

	@Override
	public Object asMap_new(Object a) {
		return null;
	}

	@Override
	public Object asMap_new(Object a, Object b) {
		return null;
	}

	@Override
	public Object asMap_getElement(int element) {
		return null;
	}

	@Override
	public Object asMap_setElement(int element, Object o) {
		return null;
	}

	@Override
	public boolean asMap_delete(Object o) {
		return false;
	}
}
