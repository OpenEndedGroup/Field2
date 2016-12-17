package fielded;

import field.utility.Quad;

import java.math.BigDecimal;

/**
 * For quickly tokenizing around the current position to manipulate numbers
 */
public class Numbers {

	static public Quad<Integer, Integer, String, Double> extractNumberAt(String code, int cursor) {
		code = code+" ";
		if (!Character.isDigit(code.charAt(cursor)) && Character.isDigit(code.charAt(cursor-1)))
			cursor--;
		int first = cursor;
		int second = cursor;


		String a = "" + code.charAt(cursor);

		for (int i = cursor + 1; i < code.length(); i++) {
			if (Character.isWhitespace(code.charAt(i))) break;
			if (!Character.isDigit(code.charAt(i)) && code.charAt(i)!='.') break;

			try {
				double z = Double.parseDouble(a + code.charAt(i));
			} catch (NumberFormatException e) {
				break;
			}

			a += code.charAt(i);
			second = i + 1;

		}
		for (int i = cursor - 1; i >= 1; i--) {
			if (Character.isWhitespace(code.charAt(i))) break;
			if (!Character.isDigit(code.charAt(i)) && code.charAt(i)!='.' && code.charAt(i)!='-') break;
			first = i;
			try {
				double z = Double.parseDouble(code.charAt(i) + a);
			} catch (NumberFormatException e) {
				break;
			}
			a = code.charAt(i) + a;
			first = i;

		}


		try {

			int indexOfPoint = a.contains(".") ? a.lastIndexOf(".") : a.length()-1;
			int indexOfCursor = cursor-first;

			double exp = (double)(indexOfPoint-indexOfCursor);

			if (code.charAt(cursor+1)=='.') exp=0;

			return new Quad<>(first, first+a.length(), a, exp);
		} catch (NumberFormatException e) {
			return null;
		}
	}
//
//	static public Triple<Integer, Integer, String> extractNumberAt(String code, int cursor) {
//		int first = cursor;
//		int second = cursor;
//
//		for (int i = cursor; i < code.length() - 1; i++) {
//			if (Character.isWhitespace(code.charAt(i))) break;
//			second = i;
//			if (notNumberPart(code.substring(first, second+1))) {
//				break;
//			}
//		}
//		for (int i = cursor; i >= 1; i--) {
//			if (Character.isWhitespace(code.charAt(i))) break;
//			first = i;
//			if (notNumberPart(code.substring(first - 1, second))) {
//				break;
//			}
//		}
//
//
//		if (first == second) return null;
//
//		try {
//			Double.parseDouble(code.substring(first, Math.min(code.length(), second)));
//
//			return new Triple<>(first, second, code.substring(first, Math.min(code.length(), second)));
//		} catch (NumberFormatException e) {
//			return null;
//		}
//	}

	static private boolean notNumberPart(String c) {

		if (c.length() == 1 && c.equals(".")) return false;

		if (c.length() == 0) return true;
		try {
			Double.parseDouble(c);
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	static public String increment(String number, int pow) {
		BigDecimal c = new BigDecimal(number);
		BigDecimal u = new BigDecimal(1);
		u = u.scaleByPowerOfTen(pow);
		if (c.signum() < 0) u = u.negate();
		return c.add(u)
			.toString();
	}

	static public String decrement(String number, int pow) {
		BigDecimal c = new BigDecimal(number);
		BigDecimal u = new BigDecimal(1);
		u = u.scaleByPowerOfTen(pow);
		if (c.signum() < 0) u = u.negate();
		return c.subtract(u)
			.toString();
	}

}
