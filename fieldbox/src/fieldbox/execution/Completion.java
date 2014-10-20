package fieldbox.execution;

/**
 * helper class for completion results
 */
public class Completion {
	public int start, end;
	public String replacewith;
	public String info;
	public String header;

	public Completion(int start, int end, String replacewith, String info) {
		this.start = start;
		this.end = end;
		this.replacewith = replacewith;
		this.info = info;
	}

	@Override
	public String toString() {
		return "comp<" + replacewith + " | " + info + ">";
	}
}
