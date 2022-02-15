package auw;

import field.utility.OverloadedMath;
import java.util.function.Supplier;

public interface _FBuffer extends Supplier<FBuffer>, OverloadedMath.ReducedWhenOverloaded {
    Object source();
}