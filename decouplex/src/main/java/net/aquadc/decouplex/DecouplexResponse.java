package net.aquadc.decouplex;

import android.support.annotation.UiThread;

/**
 * Created by miha on 01.09.16
 */
public class DecouplexResponse {

    final DecouplexRequest request;
    final Object result;

    DecouplexResponse(DecouplexRequest request, Object result) {
        this.request = request;
        this.result = result;
    }

    @UiThread
    void dispatchResult(Object resultHandler) {
        request.decouplex.dispatchResult(resultHandler, this);
    }

}
