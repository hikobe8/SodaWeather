package com.soda.sodaweather.util;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.soda.sodaweather.db.City;
import com.soda.sodaweather.db.County;
import com.soda.sodaweather.db.Province;
import com.soda.sodaweather.gson.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;


/**
 * Created by Ray on 2017/1/8.
 */

public class Utility {

    /**
     * 解析和处理服务器返回的省级数据
     * @param resposne
     * @return
     */
    public static boolean handleProvinceResponse(String resposne) {
        if (!TextUtils.isEmpty(resposne)) {
            try {
                JSONArray allProvinces = new JSONArray(resposne);
                for (int i = 0; i < allProvinces.length(); i ++) {
                    JSONObject jsonObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(jsonObject.getString("name"));
                    province.setProvinceCode(jsonObject.getInt("id"));
                    province.save(); //使用litepal 存储省份数据到数据库
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     * @param resposne
     * @param provinceId 省份id
     * @return
     */
    public static boolean handleCityResponse(String resposne, int provinceId) {
        if (!TextUtils.isEmpty(resposne)) {
            try {
                JSONArray allCities = new JSONArray(resposne);
                for (int i = 0; i < allCities.length(); i ++) {
                    JSONObject jsonObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityName(jsonObject.getString("name"));
                    city.setCityCode(jsonObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save(); //使用litepal 存储市级数据到数据库
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     * @param resposne
     * @param cityId
     * @return
     */
    public static boolean handleCountyResponse(String resposne, int cityId) {
        if (!TextUtils.isEmpty(resposne)) {
            try {
                JSONArray allCounties = new JSONArray(resposne);
                for (int i = 0; i < allCounties.length(); i ++) {
                    JSONObject jsonObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(jsonObject.getString("name"));
                    county.setWeatherId(jsonObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save(); //使用litepal 存储县级数据到数据库
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static Weather handleWeatherResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent, Weather.class);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
