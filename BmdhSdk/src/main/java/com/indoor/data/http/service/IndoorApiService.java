package com.indoor.data.http.service;

import androidx.room.Entity;

import com.indoor.data.entity.author.AuthorResponse;
import com.indoor.data.http.BaseResponse;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by Aaron on  2017/6/15.
 */

public interface IndoorApiService {
//    @GET("action/apiv2/banner?catalog=1")
//    Observable<BaseResponse<AuthorResponse>> uploadAction();

    @FormUrlEncoded
    @POST("sdk/auth/addAuth")
    Observable<BaseResponse<AuthorResponse>> verifyAuth(@Field("apikey") String apikey, @Field("packagename") String packagename, @Field("sha1") String sha1);
}
