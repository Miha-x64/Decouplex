package net.aquadc.decouplex.example;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by miha on 15.05.16.
 *
 */
public class LongRunningTaskImpl implements LongRunningTask {
    @Override
    public BigInteger calculateFactorial(BigInteger n) {
        BigInteger fact = BigInteger.ONE;
        while (n.compareTo(BigInteger.ONE) > 0) { // n > 1
            fact = fact.multiply(n);
            n = n.subtract(BigInteger.ONE);
        }
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            //
        }
        return fact;
    }
}
