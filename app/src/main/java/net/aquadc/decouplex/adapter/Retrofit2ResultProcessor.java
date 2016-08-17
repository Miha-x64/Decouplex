package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.Response;

import static net.aquadc.decouplex.Converter.put;

/**
 * Created by miha on 14.05.16.
 *
 */
public final class Retrofit2ResultProcessor implements ResultProcessor {

    public static final Retrofit2ResultProcessor INSTANCE = new Retrofit2ResultProcessor();

    private Retrofit2ResultProcessor() {}

    @Override
    public void process(Bundle addFieldsHere,
                        Class face, Method method,
                        Object[] args, Object result) throws Exception {
        Response resp = ((Call<?>) result).execute();

        if (!resp.isSuccessful()) {
            throw new HttpException(resp.code(), resp.message());
        }

        Type[] types = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments();
        put(addFieldsHere, "body", types[0], resp.body());

        addFieldsHere.putInt("code", resp.code());
    }
}
