package com.soda.sodaweather.util;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * 网络请求工具类
 * Created by Ray on 2017/1/8.
 */

public class HttpUtil {

    /**
     * get方法请求网络
     * @param address 接口地址
     * @param callback 方法回调
     */
    public static void sendOkHttpRequest(String address, Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }

}
