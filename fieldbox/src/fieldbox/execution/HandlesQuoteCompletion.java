package fieldbox.execution;

import java.util.List;

/**
 * Created by marc on 9/2/15.
 */
public interface HandlesQuoteCompletion {
	public List<Completion> getQuoteCompletionsFor(String prefix);
}
