package field.utility;

public interface OverloadedMath {
	Object __sub__(Object b);
	Object __rsub__(Object b);
	Object __add__(Object b);
	Object __radd__(Object b);
	Object __mul__(Object b);
	Object __rmul__(Object b);
}
