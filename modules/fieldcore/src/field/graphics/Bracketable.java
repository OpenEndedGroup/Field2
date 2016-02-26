package field.graphics;

/**
 * Codifies a general pattern for a resource in the Field graphics system: somethign that can be opened and the later closed. For example a
 * MeshBuilder is first open(), then some geometry is written to it, and then it's finally sealed again with a matching close(). Successive calls to
 * open() are fine, it's the last and final call to close() that matters.
 *
 * close() is defined in AutoCloseable (which is part of Java8 and allows Bracketable's to be used inside try-with-resources blocks.
 */
public interface Bracketable extends AutoCloseable {
	Bracketable open();
}
