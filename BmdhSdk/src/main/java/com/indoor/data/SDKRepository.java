package com.indoor.data;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.indoor.AzimuthIndoorStrategy;
import com.indoor.IAzimuthNaviManager;
import com.indoor.data.entity.author.AuthorData;
import com.indoor.data.entity.projectareo.ProjectAreaData;
import com.indoor.data.http.BaseResponse;
import com.indoor.data.http.DownLoadManager;
import com.indoor.data.http.ExceptionHandle;
import com.indoor.data.http.HttpDataSource;
import com.indoor.data.http.HttpDataSourceImpl;
import com.indoor.data.http.HttpStatus;
import com.indoor.data.http.ResponseThrowable;
import com.indoor.data.http.download.ProgressCallBack;
import com.indoor.data.local.LocalDataSource;
import com.indoor.data.local.LocalDataSourceImpl;
import com.indoor.data.local.db.UserActionData;
import com.indoor.utils.KLog;
import com.indoor.utils.RxAppUtils;
import com.indoor.utils.RxFileUtils;
import com.indoor.utils.RxUtils;
import com.indoor.utils.Utils;

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
public class SDKRepository {
    private static final String TAG = "HttpDataSourceImpl";
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


    public void destroyInstance() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear(); // clear时网络请求会随即cancel
            mCompositeDisposable = null;
        }
        if (mLocalDataSource instanceof LocalDataSourceImpl) {
            ((LocalDataSourceImpl) mLocalDataSource).destroyInstance();
        }
        if (mHttpDataSource instanceof HttpDataSourceImpl) {
            ((HttpDataSourceImpl) mHttpDataSource).destroyInstance();
        }
        INSTANCE = null;
    }


    protected void addSubscribe(Disposable disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mCompositeDisposable.add(disposable);
    }

    public String getApiKey() {
        return mLocalDataSource.getApiKey();
    }

    public String getPackageName() {
        return mLocalDataSource.getPackageName();
    }

    public String getShaCode() {
        return mLocalDataSource.getShaCode();
    }

    public void saveAreaId(String areaId) {
        mLocalDataSource.saveAreaId(areaId);
    }

    public String getAreaId() {
        return mLocalDataSource.getAreaId();
    }

    /**
     * 提交日志信息
     */
    public void submitDefineLogRecord() {

//        UserActionData userActionData=new UserActionData();
//        userActionData.machineManu="";
//        userActionData.machineOs="Android";
//        userActionData.apiKey= mLocalDataSource.getApiKey();
//        userActionData.floorNum="1";
//        userActionData.ipsInfo="info";
//        userActionData.latitude="124.0003435";
//        userActionData.longitude="324.0003435";
//        userActionData.positionState=1;
//        userActionData.projectAreaId=getAreaId();
//        userActionData.uuid="dsdsdsdasdas2321426346235";
//        saveUserActionDataToDB(userActionData);
        List<UserActionData> userActionDatas = mLocalDataSource.getLimitUserActionDataToDB();
        if (userActionDatas == null || userActionDatas.size() == 0) {
            KLog.d(TAG, "no log to submitDefineLogRecord");
            return;
        }
        addSubscribe(mHttpDataSource.submitLogRecord(mLocalDataSource.getToken(), userActionDatas).compose(RxUtils.schedulersTransformer()) //线程调度
                .doOnSubscribe((Consumer<Disposable>) disposable -> {
                    KLog.e(TAG, "doOnSubscribe ... ");
                })
                .subscribe(new Consumer<BaseResponse<String>>() {
                    @Override
                    public void accept(BaseResponse<String> entity) throws Exception {
                        KLog.e(TAG, "submitLogRecord result is " + entity.getResult());
                        if (entity.getResultCode() == HttpStatus.STATUS_CODE_SUCESS) {
                            KLog.d(TAG, "submit sucess...size is " + userActionDatas.size() + "; first recoredId is " + userActionDatas.get(0).recoredId);
                            synchronized (SDKRepository.INSTANCE) {
                                if (mLocalDataSource.getLimitUserActionDataToDB().size() > 0) {
                                    mLocalDataSource.deleteUserActionDataToDB(userActionDatas);
                                    submitDefineLogRecord();
                                } else {
                                    KLog.d(TAG, "nothing to submit...");
                                }

                            }
                        } else if (HttpStatus.isTokenErr(entity.getResultCode())) {
                            //TODOhandle token error
                            KLog.e(TAG, "submit failed,statusCode is "+entity.getResultCode());
                        } else {
                            KLog.e(TAG, "statusCode is other condition...");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ResponseThrowable e = ExceptionHandle.handleException(throwable);
                        KLog.e(TAG + " accept error :", e.getMessage());
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        KLog.e(TAG, " http request complete...");
                    }
                }));
    }

    public String getToken() {
        return mLocalDataSource.getToken();
    }

    /**
     * save 用户数据
     */
    void saveUserActionDataToDB(UserActionData... userActionData){
        mLocalDataSource.saveUserActionDataToDB(userActionData);
    }

    /**
     * SDK鉴权
     *
     * @param authorData
     * @return
     */
    public boolean verrifySDK(AuthorData authorData, IAzimuthNaviManager.IInitSDKListener iInitSDKListener) {
        final boolean[] result = {false};
        iInitSDKListener.initStart();
        addSubscribe(mHttpDataSource.verifyAuth(authorData).compose(RxUtils.schedulersTransformer()) //线程调度
                .doOnSubscribe((Consumer<Disposable>) disposable -> {
                    KLog.e(TAG, "doOnSubscribe ... ");
                })
                .subscribe(new Consumer<BaseResponse<String>>() {
                    @Override
                    public void accept(BaseResponse<String> entity) throws Exception {
                        KLog.e(TAG, "submitLogRecord result is " + entity.getResult());
                        int code = entity.getResultCode();
                        if (code == HttpStatus.STATUS_CODE_SUCESS) {
                            String token = entity.getResult();
                            KLog.e(TAG, "token is :" + token);
                            mLocalDataSource.saveToken(token);
                            submitDefineLogRecord();
                            iInitSDKListener.initSuccess();
                            result[0] = true;
                        } else {
                            //TODOhandle token error
                            iInitSDKListener.initFailed(code, entity.getResultMsg());
                            KLog.e(TAG, "handle Error:" + ExceptionHandle.getHttpExceptionMsg(code));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ResponseThrowable e = ExceptionHandle.handleException(throwable);
                        iInitSDKListener.initFailed(e.code, e.message);
                        KLog.e(TAG + " accept error :", e.message);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        KLog.e(TAG, " http request complete...");
                    }
                }));
        return result[0];
    }

    public void refreshAreaConfig(ProjectAreaData projectAreaData) {
        addSubscribe(mHttpDataSource.getProjectAreaData(projectAreaData).compose(RxUtils.schedulersTransformer()) //线程调度
                .doOnSubscribe((Consumer<Disposable>) disposable -> {
                    KLog.e(TAG, "doOnSubscribe ... ");
                })
                .subscribe(new Consumer<BaseResponse<String>>() {
                    @Override
                    public void accept(BaseResponse<String> entity) throws Exception {
                        KLog.e(TAG, "submitLogRecord result is " + entity.getResult());
                        int code = entity.getResultCode();
                        if (code == HttpStatus.STATUS_CODE_SUCESS) {
                            String token = entity.getResult();
                            KLog.e(TAG, "token is :" + token);
                            //TODO 下载配置文件
                            String url = entity.getResult();
                            String areaId = projectAreaData.getProjectAreaId();
                            DownLoadManager.getInstance().load(url, new ProgressCallBack(AzimuthIndoorStrategy.getMapConfigPath(), AzimuthIndoorStrategy.getAreaCode(areaId)) {
                                @Override
                                public void onSuccess(Object o) {
                                    KLog.d(TAG, "load config sucess...");
                                }

                                @Override
                                public void progress(long progress, long total) {
                                    KLog.d(TAG, "load config progress:" + progress + ";total is " + total);
                                }

                                @Override
                                public void onError(Throwable e) {
                                    KLog.d(TAG, "load config error:" + e.getMessage());
                                }
                            });
                        } else {
                            //TODOhandle token error
                            KLog.e(TAG, "handle Error:" + ExceptionHandle.getHttpExceptionMsg(code));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ResponseThrowable e = ExceptionHandle.handleException(throwable);
                        KLog.e(TAG + " accept error :", e.message);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        KLog.e(TAG, " http request complete...");
                    }
                }));
    }
}
