package net.aquadc.decouplex;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

/**
 * Created by miha on 20.08.16
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class DecouplexRequestTest {

    @Test
    public void testParcel() {
        TestInterface impl = (TestInterface) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{TestInterface.class},
                (proxy, method, args) -> {

                    DecouplexRequest request = new DecouplexRequest(100500, method, args);
                    Parcel p = Parcel.obtain();
                    p.writeParcelable(request, 0);

                    p.setDataPosition(0);
                    DecouplexRequest unparceled = p.readParcelable(getClass().getClassLoader());

                    assertTrue(request != unparceled);
                    assertEquals(request.decouplexId, unparceled.decouplexId);
                    assertEquals(request.methodName, unparceled.methodName);
                    assertArrayEquals(request.parameterTypes, unparceled.parameterTypes);
                    unparceled.parameters.get("unparcel please");
                    Class[] types = method.getParameterTypes();
                    assertArrayEquals(types == null ? TypeUtils.EMPTY_ARRAY : types, unparceled.parameterTypes());
                    assertArrayEquals(args == null ? TypeUtils.EMPTY_ARRAY : args, unparceled.parameters());

                    return null;
                });
        impl.method("some text");
        impl.another();
    }

    private interface TestInterface {
        void method(String argument);
        void another();
    }

}
