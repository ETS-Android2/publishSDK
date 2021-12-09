package com.indoor.data.http;

import com.indoor.data.entity.author.AuthorData;
import com.indoor.data.entity.author.AuthorResponse;
import com.indoor.data.entity.projectareo.ProjectAreaData;
import com.indoor.data.http.service.IndoorApiService;
import com.indoor.data.local.db.UserActionData;
import com.indoor.utils.KLog;
import com.indoor.utils.RxUtils;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import retrofit2.http.Body;

/**
 * Created by Aaron on  2019/3/26.
 */
public class HttpDataSourceImpl implements HttpDataSource {

    private static final String TAG="HttpDataSourceImpl";
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


    @Override
    public Observable<BaseResponse<String>> submitLogRecord(List<UserActionData> userActionDatas) {
       return mIndoorApiService.submitLogRecord(userActionDatas);
    }

    @Override
    public Observable<BaseResponse<String>> verifyAuth(AuthorData authorData) {
        return mIndoorApiService.verifyAuth(authorData);
    }

    @Override
    public Observable<BaseResponse<String>> getProjectAreaData(ProjectAreaData projectArea) {
        return mIndoorApiService.getProjectAreaData(projectArea);
    }
}
