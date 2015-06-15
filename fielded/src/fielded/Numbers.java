package fielded;

import field.utility.Triple;

import java.math.BigDecimal;

/**
 * For quickly tokenizing around the current position to manipulate numbers
 */
public class Numbers {

	static public Triple<Integer, Integer, String> extractNumberAt(String code, int cursor) {
		int first = cursor;
		int second = cursor;

		for (int i = cursor; i < code.length() - 1; i++) {
			if (Character.isWhitespace(code.charAt(i))) break;
			second = i;
			if (notNumberPart(code.substring(first, second+1))) {
				break;
			}
		}
		for (int i = cursor; i >= 1; i--) {
			if (Character.isWhitespace(code.charAt(i))) break;
			first = i;
			if (notNumberPart(code.substring(first - 1, second))) {
				break;
			}
		}


		if (first == second) return null;

		try {
			Double.parseDouble(code.substring(first, Math.min(code.length(), second)));

			return new Triple<>(first, second, code.substring(first, Math.min(code.length(), second)));
		} catch (NumberFormatException e) {
			return null;
		}
	}

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

	static public String increment(String number)
	{
		BigDecimal c = new BigDecimal(number);
		BigDecimal u = c.ulp();
		if (c.signum()<0) u = u.negate();
		return c.add(u).toString();
	}
	static public String decrement(String number)
	{
		BigDecimal c = new BigDecimal(number);
		BigDecimal u = c.ulp();
		if (c.signum()<0) u = u.negate();
		return c.subtract(u).toString();
	}

}
