package field.utility;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by marc on 3/18/14.
 */
public abstract class Lazy<T> {

	private final int estimatedSize;

	public Lazy(int estimatedSize) {
		this.estimatedSize = estimatedSize;
	}

	public Lazy() {
		this.estimatedSize = 10;
	}

	public Lazy<T> reset()
	{
		future = initialize();
		return this;
	}


	public Stream<T> stream() {
		return StreamSupport.stream(Spliterators.spliterator(internal, estimatedSize, Spliterator.IMMUTABLE), false);
	}

	protected abstract Iterator<T> initialize();

	Iterator<T> future = null;

	private Iterator<T> internal =new Iterator() {
		@Override public boolean hasNext () {
			if (future == null) future = pull();
			if (future == null) return false;
			while (!future.hasNext())
			{
				future = pull();
				if (future==null) return false;
			}
			return true;
		}

		@Override public T next () {
			if (!hasNext()) return null;
			return future.next();
		}
	};

	protected abstract Iterator<T> pull();

}
