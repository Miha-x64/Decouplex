package net.aquadc.decouplex.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by miha on 14.05.16.3
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnResult {
    Class face() default Void.class; // handling results on _face_ interface; Void means any
    String value() default "*"; // handling results of _value_ method; "*" means any
}
