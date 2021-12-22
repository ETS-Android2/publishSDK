package com.indoor.data;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.indoor.AzimuthIndoorStrategy;
import com.indoor.IAzimuthNaviManager;
import com.indoor.data.entity.author.AuthorData;
import com.indoor.data.entity.projectareo.ProjectAreaData;
import com.indoor.data.http.BaseResponse;
import com.indoor.data.http.DownLoadManager;
import com.indoor.data.http.ExceptionHandle;
import com.indoor.data.http.HttpDataSource;
import com.indoor.data.http.HttpDataSourceImpl;
import com.indoor.data.http.ResponseThrowable;
import com.indoor.data.http.ResultCodeUtils;
import com.indoor.data.http.download.ProgressCallBack;
import com.indoor.data.local.LocalDataSource;
import com.indoor.data.local.LocalDataSourceImpl;
import com.indoor.data.local.db.UserActionData;
import com.indoor.position.IPSMeasurement;
import com.indoor.utils.KLog;
import com.indoor.utils.RxDeviceTool;
import com.indoor.utils.RxEncryptTool;
import com.indoor.utils.RxUtils;
import com.indoor.utils.Utils;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * MVVM的Model层，统一模块的数据仓库，包含网络数据和本地数据（一个应用可以有多个Repositor）
 * Created by Aaron on 2021/11/18.
 */
public class SDKRepository {
    private static final String TAG = "SDKRepository";
    private static final String SALT = "shanghai-azimuth-data-Technology-Company-Limited-@-api-key-salt-001-*";
    private volatile static SDKRepository INSTANCE = null;
    private final HttpDataSource mHttpDataSource;

    private final LocalDataSource mLocalDataSource;

    private volatile static CompositeDisposable mCompositeDisposable;

    private AuthorData mAuthorData;
    private String mDesSalt;

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

