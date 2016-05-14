package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16.
 *
 */
public interface ResultAdapter {
    Set<Object> resultParams(Class face, String methodName, Method handler, Bundle answer);
}
