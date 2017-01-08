package com.soda.sodaweather.gson;

/**
 * Created by Ray on 2017/1/8.
 */

public class AQI {

    public AQICity city;

    public class AQICity {
        public  String aqi;
        public  String pm25;
    }
}
