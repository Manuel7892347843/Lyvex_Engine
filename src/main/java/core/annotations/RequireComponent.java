package core.annotations;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequireComponent {
    Class<?>[] value();
}
