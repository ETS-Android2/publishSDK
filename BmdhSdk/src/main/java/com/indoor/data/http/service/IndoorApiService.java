package com.indoor.data.http.service;

import androidx.room.Entity;

import com.indoor.data.entity.author.AuthorData;
import com.indoor.data.entity.author.AuthorResponse;
import com.indoor.data.entity.projectareo.ProjectAreaData;
import com.indoor.data.http.BaseResponse;
import com.indoor.data.local.db.UserActionData;

import java.util.List;

import io.reactivex.Observable;
import lombok.Builder;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Created by Aaron on  2017/6/15.
 */

public interface IndoorApiService {
    @POST("sdk/app/log/addLogRecord")
    Observable<BaseResponse<String>> submitLogRecord(@Header("Access-Token") String token, @Body List<UserActionData> userActionDatas);

    @POST("sdk/auth/getAccessToken")
    Observable<BaseResponse<String>> verifyAuth(@Body AuthorData authorData);

    @POST("sdk/configfile/getProjectAreaJson")
    Observable<BaseResponse<String>> getProjectAreaData(@Body ProjectAreaData projectArea);
}
