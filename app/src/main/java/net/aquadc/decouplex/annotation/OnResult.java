package net.aquadc.decouplex.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by miha on 14.05.16.3
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnResult {
    Class face();
    String method();
}
