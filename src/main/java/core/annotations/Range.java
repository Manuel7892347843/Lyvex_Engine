package core.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {
    float min() default 0.0f;
    float max() default 100.0f;
}
