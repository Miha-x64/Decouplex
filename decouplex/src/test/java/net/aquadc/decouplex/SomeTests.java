package net.aquadc.decouplex;

import net.aquadc.decouplex.annotation.DcxNullable;
import net.aquadc.decouplex.annotation.OnError;
import net.aquadc.decouplex.annotation.OnResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by miha on 20.08.16
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class SomeTests {

    @Test
    public void methodTargetingTest() {

        class TargetClass {
            @OnResult(face = Runnable.class, value = "run")
            void onRunnableRunResult() {}
            @OnResult(face = Runnable.class)
            void onRunnableResult() {}
            @OnResult("run")
            void onRunResult() {}
            @OnResult
            void onResult() {}

            @OnError(face = Runnable.class, value = "run")
            void onRunnableRunError() {}
            @OnError(face = Runnable.class)
            void onRunnableError() {}
            @OnError("run")
            void onRunError() {}
            @OnError
            void onError() {}
        }

        class Whatever {}

        assertEquals(
                "onRunnableRunResult",
                HandlerSet.forMethod(Runnable.class, "run", true, TargetClass.class).getName());
        assertEquals(
                "onRunnableResult",
                HandlerSet.forMethod(Runnable.class, "whatever", true, TargetClass.class).getName());
        assertEquals(
                "onRunResult",
                HandlerSet.forMethod(Whatever.class, "run", true, TargetClass.class).getName());
        assertEquals(
                "onResult",
                HandlerSet.forMethod(Whatever.class, "whatever", true, TargetClass.class).getName());

        assertEquals(
                "onRunnableRunError",
                HandlerSet.forMethod(Runnable.class, "run", false, TargetClass.class).getName());
        assertEquals(
                "onRunnableError",
                HandlerSet.forMethod(Runnable.class, "whatever", false, TargetClass.class).getName());
        assertEquals(
                "onRunError",
                HandlerSet.forMethod(Whatever.class, "run", false, TargetClass.class).getName());
        assertEquals(
                "onError",
                HandlerSet.forMethod(Whatever.class, "whatever", false, TargetClass.class).getName());
    }

    @Test
    public void argumentsTargetingTest() throws ReflectiveOperationException {

        class ArgumentsTest {
            public void method0(String arg0, int arg1) {}
            public void method1(int arg0, String arg1) {}
            public void method2(int arg0, String arg1, @DcxNullable Object obj) {}
        }

        Method method0 = ArgumentsTest.class.getDeclaredMethod("method0", String.class, int.class);
        assertArrayEquals(
                new Object[] { "arg0", 0 },
                TypeUtils.arguments(method0, setOf(0, "arg0")));

        Method method1 = ArgumentsTest.class.getDeclaredMethod("method1", int.class, String.class);
        assertArrayEquals(
                new Object[] { 0, "arg0" },
                TypeUtils.arguments(method1, setOf(0, "arg0")));

        Method method2 = ArgumentsTest.class.getDeclaredMethod("method2", int.class, String.class, Object.class);
        assertArrayEquals(
                new Object[] { 0, "arg0", null },
                TypeUtils.arguments(method2, setOf(0, "arg0")));
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... objs) {
        return new HashSet<>(Arrays.asList(objs));
    }

}
