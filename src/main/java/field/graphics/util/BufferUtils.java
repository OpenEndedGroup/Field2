package field.graphics.util;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class BufferUtils {

	public static ByteBuffer asByteBuffer(FloatBuffer floatBuffer) throws NoSuchFieldException, IllegalAccessException {

		if (floatBuffer.getClass().getName().endsWith("DirectFloatBufferU")) {
			ByteBuffer ret = ByteBuffer.allocateDirect(0);

			setLong(ret, Buffer.class, "address", getLong(floatBuffer, Buffer.class, "address"));
			setInt(ret, Buffer.class, "limit", getInt(floatBuffer, Buffer.class, "limit")*Float.BYTES);
			setInt(ret, Buffer.class, "capacity", getInt(floatBuffer, Buffer.class, "capacity")*Float.BYTES);

			return ret;
		} else if (floatBuffer.getClass().getName().endsWith("DirectFloatBufferS")) {
			ByteBuffer ret = ByteBuffer.allocateDirect(0);

			setLong(ret, Buffer.class, "address", getLong(floatBuffer, Buffer.class, "address"));
			setInt(ret, Buffer.class, "limit", getInt(floatBuffer, Buffer.class, "limit")*Float.BYTES);
			setInt(ret, Buffer.class, "capacity", getInt(floatBuffer, Buffer.class, "capacity")*Float.BYTES);

			return ret;
		} throw new IllegalArgumentException("Unsupported implementing class " + floatBuffer.getClass()
													 .getName());
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

	static private int getInt(Object ret, Class<Buffer> inside, String field) throws NoSuchFieldException, IllegalAccessException {
		Field f = inside.getDeclaredField(field);
		f.setAccessible(true);
		return f.getInt(ret);
	}
	static private long getLong(Object ret, Class<Buffer> inside, String field) throws NoSuchFieldException, IllegalAccessException {
		Field f = inside.getDeclaredField(field);
		f.setAccessible(true);
		return f.getLong(ret);
	}
}