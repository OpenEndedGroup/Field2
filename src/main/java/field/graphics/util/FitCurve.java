package field.graphics.util;

import field.linalg.Vec2;

// untested
public class FitCurve {

	public void fit(Vec2[] p, double[] t) {

		int n = p.length;

		Vec2 P0 = p[0];
		Vec2 P3 = p[n - 1];
		Vec2 P1, P2;

		if (n == 1)

		{
			P1 = P0;
			P2 = P0;
		} else if (n == 2) {

			P1 = P0;
			P2 = P3;
		}
		else if (n==3)
		{
			P1 = p[1];
			P2 = p[1];
		}
		else
		{
			double A1=0, A2=0, A12=0;
			Vec2 C1 = new Vec2();
			Vec2 C2 = new Vec2();
			for (int i = 2;i<n -1;i++)
			{
				double B0 = (1 - t[i])*(1 - t[i])*(1 - t[i]) ;
				double B1 = (3 * t[i] * (1 - t[i])*(1 - t[i]));
				double B2 = (3 * t[i] *t[i]  * (1 - t[i]));
				double B3 = t[i] *t[i] *t[i] ;

				A1 = A1 + B1 * B1;
				A2 = A2 + B2 * B2;
				A12 = A12 + B1 * B2;
				Vec2 temp = (new Vec2(p[i]).sub(new Vec2(P0).mul(B0)).sub(new Vec2(P3).mul(B3)));
				C1.fma((float) B1, temp);
				C2.fma((float) B2, temp);

			}

			double DENOM = (A1 * A2 - A12 * A12);
			if (DENOM == 0) {
				P1 = P0;
				P2 = P3;
			} else {
				P1 = (new Vec2(C1).mul(A2).sub( new Vec2(C2).mul(A12))).mul(1 / DENOM);
				P2 = (new Vec2(C2).mul(A1).sub(  new Vec2(C1).mul(A12))).mul(1 / DENOM);
			}

		}
	}
}
