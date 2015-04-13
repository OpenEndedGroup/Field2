package field.graphics.util;

import field.graphics.FLinesAndJavaShapes;
import field.linalg.Vec2;
import field.utility.Log;

import java.util.Arrays;
import java.util.List;

public class NearestPoint {

	 int MAXDEPTH = 64;	/*  Maximum depth for recursion */

	 double EPSILON = (Math.pow(2.0, -MAXDEPTH - 1)); /*Flatness control value */
	 int DEGREE = 3;/*  Cubic Bezier curve		*/
	 int W_DEGREE = 5; /*  Degree of eqn to find roots of */

	 public Vec2 NearestPointOnCurve(Vec2 P, Vec2 a, Vec2 c1, Vec2 c2, Vec2 b) {
		return NearestPointOnCurve(P, new Vec2[]{a, c1, c2, b});
	}

	 public Vec2 NearestPointOnCurve(Vec2 P, FLinesAndJavaShapes.CubicSegment3 s) {
		return NearestPointOnCurve(P, new Vec2[]{s.a.toVec2(), s.b.toVec2(), s.c.toVec2(), s.d.toVec2()});
	}

	double lastDistance = 0;
	double lastT = 0;

	public double getLastDistance() {
		return lastDistance;
	}

	public double getLastT() {
		return lastT;
	}

	/*
			 *  NearestPointOnCurve :
			 *  	Compute the parameter value of the point on a Bezier
			 *		curve segment closest to some arbtitrary, user-input point.
			 *		Return the point on the curve at that parameter value.
			 *
			 */
	 public Vec2 NearestPointOnCurve(Vec2 P, Vec2[] V) {
		Vec2[] w;			/* Ctl pts for 5th-degree eqn	*/
		double[] t_candidate = new double[W_DEGREE];	/* Possible roots		*/
		int n_solutions;		/* Number of roots found	*/
		double t;			/* Parameter value of closest pt*/

    /*  Convert problem to 5th-degree Bezier form	*/
		w = ConvertToBezierForm(P, V);

    /* Find all possible roots of 5th-degree equation */
		n_solutions = FindRoots(w, W_DEGREE, t_candidate, 0);

    /* Compare distances of P to all candidates, and to t=0, and t=1 */
		{
			double dist, new_dist;
			Vec2 p;
			Vec2 v;
			int i;


	/* Check distance to beginning of curve, where t = 0	*/
			dist = P.distanceFrom(V[0]);
			t = 0.0;

	/* Find distances for candidate points	*/
			for (i = 0; i < n_solutions; i++) {
				p = Bezier(V, DEGREE, t_candidate[i], null, null);
				new_dist = P.distanceFrom(p);
				if (new_dist < dist) {
					dist = new_dist;
					t = t_candidate[i];
				}
			}

	/* Finally, look at distance to end point, where t = 1.0 */
			new_dist = P.distanceFrom(V[DEGREE]);
			if (new_dist < dist) {
				dist = new_dist;
				t = 1.0;
			}
			lastDistance = dist;
			lastT = t;
		}


    /*  Return the point on the curve at parameter value t */
		return (Bezier(V, DEGREE, t, null, null));
	}


	/*
	 *  ConvertToBezierForm :
	 *		Given a point and a Bezier curve, generate a 5th-degree
	 *		Bezier-format equation whose solution finds the point on the
	 *      curve nearest the user-defined point.
	 */
	public  Vec2[] ConvertToBezierForm(Vec2 P, Vec2[] V) {
		int i, j, k, m, n, ub, lb;
		int row, column;		/* Table indices		*/
		Vec2[] c = new Vec2[DEGREE + 1];		/* V(i)'s - P			*/
		Vec2[] d = new Vec2[DEGREE];		/* V(i+1) - V(i)		*/
		Vec2[] w;			/* Ctl pts of 5th-degree curve  */
		double[][] cdTable = new double[3][4];		/* Dot product of c, d		*/
		double[][] z = {	/* Precomputed "z" for cubics	*/
			    {1.0, 0.6, 0.3, 0.1}, {0.4, 0.6, 0.6, 0.4}, {0.1, 0.3, 0.6, 1.0},};


    /*Determine the c's -- these are vectors created by subtracting*/
    /* point P from each of the control points				*/
		for (i = 0; i <= DEGREE; i++) {
			c[i] = new Vec2(V[i]).sub(P);
		}
    /* Determine the d's -- these are vectors created by subtracting*/
    /* each control point from the next					*/
		for (i = 0; i <= DEGREE - 1; i++) {
			d[i] = V2ScaleII(new Vec2(V[i + 1]).sub(V[i]), 3.0);
		}

    /* Create the c,d table -- this is a table of dot products of the */
    /* c's and d's							*/
		for (row = 0; row <= DEGREE - 1; row++) {
			for (column = 0; column <= DEGREE; column++) {
				cdTable[row][column] = d[row].dot(c[column]);
			}
		}

    /* Now, apply the z's to the dot products, on the skew diagonal*/
    /* Also, set up the x-values, making these "points"		*/
		w = new Vec2[W_DEGREE + 1];
		for (i = 0; i <= W_DEGREE; i++) {
			w[i] = new Vec2();
			w[i].y = 0.0;
			w[i].x = ((double) (i)) / W_DEGREE;
		}

		n = DEGREE;
		m = DEGREE - 1;
		for (k = 0; k <= n + m; k++) {
			lb = Math.max(0, k - m);
			ub = Math.min(k, n);
			for (i = lb; i <= ub; i++) {
				j = k - i;
				w[i + j].y += cdTable[j][i] * z[j][i];
			}
		}

		return (w);
	}


