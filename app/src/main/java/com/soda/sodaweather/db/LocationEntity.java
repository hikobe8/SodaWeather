package com.soda.sodaweather.db;

/**
 * Created by Ray on 2017/1/17.
 */

public class LocationEntity {

    private String province;
    private String city;
    private String county;

    public LocationEntity(String province, String city, String county) {
        this.province = province;
        this.city = city;
        this.county = county;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getCounty() {
        return county;
    }
}
