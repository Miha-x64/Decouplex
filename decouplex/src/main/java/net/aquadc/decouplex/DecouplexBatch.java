package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.text.TextUtils;

import net.aquadc.decouplex.delivery.DeliveryStrategies;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_EXEC_BATCH;
import static net.aquadc.decouplex.TypeUtils.*;

/**
 * Created by miha on 21.05.16.
 *
 */
public final class DecouplexBatch<HANDLER> {

    /**
     * Instances management
     */
    private static final Map<Integer, WeakReference<DecouplexBatch>> instances = new ConcurrentHashMap<>(4);
    private static AtomicInteger instancesCount = new AtomicInteger();

    @SuppressWarnings("unchecked")
    static <T> DecouplexBatch<T> find(int id) {
        return instances.get(id).get();
    }

    private final int id;
    private final Class<HANDLER> handlerClass;
    final ThreadLocal<List<Request>> requestsLoc = new ThreadLocal<>();

    public DecouplexBatch(Class<HANDLER> handlerClass) {
        this.handlerClass = handlerClass;

        id = instancesCount.incrementAndGet();
        instances.put(id, new WeakReference<DecouplexBatch>(this));
    }

    @SuppressWarnings("unchecked")
    public <T> T add(T decouplex) {
        if (!(decouplex instanceof Proxy))
            throw new IllegalArgumentException("Decouplex proxy must be given.");

        Proxy p = (Proxy) decouplex;
        InvocationHandler h = Proxy.getInvocationHandler(p);
        if (!(h instanceof DcxInvocationHandler))
            throw new IllegalArgumentException("Decouplex proxy must be given.");

        final DcxInvocationHandler d = (DcxInvocationHandler) h;

        return (T) Proxy.newProxyInstance(d.face.getClassLoader(), new Class[]{d.face}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                List<Request> requests = requestsLoc.get();
                if (requests == null) {
                    requests = new ArrayList<>(4);
                    requestsLoc.set(requests);
                }
                requests.add(new Request(d, method, args == null ? EMPTY_ARRAY : args));

                if (method.getReturnType().isPrimitive())
                    return 0;
                return null;
            }
        });
    }

    public void start(Context context) {
        List<Request> requests = requestsLoc.get();
        if (requests.size() == 0)
            throw new IllegalStateException("Trying to start empty batch execution.");
        Bundle batch = new Bundle();
        int i = 0;
        for (Request request : requests) {
            Bundle req = request.dcxInvocationHandler.prepareRequest(request.method, request.args);
            batch.putBundle(Integer.toString(i), req);
            i++;
        }
        batch.putInt("id", id);
        batch.putString("receiver", '_' + handlerClass.getSimpleName());

        Intent service = new Intent(context.getApplicationContext(), DcxService.class);
        service.setAction(ACTION_EXEC_BATCH);
        service.putExtras(batch);

        if (context.startService(service) == null) {
            throw new IllegalStateException("Did you forget to declare DcxService in your manifest?");
        }
        requests.clear();
    }

    /**
     * Dispatch results of a batch invocation
     * @param resultHandler    an object that will receive a result
     * @param results          a bundle with invocation results
     */
    @UiThread
    void dispatchResults(HANDLER resultHandler, Bundle results) {
        Collection<String> methods = new ArrayList<>(3);
        Collection<Set<Object>> resultSets = new ArrayList<>(3);
        int i = 0;
        while (true) {
            String n = Integer.toString(i);
            String strategyName = results.getString("strategy" + n);
            if (strategyName == null) {
                break;
            }
            DeliveryStrategy strategy = DeliveryStrategies.forName(strategyName);
            DcxResponse response = strategy.obtainResponse(results.getParcelable(n));

            String method = response.request.methodName;
            methods.add(method);

            Set<Object> args = new HashSet<>(2);
            args.add(response.result);
            if (response.request.resultAdapter != null) {
                response.request.resultAdapter.adapt(response.request.face, method, null, response.result, args);
            }
            resultSets.add(args);

            i++;
        }

        String method = TextUtils.join(", ", methods);

        Method handler = HandlerSet.forMethod(Void.class, method, true, handlerClass);
        handler.setAccessible(true); // protected methods are inaccessible by default O_o

        Set<Object> resultSet = new HashSet<>();
        for (Set<?> set : resultSets) {
            resultSet.addAll(set);
        }
        try {
            handler.invoke(resultHandler, arguments(handler, resultSet));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    private static final class Request {
        final DcxInvocationHandler dcxInvocationHandler;
        final Method method;
        final Object[] args;

        Request(DcxInvocationHandler dcxInvocationHandler, Method method, Object[] args) {
            this.dcxInvocationHandler = dcxInvocationHandler;
            this.method = method;
            this.args = args;
        }
    }

}