	/*
	 *  FindRoots :
	 *	Given a 5th-degree equation in Bernstein-Bezier form, find
	 *	all of the roots in the interval [0, 1].  Return the number
	 *	of roots found.
	 */
	public  int FindRoots(Vec2[] w, int degree, double[] t, int depth) {
		int i;
		Vec2[] Left = new Vec2[W_DEGREE + 1];	/* New left and right 		*/
		Vec2[] Right = new Vec2[W_DEGREE + 1];	/* control polygons		*/
		int left_count,		/* Solution count from		*/
			    right_count;		/* children			*/
		double[] left_t = new double[W_DEGREE + 1];	/* Solutions from kids		*/
		double[] right_t = new double[W_DEGREE + 1];

//		Log.log("bad", "fr " + Arrays.asList(w) + " " + degree + " " + Arrays.asList(t) + " " + depth);

		switch (CrossingCount(w, degree)) {
			case 0: {	/* No solutions here	*/
				return 0;
			}
			case 1: {	/* Unique solution	*/
	    /* Stop recursion when the tree is deep enough	*/
	    /* if deep enough, return 1 solution at midpoint 	*/
				if (depth >= MAXDEPTH) {
					t[0] = (w[0].x + w[W_DEGREE].x) / 2.0;
					return 1;
				}
				if (ControlPolygonFlatEnough(w, degree)) {
					t[0] = ComputeXIntercept(w, degree);
//					Log.log("bad", "flat enough");
					return 1;
				} else {
//					Log.log("bad", "not flat enough");

				}
				break;
			}
		}

    /* Otherwise, solve recursively after	*/
    /* subdividing control polygon		*/
		Bezier(w, degree, 0.5, Left, Right);
		left_count = FindRoots(Left, degree, left_t, depth + 1);
		right_count = FindRoots(Right, degree, right_t, depth + 1);


    /* Gather solutions together	*/
		for (i = 0; i < left_count; i++) {
			t[i] = left_t[i];
		}
		for (i = 0; i < right_count; i++) {
			t[i + left_count] = right_t[i];
		}

//		Log.log("bad", "out " + Arrays.asList(t) + " " + depth + " " + left_count + " / " + right_count);

    /* Send back total number of solutions	*/
		return (left_count + right_count);
	}


	/*
	 * CrossingCount :
	 *	Count the number of times a Bezier control polygon
	 *	crosses the 0-axis. This number is >= the number of roots.
	 *
	 */
	 int CrossingCount(Vec2[] V, int degree) {
		int i;
		int n_crossings = 0;	/*  Number of zero-crossings	*/
		int sign, old_sign;		/*  Sign of coefficients	*/

		sign = old_sign = (int) Math.signum(V[0].y);
		for (i = 1; i <= degree; i++) {
			sign = (int) Math.signum(V[i].y);
			if (sign != old_sign) n_crossings++;
			old_sign = sign;
		}


//		Log.log("bad", "cc " + n_crossings);
		return n_crossings;
	}



/*
 *  ControlPolygonFlatEnough :
 *	Check if the control polygon of a Bezier curve is flat enough
 *	for recursive subdivision to bottom out.
 *
 *  Corrections by James Walker, jw@jwwalker.com, as follows:

There seem to be errors in the ControlPolygonFlatEnough function in the
Graphics Gems book and the repository (NearestPoint.c). This function
is briefly described on p. 413 of the text, and appears on pages 793-794.
I see two main problems with it.

The idea is to find an upper bound for the error of approximating the x
intercept of the Bezier curve by the x intercept of the line through the
first and last control points. It is claimed on p. 413 that this error is
bounded by half of the difference between the intercepts of the bounding
box. I don't see why that should be true. The line joining the first and
last control points can be on one side of the bounding box, and the actual
curve can be near the opposite side, so the bound should be the difference
of the bounding box intercepts, not half of it.

Second, we come to the implementation. The values distance[i] computed in
the first loop are not actual distances, but squares of distances. I
realize that minimizing or maximizing the squares is equivalent to
minimizing or maximizing the distances.  But when the code claims that
one of the sides of the bounding box has equation
a * x + b * y + c + max_distance_above, where max_distance_above is one of
those squared distances, that makes no sense to me.

I have appended my version of the function. If you apply my code to the
cubic Bezier curve used to test NearestPoint.c,

  Vec2 bezCurve[4] = {    /  A cubic Bezier curve    /
    { 0.0, 0.0 },
    { 1.0, 2.0 },
    { 3.0, 3.0 },
    { 4.0, 2.0 },
    };

my code computes left_intercept = -3.0 and right_intercept = 0.0, which you
can verify by sketching a graph. The original code computes
left_intercept = 0.0 and right_intercept = 0.9.

 */

