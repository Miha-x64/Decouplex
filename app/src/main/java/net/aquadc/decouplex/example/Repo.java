package net.aquadc.decouplex.example;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by miha on 14.05.16.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repo implements Parcelable {

    public int id;

    public boolean fork;

    public String name;

    @JsonProperty("full_name")
    public String fullName;

    public String description;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeByte((byte) (fork ? 1 : 0));
        dest.writeString(name);
        dest.writeString(fullName);
        dest.writeString(description);
    }

    public static final Parcelable.Creator<Repo> CREATOR = new Creator<Repo>() {
        @Override
        public Repo createFromParcel(Parcel source) {
            Repo repo = new Repo();
            repo.id = source.readInt();
            repo.fork = source.readByte() == 1;
            repo.name = source.readString();
            repo.fullName = source.readString();
            repo.description = source.readString();
            return repo;
        }

        @Override
        public Repo[] newArray(int size) {
            return new Repo[size];
        }
    };
}
