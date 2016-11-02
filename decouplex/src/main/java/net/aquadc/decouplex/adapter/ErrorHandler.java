package net.aquadc.decouplex.adapter;

import net.aquadc.decouplex.DcxRequest;

/**
 * Created by miha on 02.11.16.
 */

public interface ErrorHandler {

    void onError(DcxRequest failedRequest, Throwable error);

    /**
     * Called when an exception raised inside of @OnErrorMethod
     * @param failedRequest    a failed request
     * @param serviceFail      exception that was attempted to be delivered
     * @param onErrorFail      exception that raised inside of @OnError method
     */
    void onErrorDeliveryFail(DcxRequest failedRequest, Throwable serviceFail, Throwable onErrorFail);
}
