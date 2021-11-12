package com.indoor.data.entity.author;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class AuthorData {
    @SerializedName("authCode")
    private String authCode;
    @SerializedName("packageName")
    private String packageName;
    @SerializedName("shaCode")
    private String shaCode;
}
