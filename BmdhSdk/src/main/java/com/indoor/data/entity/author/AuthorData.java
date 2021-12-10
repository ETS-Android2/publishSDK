package com.indoor.data.entity.author;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;
@Keep
@NoArgsConstructor
@Data
/**
 {
 "apiKey": "",
 "packageName": "",
 "shaCode": ""
 }
 */
public class AuthorData implements Parcelable {
    @SerializedName("apiKey")
    private String apiKey;
    @SerializedName("packageName")
    private String packageName;
    @SerializedName("shaCode")
    private String shaCode;

    protected AuthorData(Parcel in) {
        apiKey = in.readString();
        packageName = in.readString();
        shaCode = in.readString();
    }

    public static final Creator<AuthorData> CREATOR = new Creator<AuthorData>() {
        @Override
        public AuthorData createFromParcel(Parcel in) {
            return new AuthorData(in);
        }

        @Override
        public AuthorData[] newArray(int size) {
            return new AuthorData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(apiKey);
        dest.writeString(packageName);
        dest.writeString(shaCode);
    }
}
