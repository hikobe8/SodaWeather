package com.soda.sodaweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Ray on 2017/1/8.
 */

public class Now {

    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More{

        @SerializedName("txt")
        public String info;

    }

}
