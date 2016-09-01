package net.aquadc.decouplex.adapter;

import java.lang.reflect.Method;

/**
 * Created by miha on 14.05.16
 */
public interface ResultProcessor {
    Object process(Class face, Method method,
                   Object[] args, Object result) throws Exception;
}
