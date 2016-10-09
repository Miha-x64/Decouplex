package net.aquadc.decouplex;

import net.aquadc.decouplex.annotation.OnError;
import net.aquadc.decouplex.annotation.OnResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Created by miha on 20.08.16
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class TargetngTests {

    @Test
    public void targetingTest() {

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

}
