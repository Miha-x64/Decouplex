package net.aquadc.decouplex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by miha on 17.08.16
 *
 * Annotate your interface method and its invocation will be debounced.
 * Does not work with Batch.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Debounce {
    int value(); // delay, ms
}
