package net.aquadc.decouplex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by miha on 14.05.16.3
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnResult {
    Class face() default Void.class; // handling results on _face_ interface; Void means any
    String value() default "*"; // handling results of _value_ method; "*" means any
}
