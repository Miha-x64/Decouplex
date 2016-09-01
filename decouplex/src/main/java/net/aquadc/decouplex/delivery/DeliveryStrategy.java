package net.aquadc.decouplex.delivery;

import android.os.ParcelUuid;
import android.os.Parcelable;

import net.aquadc.decouplex.DecouplexRequest;
import net.aquadc.decouplex.DecouplexResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by miha on 31.08.16
 */
public enum DeliveryStrategy {

    LOCAL {
        private final Map<UUID, DecouplexRequest> requests = new HashMap<>();
        private final Map<UUID, DecouplexResponse> responses = new HashMap<>();

        @Override
        public Parcelable transferRequest(DecouplexRequest request) {
            UUID uuid = UUID.randomUUID();
            requests.put(uuid, request);
            return new ParcelUuid(uuid);
        }

        @Override
        public DecouplexRequest obtainRequest(Parcelable data) {
            DecouplexRequest request = requests.remove(((ParcelUuid) data).getUuid());
            if (request == null) {
                throw new NullPointerException();
            }
            return request;
        }

        @Override
        public Parcelable transferResponse(DecouplexResponse response) {
            UUID uuid = UUID.randomUUID();
            responses.put(uuid, response);
            return new ParcelUuid(uuid);
        }

        @Override
        public DecouplexResponse obtainResponse(Parcelable data) {
            DecouplexResponse response = responses.remove(((ParcelUuid) data).getUuid());
            if (response == null) {
                throw new NullPointerException();
            }
            return response;
        }
    },

    /**
     * Experimental.
     */
    REMOTE {
        @Override
        public Parcelable transferRequest(DecouplexRequest request) {
            throw new NoSuchMethodError("not implemented");
//            return request;
        }
        @Override
        public DecouplexRequest obtainRequest(Parcelable data) {
            throw new NoSuchMethodError("not implemented");
//            return (DecouplexRequest) data;
        }
        @Override
        public Parcelable transferResponse(DecouplexResponse response) {
            return null;
        }
        @Override
        public DecouplexResponse obtainResponse(Parcelable data) {
            return null;
        }
    };

    public abstract Parcelable transferRequest(DecouplexRequest request);
    public abstract DecouplexRequest obtainRequest(Parcelable data);
    public abstract Parcelable transferResponse(DecouplexResponse response);
    public abstract DecouplexResponse obtainResponse(Parcelable data);

}