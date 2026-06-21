package core.annotations;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExposeInInspector {
    String displayName() default "";
    String tooltip() default "";
}
