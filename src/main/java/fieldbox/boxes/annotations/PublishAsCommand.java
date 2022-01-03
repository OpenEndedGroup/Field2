package fieldbox.boxes.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishAsCommand {
	// command name
	String name();

	// acting as a guard or not?
	boolean isGuard() default false;

	String documentation() default "undocumented";
}
