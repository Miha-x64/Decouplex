package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.Response;

import static net.aquadc.decouplex.Decouplex.put;

/**
 * Created by miha on 14.05.16.
 *
 */
public class Retrofit2PostProcessor implements PostProcessor {

    @Override
    public void processAndPutResult(Bundle bun,
                                      Class face, Method method,
                                      Object[] args, Object result) throws Exception {
        Response resp = ((Call<?>) result).execute();

        Type[] types = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments();
        put(bun, "body", types[0], resp.body());

        put(bun, "code", null, resp.code());
    }
}
