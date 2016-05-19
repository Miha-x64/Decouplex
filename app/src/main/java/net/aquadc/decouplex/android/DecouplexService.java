package net.aquadc.decouplex.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import net.aquadc.decouplex.Decouplex;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.aquadc.decouplex.Decouplex.ACTION_EXEC;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexService extends IntentService {

    private static final Map<Decouplex, Executor> executors = new HashMap<>();

    public DecouplexService() {
        super(DecouplexService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!ACTION_EXEC.equals(intent.getAction()))
            return;

        Bundle req = intent.getExtras();
        Decouplex decouplex = Decouplex.find(req.getInt("id"));

        // find required executor service
        Executor executor = executors.get(decouplex);
        if (executor == null) {
            // no executor for this task — find free one
            for (Map.Entry<Decouplex, Executor> entry : executors.entrySet()) {
                Executor val = entry.getValue();
                if (val.isFree() && val.threads == 1) {
                    executors.remove(entry.getKey());
                    executors.put(decouplex, val);
                    executor = val;
                    break;
                }
            }
        }
        if (executor == null) {
            // no free executors — add one
            executor = new Executor(1);
            executors.put(decouplex, executor);
        }

        executor.submit(() -> decouplex.execute(this, req));
    }

    class Executor {

        public final int threads;
        private final ExecutorService executor;

        private int tasks;

        Executor(int threads) {
            this.threads = threads;
            executor = Executors.newFixedThreadPool(threads);
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
    }
}
