package net.aquadc.decouplex.delivery;

import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import net.aquadc.decouplex.BuildConfig;
import net.aquadc.decouplex.DecouplexRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by miha on 31.08.16
 */
public enum DeliveryStrategy {

    LOCAL {
        private final Map<UUID, DecouplexRequest> requests = new HashMap<>();

        @Override
        public Parcelable createRequest(DecouplexRequest request) {
            UUID uuid = UUID.randomUUID();
            requests.put(uuid, request);

            if (BuildConfig.DEBUG) {
                Log.d(getClass().getSimpleName(), "created request: " + uuid);
            }

            return new ParcelUuid(uuid);
        }

        @Override
        public DecouplexRequest obtainRequest(Parcelable data) {
            if (BuildConfig.DEBUG) {
                Log.d(getClass().getSimpleName(), "created request: " + data);
            }

            return requests.remove(((ParcelUuid) data).getUuid());
        }
    },

    /**
     * Experimental.
     */
    REMOTE {
        @Override
        public Parcelable createRequest(DecouplexRequest request) {
            throw new NoSuchMethodError("not implemented");
//            return request;
        }

        @Override
        public DecouplexRequest obtainRequest(Parcelable data) {
            throw new NoSuchMethodError("not implemented");
//            return (DecouplexRequest) data;
        }
    };

    public abstract Parcelable createRequest(DecouplexRequest request);
    public abstract DecouplexRequest obtainRequest(Parcelable data);

}