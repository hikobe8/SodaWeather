package com.soda.sodaweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Ray on 2017/1/8.
 */

public class Basic {

    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
