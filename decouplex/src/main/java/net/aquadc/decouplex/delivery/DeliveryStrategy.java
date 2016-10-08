package net.aquadc.decouplex.delivery;

import android.os.Parcelable;

import net.aquadc.decouplex.DcxRequest;
import net.aquadc.decouplex.DcxResponse;

/**
 * Created by miha on 31.08.16
 */
public interface DeliveryStrategy {

    String name();
    Parcelable transferRequest(DcxRequest request);
    DcxRequest obtainRequest(Parcelable data);
    Parcelable transferResponse(DcxResponse response);
    DcxResponse obtainResponse(Parcelable data);

}