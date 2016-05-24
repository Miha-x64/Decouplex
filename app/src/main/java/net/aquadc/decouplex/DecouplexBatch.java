package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static net.aquadc.decouplex.Decouplex.ACTION_EXEC_BATCH;

/**
 * Created by miha on 21.05.16.
 *
 */
public class DecouplexBatch<HANDLER> {

    /**
     * Instances management, like in {@see Decouplex}
     */
    private static final Map<Integer, WeakReference<DecouplexBatch>> instances = new ConcurrentHashMap<>();
    private static AtomicInteger instancesCount = new AtomicInteger();

    @SuppressWarnings("unchecked")
    static <T> DecouplexBatch<T> find(int id) {
        return instances.get(id).get();
    }

    private final int id;
    private final Class<HANDLER> handlerClass;
    private final ThreadLocal<List<Request>> requestsLoc = new ThreadLocal<>();
    private HandlerSet handlers;

    public DecouplexBatch(Class<HANDLER> handlerClass) {
        this.handlerClass = handlerClass;

        id = instancesCount.incrementAndGet();
        instances.put(id, new WeakReference<>(this));
    }

    @SuppressWarnings("unchecked")
    public <T> T add(T decouplex) {
        if (!(decouplex instanceof Proxy))
            throw new IllegalArgumentException("Decouplex proxy must be given.");

        Proxy p = (Proxy) decouplex;
        InvocationHandler h = Proxy.getInvocationHandler(p);
        if (!(h instanceof Decouplex))
            throw new IllegalArgumentException("Decouplex proxy must be given.");

        Decouplex d = (Decouplex) h;

        return (T) Proxy.newProxyInstance(d.face.getClassLoader(), new Class[]{d.face}, ((proxy, method, args) -> {
            List<Request> requests = requestsLoc.get();
            if (requests == null) {
                requests = new ArrayList<>();
                requestsLoc.set(requests);
            }
            requests.add(new Request(d, method, args));

            if (method.getReturnType().isPrimitive())
                return 0;
            return null;
        }));
    }

    public void start(Context context) {
        List<Request> requests = requestsLoc.get();
        if (requests.size() == 0)
            throw new IllegalStateException("Trying to start empty batch execution.");
        Bundle batch = new Bundle();
        int i = 0;
        for (Request request : requests) {
            Bundle req = request.decouplex.prepareRequest(request.method, request.args);
            batch.putBundle(Integer.toString(i), req);
            i++;
        }
        batch.putInt("id", id);

        Intent service = new Intent(context.getApplicationContext(), DecouplexService.class); // TODO: different executors
        service.setAction(ACTION_EXEC_BATCH);
        service.putExtras(batch);

        context.startService(service);
        requests.clear();
    }

    void dispatchResults(HANDLER handler, Bundle results) {
        if (handlers == null) {
            handlers = Handlers.forClass(handlerClass);
        }

        Decouplex.dispatchResults(handler, results, handlers);
    }

    @Override
    public int hashCode() {
        return id;
    }

    class Request {
        final Decouplex decouplex;
        final Method method;
        final Object[] args;

        public Request(Decouplex decouplex, Method method, Object[] args) {
            this.decouplex = decouplex;
            this.method = method;
            this.args = args;
        }
    }

}
