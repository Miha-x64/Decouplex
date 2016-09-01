package net.aquadc.decouplex.adapter;

import java.lang.reflect.Method;

/**
 * Created by miha on 14.05.16.
 *
 */
public interface ErrorProcessor {
    void process(Class face, Method method, Object[] args, Throwable error);
}
