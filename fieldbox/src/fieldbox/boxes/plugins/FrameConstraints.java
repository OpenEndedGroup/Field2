package fieldbox.boxes.plugins;

import EDU.Washington.grad.gjb.cassowary.*;
import field.graphics.RunLoop;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.io.IO;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A constraint system for frames.
 * <p>
 * TODO: cassowary?
 */
public class FrameConstraints extends Box {

	static public final Dict.Prop<FrameConstraints> frameConstraints = new Dict.Prop<FrameConstraints>("frameConstraints").type().toCannon()
		    .doc("The constraint layout plugin");

	ClSimplexSolver solver = new ClSimplexSolver();

	public FrameConstraints(Box root) {
		this.properties.put(frameConstraints, this);
		this.properties.putToMap(Boxes.insideRunLoop, "main.updateConstraints", () -> run());
	}

	public interface OnFrameChange {
		public FrameChanging frameChange(Box target, Rect was, Rect now);
	}

	public interface FrameChanging {

	}


	public enum TargetType {
		left(r -> r.x, (r, f) -> {
			double d = f.floatValue() - r.x;
			r.x = f.floatValue();
			r.w -= d;
			return r;
		}),
		right(r -> r.x + r.w, (r, f) -> {
			r.w = (float) (f.floatValue() - r.x);
			return r;
		}),
		top(r -> r.y, (r, f) -> {
			double d = f.floatValue() - r.y;
			r.y = (float) f.floatValue();
			r.h -= d;
			return r;
		}),
		bottom(r -> r.y+ r.h, (r, f) -> {
			r.h = (float) (f.floatValue() - r.y);
			return r;
		}),
		width(r -> r.w, (r, f) -> {
			double d = f.floatValue() - r.w;
			r.x -= d / 2;
			r.w = f.floatValue();
			return r;
		}),
		height(r -> r.h, (r, f) -> {
			double d = f.floatValue() - r.h;
			r.x -= d / 2;
			r.h = f.floatValue();
			return r;
		});

		public final Function<Rect, Number> from;
		public final BiFunction<Rect, Number, Rect> to;

		private TargetType(Function<Rect, Number> from, BiFunction<Rect, Number, Rect> to) {
			this.from = from;
			this.to = to;
		}
	}

	static public class Target {
		BoxRef box;
		TargetType type;
		ClVariable var;
		double lastSuggestedAt;

		@Override
		public String toString() {
			return box+"/"+type;
		}
	}

	public class Constraint {
		ClConstraint constraint;
		Target[] targets;

		public Constraint(ClConstraint c, Target... targets) {
			this.constraint = constraint;
			this.targets = targets;
		}

		public void remove() {
			try {
				solver.removeConstraint(constraint);
			} catch (ExCLConstraintNotFound exCLConstraintNotFound) {
				exCLConstraintNotFound.printStackTrace();
			} catch (ExCLInternalError exCLInternalError) {
				exCLInternalError.printStackTrace();
			}
		}
	}

	Map<String, Target> vars = new LinkedHashMap<>();

	protected Target newTarget(Box b, TargetType type) {
		Target c = new Target();
		c.box = new BoxRef(b);
		c.type = type;
		c.var = new ClVariable(nameOf(b, type), c.type.from.apply(b.properties.get(Box.frame)).floatValue());
		try {
			if (type.equals(TargetType.width) || type.equals(TargetType.height))
				solver.addStay(c.var, ClStrength.medium, 2.0f);
			else
				solver.addStay(c.var, ClStrength.weak, 1.0f);

		} catch (ExCLRequiredFailure exCLRequiredFailure) {
			exCLRequiredFailure.printStackTrace();
		} catch (ExCLInternalError exCLInternalError) {
			exCLInternalError.printStackTrace();
		}
		return c;
	}

	private String nameOf(Box b, TargetType type) {
		return b + "-" + b.properties.get(IO.id) + "/" + type;
	}

