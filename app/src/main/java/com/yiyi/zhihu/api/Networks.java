package com.yiyi.zhihu.api;

import com.yiyi.zhihu.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by yiyi on 2016/12/27.
 */

public class Networks
{

    private static final int DEFAULT_TIMEOUT = 5;

    private static Retrofit retrofit;

    private static CommonApi mCommonApi;

    private static CommentsApi mCommentsApi;

    private static ThemeApi mThemeApi;

    private static Networks mNetworks;

    public static Networks getInstance()
    {
        if (mNetworks == null)
        {
            mNetworks = new Networks();
        }
        return mNetworks;
    }

    private OkHttpClient configClient()
    {
        OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggerInterceptor())
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        return okHttpClient.build();
    }

    private <T> T configRetrofit(Class<T> service)
    {
        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(configClient())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        return retrofit.create(service);
    }

    public CommentsApi getCommentsApi()
    {
        return mCommentsApi == null ? configRetrofit(CommentsApi.class) : mCommentsApi;
    }

    public CommonApi getCommonApi()
    {
        return mCommonApi == null ? configRetrofit(CommonApi.class) : mCommonApi;
    }

    public ThemeApi getThemeApi()
    {
        return mThemeApi == null ? configRetrofit(ThemeApi.class) : mThemeApi;
    }

}
