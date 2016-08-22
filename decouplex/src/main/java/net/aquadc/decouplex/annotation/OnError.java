package net.aquadc.decouplex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by miha on 14.05.16.3
 * for docs {@see OnResult}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnError {
    Class face() default Void.class;
    String value() default "*";
}