	public Target target(Box b, TargetType type) {
		return vars.computeIfAbsent(nameOf(b, type), k -> newTarget(b, type));
	}

	public Constraint newEquals(Target a, Target b) {

		ClLinearEquation eq = new ClLinearEquation(new ClLinearExpression(a.var), b.var, ClStrength.medium, 2f);
		try {
			solver.addConstraint(eq);
		} catch (ExCLRequiredFailure exCLRequiredFailure) {
			exCLRequiredFailure.printStackTrace();
		} catch (ExCLInternalError exCLInternalError) {
			exCLInternalError.printStackTrace();
		}
		return new Constraint(eq, a, b);
	}

	/** probably not a good idea */
	public Constraint[] addWidthAndHeightConstraints(Box b) {
		Target A = target(b, TargetType.width);
		Target B = target(b, TargetType.height);
		try {
			ClConstraint ci = new ClLinearInequality(A.var, CL.GEQ, 0, ClStrength.strong, 2f);
			solver.addConstraint(ci);
			Constraint c1 = new Constraint(ci, A);
			ci = new ClLinearInequality(B.var, CL.GEQ, 0, ClStrength.strong, 2f);
			solver.addConstraint(ci);
			Constraint c2 = new Constraint(ci, B);

			Target A0 = target(b, TargetType.left);
			Target A1 = target(b, TargetType.right);
			ci = new ClLinearEquation(new ClLinearExpression(A1.var).minus(A0.var), A.var, ClStrength.medium, 1f);
			Constraint c3 = new Constraint(ci, A0, A1, A);
			solver.addConstraint(ci);

			Target B0 = target(b, TargetType.top);
			Target B1 = target(b, TargetType.bottom);
			ci = new ClLinearEquation(new ClLinearExpression(B1.var).minus(B0.var), B.var, ClStrength.medium, 1f);
			Constraint c4 = new Constraint(ci, B0, B1, B);
			solver.addConstraint(ci);


			return new Constraint[]{c1, c2, c3, c4};
		} catch (ExCLInternalError exCLInternalError) {
			exCLInternalError.printStackTrace();
		} catch (ExCLRequiredFailure exCLRequiredFailure) {
			exCLRequiredFailure.printStackTrace();
		} catch (ExCLNonlinearExpression exCLNonlinearExpression) {


		}
		return null;
	}


	double epsilon = 0.5f; // quite large, since we are in pixels;

	// we can be more sophisticated than this with beginning and ending an edit, but here we go...
	public boolean run()
	{
		if (runOnce()) runOnce();
		return true;
	}

	public boolean runOnce() {
		Set<Target> changed = new LinkedHashSet<Target>();
		for (Target t : vars.values()) {
			Number m = t.type.from.apply(t.box.get(this).properties.get(Box.frame));
			double v = t.var.value();
			if (Math.abs(m.doubleValue() - v) > epsilon && Math.abs(m.doubleValue() - t.lastSuggestedAt) > epsilon) {
				changed.add(t);
				t.lastSuggestedAt = m.doubleValue();
			}
		}

		if (changed.size()>0)
			Log.log("constraints", "changed are :"+changed);

		if (changed.size() == 0) return false;

		try {
			solver.beginEdit();
			for (Target t : changed) {
				solver.addEditVar(t.var);
			}
			for (Target t : changed) {
				Log.log("constraints", "suggesting :" + t.lastSuggestedAt + " for " + t);
				solver.suggestValue(t.var, t.lastSuggestedAt);
			}
			solver.resolve();
			solver.endEdit();
		} catch (ExCLError exCLInternalError) {
			exCLInternalError.printStackTrace();
		}

		for (Target t : vars.values()) {
			Log.log("constraints", "resolved :"+t+" to be :"+t.var.value());
			t.type.to.apply(t.box.get(this).properties.get(Box.frame), (double) t.var.value());
			if (changed.contains(t)) {

			} else {
				t.lastSuggestedAt = t.var.value();
			}
		}

		return true;
	}

}
