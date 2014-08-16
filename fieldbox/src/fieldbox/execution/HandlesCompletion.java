package fieldbox.execution;

import java.util.List;

/**
* Created by marc on 8/16/14.
*/
public interface HandlesCompletion {
	public List<Execution.Completion> getCompletionsFor(String prefix);
}
