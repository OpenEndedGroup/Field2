package fieldbox.execution;

import java.util.UUID;

/**
 * helper class for completion results
 */
public class Completion {
	public int start, end;
	public String replacewith;
	public String info;
	public String header;

	public float rank = 0;

	public String uuid = UUID.randomUUID().toString();
	public int type; // used to differentiate between field and method

	public Completion(int start, int end, String replacewith, String info) {
		this.start = start;
		this.end = end;
		this.replacewith = replacewith;
		this.info = info;
//		rank -= replacewith.length();
	}

	@Override
	public String toString() {
		return "comp<" + replacewith + " | " + info + "> @ "+rank;
	}
}
