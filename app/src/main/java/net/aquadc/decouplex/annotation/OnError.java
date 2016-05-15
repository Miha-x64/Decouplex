package net.aquadc.decouplex.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by miha on 14.05.16.3
 * for docs {@see OnResult}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnError {
    Class face() default Void.class;
    String value() default "*";
}
