package fieldnashorn.annotations;

/**
 * Marks a field or a no-args method as being safe to .toString() or invoke and then .toString() in Autocomplete
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeToToString {
}
