package fieldnashorn.babel;

import field.utility.Pair;

import java.util.function.Function;

/**
 * Created by marc on 6/13/15.
 */
public interface SourceTransformer {
	Pair<String, Function<Integer, Integer>> transform(String c, boolean fragment) throws TranslationFailedException;

	default void incrementalUpdate(String now)
	{
	}

	class TranslationFailedException extends Exception
	{
		public TranslationFailedException(String message){super(message);}
	}
}
