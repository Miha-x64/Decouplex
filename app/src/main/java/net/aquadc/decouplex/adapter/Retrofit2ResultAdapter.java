package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by miha on 15.05.16.
 *
 */
public class Retrofit2ResultAdapter implements ResultAdapter {
    @Override
    public Set<Object> resultParams(Class face, String methodName, Method handler, Bundle answer) {
        HashSet<Object> params = new HashSet<>();
        params.add(answer.get("body"));
        params.add(answer.get("code"));
        return params;
    }
}
