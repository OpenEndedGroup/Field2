package fieldagent.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Vector;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Wrap {
	Class value();
}
