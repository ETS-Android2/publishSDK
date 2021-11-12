package com.indoor.data.http;


import com.indoor.data.entity.author.AuthorResponse;

import io.reactivex.Observable;
import retrofit2.http.Field;

/**
 * Created by Aaron on  2019/3/26.
 */
public interface HttpDataSource {
//    Observable<BaseResponse<DemoEntity>> verify();

    Observable<BaseResponse<AuthorResponse>> verifyAuth(@Field("apikey") String apikey, @Field("packagename") String packagename, @Field("sha1") String sha1);


}
