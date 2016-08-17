package net.aquadc.decouplex;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.aquadc.decouplex.Decouplex.ACTION_ERR;
import static net.aquadc.decouplex.Decouplex.ACTION_EXEC;
import static net.aquadc.decouplex.Decouplex.ACTION_EXEC_BATCH;
import static net.aquadc.decouplex.Decouplex.ACTION_RESULT_BATCH;
import static net.aquadc.decouplex.Decouplex.broadcast;

/**
 * Created by miha on 14.05.16.
 *
 */
public final class DecouplexService extends IntentService {

    private static final Map<String, ScheduledFuture<?>> debounced = new HashMap<>();
    private static final Map<Decouplex, Executor> executors = new HashMap<>();

    public DecouplexService() {
        super(DecouplexService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case ACTION_EXEC: {
                Bundle req = intent.getExtras();
                int id = req.getInt("id");
                Decouplex decouplex = Decouplex.find(id);

                int debounce = req.getInt("debounce", -1);
                if (debounce == -1) {
                    findExecutorFor(decouplex).submit(() -> decouplex.executeAndBroadcast(this, req));
                } else {
                    String methodId = id + "|" + req.getString("receiver") + "|" + req.getString("method");
                    Future<?> old = debounced.get(methodId);
                    if (old != null) {
                        old.cancel(false);
                    }

                    ScheduledFuture<?> f =
                            findExecutorFor(decouplex).schedule(() -> decouplex.executeAndBroadcast(this, req), debounce, methodId);
                    debounced.put(methodId, f);
                }
                break;
            }
            case ACTION_EXEC_BATCH: {
                Bundle bun = intent.getExtras();
                List<Future<Pair<Boolean, Bundle>>> futures = new ArrayList<>();
                int i = 0;
                Executor exec = null;
                while (true) {
                    Bundle req = bun.getBundle(Integer.toString(i));
                    if (req == null)
                        break;

                    Decouplex decouplex = Decouplex.find(req.getInt("id"));

                    exec = findExecutorFor(decouplex);
                    futures.add(exec.submit(() -> decouplex.execute(req)));
                    i++;
                }

                if (exec == null)
                    return; // WAT?

                final int id = bun.getInt("id");

                // post waiter-task to last executor
                exec.submit(() -> {
                    int j = 0;
                    try {
                        Bundle resp = new Bundle();
                        for (Future<Pair<Boolean, Bundle>> future : futures) {
                            Pair<Boolean, Bundle> result = future.get();
                            if (result.first) {
                                resp.putBundle(Integer.toString(j), result.second);
                            } else {
                                Log.e("DecouplexService", "Broadcasting error from batch.");
                                broadcast(this, ACTION_ERR + bun.get("receiver"), result.second);
                                return;
                            }
                            j++;
                        }
                        resp.putInt("id", id);
                        broadcast(this, ACTION_RESULT_BATCH + bun.get("receiver"), resp);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                break;
            }
        }
    }

    static class Executor {

        public final int threads;
        private final ScheduledExecutorService executor;

        private int tasks;

        Executor(int threads) {
            this.threads = threads;
            executor = Executors.newScheduledThreadPool(threads);
        }

        boolean isFree() {
            return tasks == 0;
        }

        void submit(Runnable r) {
            tasks++;
            executor.submit(() -> {
                try {
                    r.run();
                } finally {
                    tasks--;
                }
            });
        }

        <T> Future<T> submit(Callable<T> c) {
            tasks++;
            return executor.submit(() -> {
                try {
                    return c.call();
                } finally {
                    tasks--;
                }
            });
        }

        ScheduledFuture<?> schedule(Runnable r, int millis, String methodId) {
            tasks++;
            return executor.schedule(() -> {
                try {
                    r.run();
                } finally {
                    tasks--;
                    debounced.remove(methodId);
                }
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    private static synchronized Executor findExecutorFor(Decouplex decouplex) {
        Executor executor = executors.get(decouplex);
        if (executor == null) {
            // no executor for this task — find free one
            for (Map.Entry<Decouplex, Executor> entry : executors.entrySet()) {
                Executor val = entry.getValue();
                if (val.isFree() && val.threads == decouplex.threads) {
                    executors.remove(entry.getKey());
                    executors.put(decouplex, val);
                    executor = val;
                    break;
                }
            }
        }
        if (executor == null) {
            // no free executors — add one
            executor = new Executor(decouplex.threads);
            executors.put(decouplex, executor);
        }
        return executor;
    }
}
