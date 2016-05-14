package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;

import retrofit2.Call;

/**
 * Created by miha on 14.05.16.
 *
 */
public class RetrofitResultAdapter implements ResultAdapter {

    @Override
    public void processResult(Bundle putResultHere, Class face, Method method, Object[] args, Object result) throws Exception {
        ((Call<?>) result).execute();
        putResultHere.putString("result", ":)");
    }
}
