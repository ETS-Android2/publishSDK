package com.indoor.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.indoor.data.entity.author.AuthorResponse;
import com.indoor.data.http.BaseResponse;
import com.indoor.data.http.ExceptionHandle;
import com.indoor.data.http.HttpDataSource;
import com.indoor.data.http.HttpStatus;
import com.indoor.data.http.ResponseThrowable;
import com.indoor.data.local.LocalDataSource;
import com.indoor.data.local.db.UserActionData;
import com.indoor.utils.KLog;
import com.indoor.utils.RxUtils;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * MVVM的Model层，统一模块的数据仓库，包含网络数据和本地数据（一个应用可以有多个Repositor）
 * Created by Aaron on 2021/11/18.
 */
public class SDKRepository implements HttpDataSource, LocalDataSource {
    private static final String TAG="HttpDataSourceImpl";
    private volatile static SDKRepository INSTANCE = null;
    private final HttpDataSource mHttpDataSource;

    private final LocalDataSource mLocalDataSource;
    private volatile static CompositeDisposable mCompositeDisposable;
    private SDKRepository(@NonNull HttpDataSource httpDataSource,
                          @NonNull LocalDataSource localDataSource) {
        this.mHttpDataSource = httpDataSource;
        this.mLocalDataSource = localDataSource;
    }

    public static SDKRepository getInstance(HttpDataSource httpDataSource,
                                            LocalDataSource localDataSource) {
        if (INSTANCE == null) {
            synchronized (SDKRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SDKRepository(httpDataSource, localDataSource);
                    mCompositeDisposable = new CompositeDisposable();
                }
            }
        }
        return INSTANCE;
    }



    @VisibleForTesting
    public static void destroyInstance() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear(); // clear时网络请求会随即cancel
            mCompositeDisposable = null;
        }
        INSTANCE = null;
    }


    protected void addSubscribe(Disposable disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mCompositeDisposable.add(disposable);
    }


    /**
     * 提交日志信息
     */
    public void submitDefineLogRecord(){
        List<UserActionData> userActionDatas=getLimitUserActionDataToDB();
                addSubscribe(INSTANCE.submitLogRecord(userActionDatas).compose(RxUtils.schedulersTransformer()) //线程调度
                        .doOnSubscribe((Consumer<Disposable>) disposable -> {
                            KLog.e(TAG,"doOnSubscribe ... ");
                        })
                        .subscribe(new Consumer<BaseResponse<String>>(){
                            @Override
                            public void accept(BaseResponse<String> entity) throws Exception {
                                KLog.e(TAG,"submitLogRecord result is "+entity.getResult());
                                if(entity.getResultCode()== HttpStatus.STATUS_CODE_SUCESS){
                                    KLog.d(TAG,"submit sucess...size is "+userActionDatas.size()+"; first recoredId is "+userActionDatas.get(0).recoredId);
                                    deleteUserActionDataToDB(userActionDatas);
                                    submitDefineLogRecord();
                                } else if(HttpStatus.isTokenErr(entity.getResultCode())){
                                    //TODOhandle token error
                                    KLog.e(TAG,"submit failed,statusCode is other condition...");
                                }else{
                                    KLog.e(TAG,"statusCode is other condition...");
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                ResponseThrowable e= ExceptionHandle.handleException(throwable);
                                KLog.e(TAG+" accept error :",e.getMessage());
                            }
                        }, new Action() {
                            @Override
                            public void run() throws Exception {
                                KLog.e(TAG," http request complete...");
                            }
                        }));
    }


    /**
     * SDK鉴权
     *
     * @param context
     * @return
     */
    public boolean verrifySDK(Context context){
        boolean result= false;
        addSubscribe(INSTANCE.verifyAuth("","","").compose(RxUtils.schedulersTransformer()) //线程调度
                .doOnSubscribe((Consumer<Disposable>) disposable -> {
                    KLog.e(TAG,"doOnSubscribe ... ");
                })
                .subscribe(new Consumer<BaseResponse<AuthorResponse>>(){
                    @Override
                    public void accept(BaseResponse<AuthorResponse> entity) throws Exception {
                        KLog.e(TAG,"submitLogRecord result is "+entity.getResult());
                        if(entity.getResultCode()== HttpStatus.STATUS_CODE_SUCESS){

                        } else if(HttpStatus.isTokenErr(entity.getResultCode())){
                            //TODOhandle token error
                            KLog.e(TAG,"submit failed,statusCode is other condition...");
                        }else{
                            KLog.e(TAG,"statusCode is other condition...");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ResponseThrowable e= ExceptionHandle.handleException(throwable);
                        KLog.e(TAG+" accept error :",e.getMessage());
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        KLog.e(TAG," http request complete...");
                    }
                }));
       return result;
    }


    @Override
    public Observable<BaseResponse<String>> submitLogRecord(List<UserActionData> userActionDatas) {
//        List<UserActionData> actionDataList= mSDKDataBase.getUserActionDao().getTopTenActionData();

        return mHttpDataSource.submitLogRecord(userActionDatas);
    }

    @Override
    public Observable<BaseResponse<AuthorResponse>> verifyAuth(String apikey, String packagename, String sha1) {
        return mHttpDataSource.verifyAuth(apikey, packagename, sha1);
    }

    @Override
    public void saveToken(String token) {

    }

    @Override
    public void saveUnitId(int unitId) {

    }

    @Override
    public String getToken() {
        return null;
    }

    @Override
    public int getUnitID() {
        return 0;
    }



    @Override
    public void saveUserActionDataToDB(UserActionData userActionData) {
        mLocalDataSource.saveUserActionDataToDB(userActionData);
    }

    @Override
    public void deleteUserActionDataToDB(List<UserActionData> userActionDatas) {
        mLocalDataSource.deleteUserActionDataToDB(userActionDatas);
    }

    @Override
    public List<UserActionData> getLimitUserActionDataToDB() {
        return mLocalDataSource.getLimitUserActionDataToDB();
    }
}
