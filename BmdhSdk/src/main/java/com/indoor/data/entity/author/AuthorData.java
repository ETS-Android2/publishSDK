package com.indoor.data.entity.author;

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
public class AuthorData {
    @SerializedName("apiKey")
    private String apiKey;
    @SerializedName("packageName")
    private String packageName;
    @SerializedName("shaCode")
    private String shaCode;
}
