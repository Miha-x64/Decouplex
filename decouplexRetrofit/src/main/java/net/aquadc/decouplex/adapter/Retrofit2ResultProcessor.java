package net.aquadc.decouplex.adapter;

import java.lang.reflect.Method;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by miha on 14.05.16.
 *
 */
public final class Retrofit2ResultProcessor implements ResultProcessor {

    public static final Retrofit2ResultProcessor INSTANCE = new Retrofit2ResultProcessor();

    private Retrofit2ResultProcessor() {}

    @Override
    public Object process(Class face, Method method, Object[] args, Object result) throws Exception {
        Response resp = ((Call<?>) result).execute();

        if (!resp.isSuccessful()) {
            throw new HttpException(resp.code(), resp.message());
        }

        return resp.body();
    }
}
