package com.indoor.position;


import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.Call;

import androidx.annotation.Keep;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * {@link IPSMeasurement} is the DTO for {@link IndoorPositionService} result.
 */

@Keep
@Value
@AllArgsConstructor
public class IPSMeasurement implements Parcelable {
    String text;
    double x;
    double y;
    double z;
    double vx;
    double vy;
    double vz;
    long mapID;
    public Mode mode;
    public enum Mode {
        PARK,
        INDOOR,
        OUTDOOR
    }
    protected IPSMeasurement(Parcel in) {
        x = in.readDouble();
        y = in.readDouble();
        z = in.readDouble();
        vx = in.readDouble();
        vy = in.readDouble();
        vz = in.readDouble();
        mapID = in.readLong();
        mode = Mode.values()[in.readInt()];
        text = in.readString();
    }

    /**
     * Creator for {@link IPSMeasurement}.
     */
    public static final Creator<IPSMeasurement> CREATOR = new Creator<IPSMeasurement>() {
        @Override
        public IPSMeasurement createFromParcel(Parcel in) {
            return new IPSMeasurement(in);
        }

        @Override
        public IPSMeasurement[] newArray(int size) {
            return new IPSMeasurement[size];
        }
    };

    /**
     * Describe the kinds of special objects contained in this Parcelable instance's marshaled
     * representation.
     */
    @Override
    public int describeContents() {
        return 0;
    }


    /**
     * Flatten this object in to a Parcel.
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(x);
        parcel.writeDouble(y);
        parcel.writeDouble(z);
        parcel.writeDouble(vx);
        parcel.writeDouble(vy);
        parcel.writeDouble(vz);
        parcel.writeLong(mapID);
        parcel.writeInt(mode.ordinal());
    }

    /**
     * Register {@link Callback} in {@link IndoorPositionService}, the callback will be called when
     * new position data is received.
     */
    @Keep
    public interface Callback {
        /**
         * TODO: docstring, add usage here.
         */
        void onReceive(IPSMeasurement measurement);
    }
}
