package com.indoor.data.entity.author;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;
@Keep
@NoArgsConstructor
@Data
public class AuthorResponse {

    @SerializedName("result")
    private String result;
    @SerializedName("resultCode")
    private String resultCode;
    @SerializedName("resultMsg")
    private String resultMsg;
    @SerializedName("timestamp")
    private Integer timestamp;
}
