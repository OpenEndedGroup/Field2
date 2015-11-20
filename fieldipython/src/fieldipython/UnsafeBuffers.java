package fieldipython;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Utilties to go from raw 'c' pointers to Direct ByteBuffers. Code, that is, you'll go to hell for.
 */
public class UnsafeBuffers {


	static public ByteBuffer direct(long pointer, int size) throws NoSuchFieldException, IllegalAccessException {
		// if our fake bytebuffers ever get GC'ed their cleaner will free this memory, not the actual pointer
		ByteBuffer ret = ByteBuffer.allocateDirect(0);

		setLong(ret, Buffer.class, "address", pointer);
		setInt(ret, Buffer.class, "limit", size);
		setInt(ret, Buffer.class, "capacity", size);

		return ret;
	}

	static private void setLong(Object ret, Class<Buffer> inside, String field, long value) throws NoSuchFieldException, IllegalAccessException {

		Field f = inside.getDeclaredField(field);
		f.setAccessible(true);
		f.setLong(ret, value);

	}
	static private void setInt(Object ret, Class<Buffer> inside, String field, int value) throws NoSuchFieldException, IllegalAccessException {

		Field f = inside.getDeclaredField(field);
		f.setAccessible(true);
		f.setInt(ret, value);
	}

}
