package net.aquadc.decouplex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by miha on 09.10.16
 *
 * You can ignore result or error of method invocation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DcxDelivery {
    boolean deliverResult() default true;
    boolean deliverError() default true;
}
