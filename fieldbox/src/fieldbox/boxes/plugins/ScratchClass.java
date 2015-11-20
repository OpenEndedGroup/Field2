package fieldbox.boxes.plugins;

/**
 * Created by marc on 7/25/14.
 */
public class ScratchClass {

	public class ClassOne {
		public String peach = "pear";

		public String banana() {
			return "banana";
		}
	}

	public ClassOne c2 = new ClassOne() {
		public String yetMoreMember = "yet more";

		public String banana() {
			return "banana, overridden in c2";
		}

		public String pear() {
			return "pear";
		}
	};


	public class ClassTwo extends ClassOne {
		public String yetMoreMember = "yet more";

		public String banana() {
			return "banana, overridden in c2";
		}

		public String pear() {
			return "pear";
		}
	}

	public ClassOne c1 = new ClassOne();
	public ClassTwo c22 = new ClassTwo();
}

