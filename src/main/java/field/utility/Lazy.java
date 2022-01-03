package field.utility;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(internal, Spliterator.IMMUTABLE | Spliterator.ORDERED), false);
	}

	protected abstract Iterator<T> initialize();

	Iterator<T> future = null;

	private Iterator internal =new Iterator() {
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
			if (future==null) {
				if (!hasNext()) return null;
				T r = future.next();
//				System.out.println(" next returning 1:"+r);
				return r;
			}
			if (!future.hasNext())
			{
				if (!hasNext()) return null;
				T r = future.next();
//				System.out.println(" next returning 2:"+r);
				return r;
			}
			T r = future.next();
//			System.out.println(" next returning 3:"+r);
			return r;
		}
	};

	protected abstract Iterator<T> pull();

}
