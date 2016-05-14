package net.aquadc.decouplex;

import android.content.Context;

import java.lang.reflect.Proxy;

import static net.aquadc.decouplex.Decouplex.*;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexBuilder<FACE> {

    private static int counter;
    private static final Object lock = new Object();

    private Class face;
    private FACE impl;

    public DecouplexBuilder<FACE> face(Class<FACE> face) {
        if (face == null)
            throw new NullPointerException("interface required, null given");
        if (!face.isInterface())
            throw new IllegalArgumentException("interface required");
        this.face = face;
        return this;
    }

    public DecouplexBuilder<FACE> impl(FACE impl) {
        if (impl == null) {
            throw new NullPointerException("implementation required, null given");
        }
        this.impl = impl;
        return this;
    }

    @SuppressWarnings("unchecked")
    public FACE create(Context context) {
        synchronized (lock) {
            counter++;
            implementations.put(counter, impl);
            return (FACE) Proxy.newProxyInstance(
                    face.getClassLoader(), new Class[]{face},
                    new DecouplexRequestHandler<>(context.getApplicationContext(), face, /*impl, */counter));
        }
    }

}
