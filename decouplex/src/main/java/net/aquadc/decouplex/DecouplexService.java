package net.aquadc.decouplex;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import android.util.Pair;

import net.aquadc.decouplex.delivery.DeliveryStrategies;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_ERR;
import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_EXEC;
import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_EXEC_BATCH;
import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_RESULT_BATCH;
import static net.aquadc.decouplex.DcxRequest.broadcast;

/**
 * Created by miha on 14.05.16.
 *
 */
public final class DecouplexService extends IntentService {

    static final SimpleArrayMap<String, ScheduledFuture<?>> debounced = new SimpleArrayMap<>(4);
    private static final Collection<Executor> executors = new HashSet<>(4);

    public DecouplexService() {
        super(DecouplexService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case ACTION_EXEC: {
                Bundle extras = intent.getExtras();
                DeliveryStrategy deliveryStrategy = DeliveryStrategies.forName(extras.getString("deliveryStrategy"));
                final DcxRequest request = deliveryStrategy.obtainRequest(extras.getParcelable("request"));

                class ExecuteAndBroadcastRunnable implements Runnable {
                    private final DcxRequest req;
                    private final Context con;
                    private final String receiver;
                    ExecuteAndBroadcastRunnable(DcxRequest req, Context con, String receiver) {
                        this.req = req;
                        this.con = con;
                        this.receiver = receiver;
                    }
                    @Override
                    public void run() {
                        req.executeAndBroadcast(con, receiver);
                    }
                }

                int debounce = extras.getInt("debounce", -1);
                String bReceiver = extras.getString("receiver");
                if (debounce == -1) {
                    findExecutorWith(request.threads).submit(
                            new ExecuteAndBroadcastRunnable(request, this, bReceiver));
                } else {
                    String methodId = bReceiver + '|' + request.methodName;
                    Future<?> old = debounced.get(methodId);
                    if (old != null) {
                        old.cancel(false);
                    }

                    ScheduledFuture<?> f =
                            findExecutorWith(request.threads).schedule(
                                    new ExecuteAndBroadcastRunnable(request, this, bReceiver), debounce, methodId);
                    debounced.put(methodId, f);
                }
                break;
            }
            case ACTION_EXEC_BATCH: {
                final Bundle bun = intent.getExtras();
                final Collection<Future<Pair<Boolean, DcxResponse>>> futures = new ArrayList<>(4);
                int i = 0;
                Executor exec = null;
                while (true) {
                    final Bundle req = bun.getBundle(Integer.toString(i));
                    if (req == null) {
                        break;
                    }

                    DeliveryStrategy deliveryStrategy = DeliveryStrategies.forName(req.getString("deliveryStrategy"));
                    final DcxRequest request = deliveryStrategy.obtainRequest(req.getParcelable("request"));

                    exec = findExecutorWith(request.threads);
                    Callable<Pair<Boolean, DcxResponse>> task = new Callable<Pair<Boolean, DcxResponse>>() {
                        @Override
                        public Pair<Boolean, DcxResponse> call() throws Exception {
                            return request.execute();
                        }
                    };
                    futures.add(exec.submit(task));
                    i++;
                }

                if (exec == null) {
                    return; // WAT?
                }

                final int id = bun.getInt("id");

                // post waiter-task to last executor
                exec.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<Parcelable> responses = new ArrayList<>(futures.size());
                            List<DeliveryStrategy> strategies = new ArrayList<>(futures.size());

                            for (Future<Pair<Boolean, DcxResponse>> future : futures) {
                                Pair<Boolean, DcxResponse> result = future.get();
                                if (result.second != null) {
                                    if (result.first) {
                                        responses.add(result.second.request.deliveryStrategy.transferResponse(result.second));
                                        strategies.add(result.second.request.deliveryStrategy);
                                    } else {
                                        Log.e("DecouplexService", "Broadcasting error from batch.");
                                        broadcast(DecouplexService.this, ACTION_ERR + bun.get("receiver"), result.second);
                                        return;
                                    }
                                }
                            }

                            Bundle resp = new Bundle(responses.size() + 1);
                            resp.putInt("id", id);
                            for (int k = 0, size = responses.size(); k < size; k++) {
                                String n = Integer.toString(k);
                                resp.putParcelable(n, responses.get(k));
                                resp.putString("strategy" + n, strategies.get(k).name());
                            }

                            if (!broadcast(DecouplexService.this, ACTION_RESULT_BATCH + bun.get("receiver"), resp)) {
                                // take & drop all responses if not successful
                                for (int z = 0, size = responses.size(); z < size; z++) {
                                    /*delete*/ strategies.get(z).obtainResponse(responses.get(z));
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                break;
            }
        }
    }

    static class Executor {

        final int threads;
        private final ScheduledExecutorService executor;

        int tasks;

        Executor(int threads) {
            this.threads = threads;
            executor = Executors.newScheduledThreadPool(threads);
        }

        void submit(final Runnable r) {
            tasks++;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        r.run();
                    } finally {
                        tasks--;
                    }
                }
            });
        }

        <T> Future<T> submit(final Callable<T> c) {
            tasks++;
            return executor.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    try {
                        return c.call();
                    } finally {
                        tasks--;
                    }
                }
            });
        }

        ScheduledFuture<?> schedule(final Runnable r, int millis, final String methodId) {
            tasks++;
            return executor.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        r.run();
                    } finally {
                        tasks--;
                        debounced.remove(methodId);
                    }
                }
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    private static synchronized Executor findExecutorWith(int threads) {
        Executor executor = null;

        for (Executor ex : executors) {
            //  ex is free    &  suitable
            if (ex.tasks == 0 && ex.threads == threads) {
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
