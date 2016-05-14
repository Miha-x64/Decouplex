package net.aquadc.decouplex.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.adapter.ResultAdapter;

import java.lang.reflect.Method;
import java.util.HashSet;

import static net.aquadc.decouplex.Decouplex.*;

/**
 * Created by miha on 14.05.16.
 *
 */
public abstract class DecouplexActivity extends AppCompatActivity {

    private BroadcastReceiver receiver;

    @Override
    protected void onStart() {
        super.onStart();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bun = intent.getExtras();
                if (ACTION_RESULT.equals(intent.getAction())) {
                    try {
                        Class face = Class.forName(bun.getString("face"));
                        String method = bun.getString("method");
                        ResultAdapter adapter = resultAdapter(bun.getInt("resultAdapter"));

                        Method handler = responseHandler(DecouplexActivity.this.getClass(), face, method);
                        handler.setAccessible(true); // protected methods are inaccessible by default O_o

                        if (adapter == null) {
                            handler.invoke(DecouplexActivity.this, bun.get("result"));
                        } else {
                            handler.invoke(DecouplexActivity.this,
                                    arguments(handler.getParameterTypes(),
                                            adapter.resultParams(face, method, handler, bun)));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else if (ACTION_ERR.equals(intent.getAction())) {
                    Throwable e = (Throwable) bun.get("exception");

                    HashSet<Object> args = new HashSet<>();
                    args.add(bun.get("request"));
                    args.add(e);

                    Bundle req = bun.getBundle("request");
                    assert req != null;

                    try {
                        Method handler = errorHandler(DecouplexActivity.this.getClass(),
                                Class.forName(req.getString("face")), req.getString("method"));

                        handler.setAccessible(true);

                        ErrorAdapter adapter = errorAdapter(req.getInt("errorAdapter"));
                        if (adapter != null) {
                            Class face = Class.forName(req.getString("face"));
                            adapter.adaptErrorParams(face, req.getString("method"), handler, e, bun, args);
                        }

                        handler.invoke(DecouplexActivity.this, arguments(handler.getParameterTypes(), args));
                    } catch (Exception f) {
                        throw new RuntimeException(f);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESULT);
        filter.addAction(ACTION_ERR);
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(receiver);
        receiver = null;
        super.onStop();
    }
}
