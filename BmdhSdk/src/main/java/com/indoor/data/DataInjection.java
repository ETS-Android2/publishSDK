package com.indoor.data;

import android.content.Context;

import com.indoor.data.http.HttpDataSource;
import com.indoor.data.http.HttpDataSourceImpl;
import com.indoor.data.http.RetrofitClient;
import com.indoor.data.http.service.IndoorApiService;
import com.indoor.data.local.LocalDataSource;
import com.indoor.data.local.LocalDataSourceImpl;


/**
 * 注入全局的数据仓库，可以考虑使用Dagger2。（根据项目实际情况搭建，千万不要为了架构而架构）
 * Created by Aaron on 2019/3/26.
 */
public class DataInjection {
    public static SDKRepository provideDemoRepository(Context context) {
        //网络API服务
        IndoorApiService apiService = RetrofitClient.getInstance().create(IndoorApiService.class);
        //网络数据源
        HttpDataSource httpDataSource = HttpDataSourceImpl.getInstance(apiService);
        //本地数据源
        LocalDataSource localDataSource = LocalDataSourceImpl.getInstance(context);
        //两条分支组成一个数据仓库
        return SDKRepository.getInstance(httpDataSource, localDataSource);
    }
}
