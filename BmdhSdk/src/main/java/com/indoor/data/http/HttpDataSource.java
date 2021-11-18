package com.indoor.data.http;


import com.indoor.data.entity.author.AuthorResponse;
import com.indoor.data.local.db.UserActionData;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Field;

/**
 * Created by Aaron on  2019/3/26.
 */
public interface HttpDataSource {
    Observable<BaseResponse<String>> submitLogRecord(@Body List<UserActionData> userActionData);

    Observable<BaseResponse<AuthorResponse>> verifyAuth(@Field("apikey") String apikey, @Field("packagename") String packagename, @Field("sha1") String sha1);


}
