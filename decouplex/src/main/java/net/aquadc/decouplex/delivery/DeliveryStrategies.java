package net.aquadc.decouplex.delivery;

import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;

import net.aquadc.decouplex.DcxRequest;
import net.aquadc.decouplex.DcxResponse;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Created by miha on 08.10.16
 */
public class DeliveryStrategies {

    private static final SimpleArrayMap<String, DeliveryStrategy> strategies = new ArrayMap<>(2);

    public static final DeliveryStrategy LOCAL = new DeliveryStrategy() {
        private final SimpleArrayMap<UUID, DcxRequest> requests = new SimpleArrayMap<>(3);
        private final SimpleArrayMap<UUID, DcxResponse> responses = new SimpleArrayMap<>(3);

        @Override
        public String name() {
            return "LOCAL";
        }

        @Override
        public Parcelable transferRequest(DcxRequest request) {
            UUID uuid = UUID.randomUUID();
            requests.put(uuid, request);
            return new ParcelUuid(uuid);
        }

        @Override
        public DcxRequest obtainRequest(Parcelable data) {
            DcxRequest request = requests.remove(((ParcelUuid) data).getUuid());
            if (request == null) {
                throw new NullPointerException();
            }
            return request;
        }

        @Override
        public Parcelable transferResponse(DcxResponse response) {
            UUID uuid = UUID.randomUUID();
            responses.put(uuid, response);
            return new ParcelUuid(uuid);
        }

        @Override
        public DcxResponse obtainResponse(Parcelable data) {
            DcxResponse response = responses.remove(((ParcelUuid) data).getUuid());
            if (response == null) {
                throw new NullPointerException();
            }
            return response;
        }
    };
    static {
        strategies.put(LOCAL.name(), LOCAL);
    }

    public static DeliveryStrategy forName(String name) {
        DeliveryStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new NoSuchElementException("No " + name + " in strategies registry.");
        }
        return strategy;
    }

}
