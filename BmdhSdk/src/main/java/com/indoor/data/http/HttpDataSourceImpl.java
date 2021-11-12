package com.indoor.data.http;

import com.indoor.data.entity.author.AuthorResponse;
import com.indoor.data.http.service.IndoorApiService;

import io.reactivex.Observable;

/**
 * Created by Aaron on  2019/3/26.
 */
public class HttpDataSourceImpl implements HttpDataSource {

    private volatile static HttpDataSourceImpl INSTANCE = null;
    private IndoorApiService mIndoorApiService;
    public static HttpDataSourceImpl getInstance(IndoorApiService apiService) {
        if (INSTANCE == null) {
            synchronized (HttpDataSourceImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HttpDataSourceImpl(apiService);
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

    private HttpDataSourceImpl(IndoorApiService mIndoorApiService) {
        this.mIndoorApiService = mIndoorApiService;
    }

//    @Override
//    public Observable<BaseResponse<AuthorResponse>> uploadAction() {
//        return mIndoorApiService.uploadAction();
//    }

    @Override
    public Observable<BaseResponse<AuthorResponse>> verifyAuth(String apikey, String packagename, String sha1) {
        return mIndoorApiService.verifyAuth(apikey, packagename, sha1);
    }
}
