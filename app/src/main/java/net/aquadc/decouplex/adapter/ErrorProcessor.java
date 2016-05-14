package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;

/**
 * Created by miha on 14.05.16.
 *
 */
public interface ErrorProcessor {
    void process(Bundle putHere, Class face, Method method, Object[] args, Throwable error);
}
