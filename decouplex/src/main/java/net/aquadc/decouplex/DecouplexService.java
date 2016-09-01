package net.aquadc.decouplex;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import static net.aquadc.decouplex.DecouplexRequest.broadcast;

/**
 * Created by miha on 14.05.16.
 *
 */
public final class DecouplexService extends IntentService {

    private static final Map<String, ScheduledFuture<?>> debounced = new HashMap<>();
    private static final Set<Executor> executors = new HashSet<>();

    public DecouplexService() {
        super(DecouplexService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case ACTION_EXEC: {
                Bundle extras = intent.getExtras();
                DeliveryStrategy deliveryStrategy = DeliveryStrategy.valueOf(extras.getString("deliveryStrategy"));
                DecouplexRequest request = deliveryStrategy.obtainRequest(extras.getParcelable("request"));

                int debounce = extras.getInt("debounce", -1);
                String bReceiver = extras.getString("receiver");
                if (debounce == -1) {
                    findExecutorWith(request.threads).submit(
                            () -> request.executeAndBroadcast(this, bReceiver));
                } else {
                    String methodId = bReceiver + "|" + request.methodName;
                    Future<?> old = debounced.get(methodId);
                    if (old != null) {
                        old.cancel(false);
                    }

                    ScheduledFuture<?> f =
                            findExecutorWith(request.threads).schedule(
                                    () -> request.executeAndBroadcast(this, bReceiver), debounce, methodId);
                    debounced.put(methodId, f);
                }
                break;
            }
            case ACTION_EXEC_BATCH: {
                Bundle bun = intent.getExtras();
                List<Future<Pair<Boolean, DecouplexResponse>>> futures = new ArrayList<>();
                int i = 0;
                Executor exec = null;
                while (true) {
                    Bundle req = bun.getBundle(Integer.toString(i));
                    if (req == null)
                        break;

                    DeliveryStrategy deliveryStrategy = DeliveryStrategy.valueOf(req.getString("deliveryStrategy"));
                    DecouplexRequest request = deliveryStrategy.obtainRequest(req.getParcelable("request"));

                    exec = findExecutorWith(request.threads);
                    Callable<Pair<Boolean, DecouplexResponse>> task = request::execute;
                    futures.add(exec.submit(task));
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
                        for (Future<Pair<Boolean, DecouplexResponse>> future : futures) {
                            Pair<Boolean, DecouplexResponse> result = future.get();
                            if (result.first) {
                                String n = Integer.toString(j);
                                resp.putParcelable(n,
                                        result.second.request.deliveryStrategy.transferResponse(result.second));
                                resp.putString("strategy" + n, result.second.request.deliveryStrategy.name());
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

    private static synchronized Executor findExecutorWith(int threads) {
        Executor executor = null;

        for (Executor ex : executors) {
            if (ex.isFree() && ex.threads == threads) {
                executor = ex;
                break;
            }
        }
        if (executor == null) {
            // no free executors — add one
            executor = new Executor(threads);
            executors.add(executor);
        }
        return executor;
    }
}