    public static String getSalt(){
        return SALT;
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
     * 敏感数据加密盐值 = 包名后6位 + sha1第8位开始取16位 + apiKey后20位 + 盐值 第10位开始取18位  md5盐值加密
     */
    public String get3DesSalt() {
        return mDesSalt;
    }

    /**
     * 敏感数据加密盐值 = 包名后6位(小于6位取整个串) + sha1第8位开始取16位(小于16位取8位后的整个串) + apiKey后20位 + 盐值 第10位开始取18位  md5盐值加密
     */
    public void set3DesSalt(){
        String packageNameSix=getPackageName().substring(Math.max(0,getPackageName().length()-6));
        String sha1Sixteen=getShaCode().substring(8,Math.min(getShaCode().length()-8, 24));
        String apikeyTwenty=getApiKey().substring(getApiKey().length()-20);
        String saltEighteen=getSalt().substring(10,Math.min(getSalt().length()-10, 28));
        String mSalt=packageNameSix+sha1Sixteen+apikeyTwenty+saltEighteen;
        mDesSalt = RxEncryptTool.encryptMD5ToString(mSalt, getSalt());
//        KLog.e(TAG, "set3DesSalt,mSalt is "+mSalt+";mDesSalt is "+mDesSalt);
    }

    /**
     * 提交日志信息
     */
    public void submitDefineLogRecord(boolean shouldRetry) {
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
                        KLog.e(TAG, "submitLogRecord result is " + entity.getResultMsg());
                        if (ResultCodeUtils.isRequestOptionSuccess(entity.getResultCode())) {
                            KLog.d(TAG, "submit sucess...size is " + userActionDatas.size() + "; first recoredId is " + userActionDatas.get(0).recoredId);
                            synchronized (SDKRepository.INSTANCE) {
                                if (mLocalDataSource.getLimitUserActionDataToDB().size() > 0) {
                                    mLocalDataSource.deleteUserActionDataToDB(userActionDatas);
                                    submitDefineLogRecord(true);
                                } else {
                                    KLog.d(TAG, "nothing to submit...");
                                }

                            }
                        } else if (shouldRetry && ResultCodeUtils.isTokenErr(entity.getResultCode())) {
                            //TODO handle token error
                            KLog.e(TAG, "submit failed,token error,statusCode is " + entity.getResultCode());
                            KLog.e(TAG, "refreshAreaConfig Error:" + ResultCodeUtils.getHttpResultMsg(entity.getResultCode()));
                            verrifySDK(getAuthorData(), new IAzimuthNaviManager.IInitSDKListener() {
                                @Override
                                public void initStart() {

                                }

                                @Override
                                public void initSuccess(String code) {
                                    submitDefineLogRecord(false);
                                }

                                @Override
                                public void initFailed(String message) {

                                }
                            });
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
    public void saveUserActionDataToDB(IPSMeasurement ipsMeasurement) {

        Gson gson = new Gson();
        UserActionData userActionData=new UserActionData();
        userActionData.machineModel=RxDeviceTool.getBuildMANUFACTURER();
        userActionData.machineOs=RxDeviceTool.getAndroidSdkVersion();
        userActionData.apiKey= mLocalDataSource.getApiKey();
        userActionData.floorNum="1";
        userActionData.ipsInfo=gson.toJson(ipsMeasurement);
        userActionData.latitude="124.0003435";
        userActionData.longitude="324.0003435";
        userActionData.positionState=0;
        userActionData.projectAreaId=ipsMeasurement.getMapID();
        userActionData.uuid= RxDeviceTool.getDeviceId(Utils.getContext());
        mLocalDataSource.saveUserActionDataToDB(userActionData);
    }

    /**
     * SDK鉴权
     *
     * @param authorData
     * @return
     */
    public boolean verrifySDK(AuthorData authorData,  IAzimuthNaviManager.IInitSDKListener iInitSDKListener) {
        if (authorData == null) {
            KLog.e(TAG, "authorData cannot be null ... ");
            return false;
        }
        final boolean[] result = {false};
        iInitSDKListener.initStart();
        addSubscribe(mHttpDataSource.verifyAuth(authorData).compose(RxUtils.schedulersTransformer()) //线程调度
                .doOnSubscribe((Consumer<Disposable>) disposable -> {
                    KLog.e(TAG, "doOnSubscribe ... ");
                })
                .subscribe(new Consumer<BaseResponse<String>>() {
                    @Override
                    public void accept(BaseResponse<String> entity) throws Exception {
                        KLog.e(TAG, "verrifySDK result is " + entity.getResultMsg());
                        if (ResultCodeUtils.isAuthorErr(entity.getResultCode())) {
                            iInitSDKListener.initFailed(entity.getResultMsg());
                            KLog.e(TAG, "handle Error:" + entity.getResultMsg());
                        } else {
                            if (ResultCodeUtils.isRequestOptionSuccess(entity.getResultCode())) {
                                String token = entity.getResult();
                                KLog.e(TAG, "token is :" + token);
                                mLocalDataSource.saveToken(token);
                                submitDefineLogRecord(true);
                                result[0] = true;
                            }
                            iInitSDKListener.initSuccess(entity.getResultCode());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ResponseThrowable e = ExceptionHandle.handleException(throwable);
                        iInitSDKListener.initSuccess(ResultCodeUtils.RESULTCODE.EXCEPTION_ERROR);
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

    public void setAuthorData(AuthorData authorData) {
        this.mAuthorData = authorData;
    }

    public AuthorData getAuthorData() {
        return this.mAuthorData;
    }

    public void refreshAreaConfig(ProjectAreaData projectAreaData, IAzimuthNaviManager.IUpdateAreaConfigListener iUpdateAreaConfigListener, boolean shouldRetry) {
        addSubscribe(mHttpDataSource.getProjectAreaData(projectAreaData).compose(RxUtils.schedulersTransformer()) //线程调度
                .doOnSubscribe((Consumer<Disposable>) disposable -> {
                    KLog.e(TAG, "doOnSubscribe ... ");
                })
                .subscribe(new Consumer<BaseResponse<String>>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void accept(BaseResponse<String> entity) throws Exception {
                        KLog.e(TAG, "refreshAreaConfig result is " + entity.getResult());
                        if (entity.isOk()) {
                            String token = entity.getResult();
                            KLog.e(TAG, "token is :" + token);
                            //TODO 下载配置文件
                            String url = entity.getResult();
                            String areaId = projectAreaData.getProjectAreaId();
                            DownLoadManager.getmInstance().load(url, new ProgressCallBack(AzimuthIndoorStrategy.getMapConfigPath(), AzimuthIndoorStrategy.getAreaCode(areaId)) {
                                @Override
                                public void onSuccess(Object o) {
                                    KLog.d(TAG, "load config sucess...");
                                    if(iUpdateAreaConfigListener!=null){
                                        iUpdateAreaConfigListener.updateSuccess();
                                    }
                                }

                                @Override
                                public void progress(long progress, long total) {
                                    KLog.d(TAG, "load config progress:" + progress + ";total is " + total);
                                }

                                @Override
                                public void onError(Throwable e) {
                                    KLog.d(TAG, "load config error:" + e.getMessage());
                                    if(iUpdateAreaConfigListener!=null){
                                        iUpdateAreaConfigListener.updateError(e);
                                    }
                                }
                            });
                        } else {
                            //TODO handle token error
                            KLog.e(TAG, "refreshAreaConfig Error:" + ResultCodeUtils.getHttpResultMsg(entity.getResultCode()));
                            if (shouldRetry && ResultCodeUtils.isTokenErr(entity.getResultCode())) {
                                verrifySDK(getAuthorData(),  new IAzimuthNaviManager.IInitSDKListener() {
                                    @Override
                                    public void initStart() {

                                    }

                                    @Override
                                    public void initSuccess(String code) {
                                        refreshAreaConfig(projectAreaData, iUpdateAreaConfigListener,false);
                                    }

                                    @Override
                                    public void initFailed(String message) {

                                    }
                                });
                            }else{
                                if(iUpdateAreaConfigListener!=null){
                                    iUpdateAreaConfigListener.updateFailed(entity.getResultMsg());
                                }
                            }

                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ResponseThrowable e = ExceptionHandle.handleException(throwable);
                        KLog.e(TAG + " accept error :", e.message);
                        if(iUpdateAreaConfigListener!=null){
                            iUpdateAreaConfigListener.updateError(e);
                        }
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        KLog.e(TAG, " http request complete...");
                    }
                }));
    }
}