	/*  int ControlPolygonFlatEnough( const Vec2* V, int degree ) */
	 boolean ControlPolygonFlatEnough(Vec2[] V, int degree) {
		int i;        /* Index variable        */
		double value;
		double max_distance_above;
		double max_distance_below;
		double error;        /* Precision of root        */
		double intercept_1,
			    intercept_2,
			    left_intercept,
			    right_intercept;
		double a, b, c;    /* Coefficients of implicit    */
	    /* eqn for line from V[0]-V[deg]*/
		double det, dInv;
		double a1, b1, c1, a2, b2, c2;

    /* Derive the implicit equation for line connecting first *'
    /*  and last control points */
		a = V[0].y - V[degree].y;
		b = V[degree].x - V[0].x;
		c = V[0].x * V[degree].y - V[degree].x * V[0].y;

		max_distance_above = max_distance_below = 0.0;

		for (i = 1; i < degree; i++) {
			value = a * V[i].x + b * V[i].y + c;

			if (value > max_distance_above) {
				max_distance_above = value;
			} else if (value < max_distance_below) {
				max_distance_below = value;
			}
		}

    /*  Implicit equation for zero line */
		a1 = 0.0;
		b1 = 1.0;
		c1 = 0.0;

    /*  Implicit equation for "above" line */
		a2 = a;
		b2 = b;
		c2 = c - max_distance_above;

		det = a1 * b2 - a2 * b1;
		dInv = 1.0 / det;

		intercept_1 = (b1 * c2 - b2 * c1) * dInv;

    /*  Implicit equation for "below" line */
		a2 = a;
		b2 = b;
		c2 = c - max_distance_below;

		det = a1 * b2 - a2 * b1;
		dInv = 1.0 / det;

		intercept_2 = (b1 * c2 - b2 * c1) * dInv;

    /* Compute intercepts of bounding box    */
		left_intercept = Math.min(intercept_1, intercept_2);
		right_intercept = Math.max(intercept_1, intercept_2);

		error = right_intercept - left_intercept;

		return (error < EPSILON);
	}


	/*
	 *  ComputeXIntercept :
	 *	Compute intersection of chord from first control point to last
	 *  	with 0-axis.
	 *
	 */
/* NOTE: "T" and "Y" do not have to be computed, and there are many useless
 * operations in the following (e.g. "0.0 - 0.0").
 */
	 double ComputeXIntercept(Vec2[] V, int degree) {
		double XLK, YLK, XNM, YNM, XMK, YMK;
		double det, detInv;
		double S, T;
		double X, Y;

		XLK = 1.0 - 0.0;
		YLK = 0.0 - 0.0;
		XNM = V[degree].x - V[0].x;
		YNM = V[degree].y - V[0].y;
		XMK = V[0].x - 0.0;
		YMK = V[0].y - 0.0;

		det = XNM * YLK - YNM * XLK;
		detInv = 1.0 / det;

		S = (XNM * YMK - YNM * XMK) * detInv;
/*  T = (XLK*YMK - YLK*XMK) * detInv; */

		X = 0.0 + XLK * S;
/*  Y = 0.0 + YLK * S; */

		return X;
	}

	 public Vec2 Bezier(List<Vec2> V, int degree, double t) {
		return Bezier(V.toArray(new Vec2[4]), degree, t, null, null);
	}

	/*
	 *  Bezier :
	 *	Evaluate a Bezier curve at a particular parameter value
	 *      Fill in control points for resulting sub-curves if "Left" and
	 *	"Right" are non-null.
	 *
	 */
	 Vec2 Bezier(Vec2[] V, int degree, double t, Vec2[] Left, Vec2[] Right) {
		int i, j;		/* Index variables	*/
		Vec2[][] Vtemp = new Vec2[W_DEGREE + 1][W_DEGREE + 1];
		for (i = 0; i < Vtemp.length; i++) for (j = 0; j < Vtemp[i].length; j++) Vtemp[i][j] = new Vec2();

    /* Copy control points	*/
		for (j = 0; j <= degree; j++) {
			Vtemp[0][j] = new Vec2(V[j]);
		}

    /* Triangle computation	*/
		for (i = 1; i <= degree; i++) {
			for (j = 0; j <= degree - i; j++) {
				Vtemp[i][j].x = (1.0 - t) * Vtemp[i - 1][j].x + t * Vtemp[i - 1][j + 1].x;
				Vtemp[i][j].y = (1.0 - t) * Vtemp[i - 1][j].y + t * Vtemp[i - 1][j + 1].y;
			}
		}

		if (Left != null) {
			for (j = 0; j <= degree; j++) {
				Left[j] = Vtemp[j][0];
			}
		}
		if (Right != null) {
			for (j = 0; j <= degree; j++) {
				Right[j] = Vtemp[degree - j][j];
			}
		}

		return (Vtemp[degree][0]);
	}

	 Vec2 V2ScaleII(Vec2 v, double s) {
		Vec2 result = new Vec2();

		result.x = v.x * s;
		result.y = v.y * s;
		return (result);
	}
}
