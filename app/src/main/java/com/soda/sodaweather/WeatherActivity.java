package com.soda.sodaweather;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.soda.sodaweather.db.City;
import com.soda.sodaweather.db.County;
import com.soda.sodaweather.db.LocationEntity;
import com.soda.sodaweather.db.Province;
import com.soda.sodaweather.gson.Forecast;
import com.soda.sodaweather.gson.Weather;
import com.soda.sodaweather.service.AutoUpdateService;
import com.soda.sodaweather.util.HttpUtil;
import com.soda.sodaweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Ray on 2017/1/9.
 */

public class WeatherActivity extends AppCompatActivity {

    @Bind(R.id.title_city)
    TextView mTitleCity;
    @Bind(R.id.title_update_time)
    TextView mTitleUpdateTime;
    @Bind(R.id.degree_text)
    TextView mDegreeText;
    @Bind(R.id.weather_info_text)
    TextView mWeatherInfoText;
    @Bind(R.id.forecast_layout)
    LinearLayout mForecastLayout;
    @Bind(R.id.aqi_text)
    TextView mAqiText;
    @Bind(R.id.pm25_text)
    TextView mPm25Text;
    @Bind(R.id.comfort_text)
    TextView mComfortText;
    @Bind(R.id.car_wash_text)
    TextView mCarWashText;
    @Bind(R.id.sport_text)
    TextView mSportText;
    @Bind(R.id.weather_layout)
    ScrollView mWeatherLayout;
    @Bind(R.id.bing_pic_img)
    ImageView mBingPicImg;
    @Bind(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefresh;
    @Bind(R.id.nav_button)
    Button mNavButton;
    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    ChooseAreaFragment mChooseAreaFragment;

    private String mWeatherId;

    //定位
//    private LocationClient mLocationClient;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    //成功返回市级数据,继续查询县级数据
                    int pId = msg.arg1;
                    final int cId = msg.arg2;
                    Bundle bundle = msg.getData();
                    final String countyName = bundle.getString("county");
                    final String cityName = bundle.getString("city");
                    String address = "http://guolin.tech/api/china/" + pId + "/" + cId;
                    HttpUtil.sendOkHttpRequest(address, new okhttp3.Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseText = response.body().string();
                            boolean result = Utility
                                    .handleCountyResponse(responseText, cId);
                            if (result) {
                                County first = DataSupport.where("countyName = ?", countyName).findFirst(County.class);
                                if (first != null) {
                                    mWeatherId = first.getWeatherId();
                                    requestWeather(mWeatherId);
                                } else {
                                    //当前版本不支持 城市地区， 比如成都市武侯区这种直接 成都市 ，上海市浦东区这种直接上海
                                    County defaultCounty = DataSupport.where("countyName = ?", cityName).findFirst(County.class);
                                    if (defaultCounty != null) {
                                        mWeatherId = defaultCounty.getWeatherId();
                                        requestWeather(mWeatherId);
                                    } else {
                                        mHandler.sendEmptyMessage(2);
                                    }
                                }
                            }

                        }
                    });

                    break;
                case 1:
                    //成功返回县级数据开始更新天气
                    mWeatherId = (String) msg.obj;
                    requestWeather(mWeatherId);
                    break;
                case 2:
                    Toast.makeText(WeatherActivity.this, "切换城市失败!", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private String mCityName;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//        mLocationClient = new LocationClient(getApplicationContext());
        setContentView(R.layout.activity_weather);
        checkNeedPermission();
    }

    private void loadWeather(){
        ButterKnife.bind(this);
        mChooseAreaFragment = (ChooseAreaFragment) getSupportFragmentManager().findFragmentById(R.id.choose_area_fragment);
        mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        mNavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = sharedPreferences.getString("weather", null);

        if (weatherString != null) {
            //有缓存直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeather(weather);
        } else {
            //无缓存的时候请求服务器获取天气数据
            mWeatherId = getIntent().getStringExtra("weather_id");
            mWeatherLayout.setVisibility(View.INVISIBLE);
            mSwipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefresh.setRefreshing(true);
                }
            });
            requestWeather(mWeatherId);
        }
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
        String bingPic = sharedPreferences.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this)
                    .load(bingPic)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .into(mBingPicImg);
        } else {
            loadBingPic();
        }
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }


    /**
     * 检查所需权限
     */
    private void checkNeedPermission() {
        List<String> permissionList = new ArrayList<>();
        //访问位置的权限
        if (ContextCompat.checkSelfPermission(this, Manifest
                .permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest
                .permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        //读取手机状态
        if (ContextCompat.checkSelfPermission(this, Manifest
                .permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        //读写手机外部存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest
                .permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else {
            //权限通过
//            requestLocation();
            loadWeather();
        }
    }

//    private void requestLocation() {
//        LocationClientOption option = new LocationClientOption();
//        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
//        option.setIsNeedAddress(true);
//        mLocationClient.setLocOption(option);
//        mLocationClient.registerLocationListener(new MyLocationListener());
//        mLocationClient.start();
//    }

    /**
     * 获取bing每日一图
     */
    private void loadBingPic() {
        String picUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(picUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                edit.putString("bing_pic", bingPic);
                edit.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this)
                                .load(bingPic)
                                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                                .into(mBingPicImg);
                    }
                });
            }
        });
    }

    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId
                + "&key=ed316cb696304e9d908b01b272df32ae";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        mSwipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            mWeatherId = weather.basic.weatherId;
                            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            edit.putString("weather", responseText);
                            edit.apply();
                            showWeather(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        mSwipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void showWeather(Weather weather) {
        mCityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        mTitleCity.setText(mCityName);
        mTitleUpdateTime.setText(updateTime);
        mDegreeText.setText(degree);
        mForecastLayout.removeAllViews();
        mWeatherInfoText.setText(weatherInfo);
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mForecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temprature.max);
            minText.setText(forecast.temprature.min);
            mForecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            mAqiText.setText(weather.aqi.city.aqi);
            mPm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度: " + weather.suggestion.comfort.info;
        String carwash = "洗车指数: " + weather.suggestion.carWash.info;
        String sport = "运动建议: " + weather.suggestion.sport.info;
        mComfortText.setText(comfort);
        mCarWashText.setText(carwash);
        mSportText.setText(sport);
        mWeatherLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            if (mChooseAreaFragment.mCurrentLevel != ChooseAreaFragment.LEVEL_PROVINCE) {
                mChooseAreaFragment.mBtnBack.performClick();
            } else {
                mDrawerLayout.closeDrawers();
            }
        } else {
            super.onBackPressed();
        }
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            String country = bdLocation.getCountry();
            String county = bdLocation.getDistrict();
            if ("中国".equals(country) && !county.contains(mCityName)) {
                //定位在中国才提示自动切换
                String province = bdLocation.getProvince();
                String city = bdLocation.getCity();
                LocationEntity location = new LocationEntity(province, city, county);
                showSwitchCityDialog(location);
            }

            StringBuilder currentPosition = new StringBuilder();
            currentPosition
                    .append("纬度: ")
                    .append(bdLocation.getLatitude())
                    .append("\n")
                    .append("经度: ")
                    .append(bdLocation.getLongitude())
                    .append("\n")
                    .append("国家: ")
                    .append(bdLocation.getCountry())
                    .append("\n")
                    .append("省:")
                    .append(bdLocation.getProvince())
                    .append("\n")
                    .append("市:")
                    .append(bdLocation.getCity())
                    .append("\n")
                    .append("区: ")
                    .append(bdLocation.getDistrict())
                    .append("\n")
                    .append("街道: ")
                    .append(bdLocation.getStreet())
                    .append("\n")
                    .append("定位方式: ");
            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                currentPosition.append("GPS");
            } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                currentPosition.append("网络");
            }
            Log.e("location", currentPosition.toString());
        }

    }


    /**
     * 切换城市的对话框
     * @param locationEntity 当前定位的城市
     */
    private void showSwitchCityDialog(final LocationEntity locationEntity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name)
                .setMessage("当前定位城市为 "+ locationEntity.getCounty() +",\n" +"是否切换")
                .setPositiveButton("切换", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doSwitch(locationEntity);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void doSwitch(final LocationEntity location) {
        String province = location.getProvince();
        String city = location.getCity();
        String county = location.getCounty();
        province = province.substring(0, province.length() - 1);
        city = city.substring(0, city.length() - 1);
        county = county.substring(0, county.length() - 1);
        final Province foundProvince = DataSupport.where("provinceName = ?", province).findFirst(Province.class);
        final City foundCity = DataSupport.where("cityName = ?", city).findFirst(City.class);
        if (foundCity == null) {
            //网络获取当前市区
            if (foundProvince == null) {
                Log.e(WeatherActivity.this.getClass().getSimpleName(), "查询省级数据失败!");
                return;
            }
            String address = "http://guolin.tech/api/china/" + foundProvince.getProvinceCode();
            final String finalCity = city;
            HttpUtil.sendOkHttpRequest(address, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    boolean result = Utility
                            .handleCityResponse(responseText, foundProvince.getProvinceCode());
                    if (result) {
                        City first = DataSupport.where("cityName = ?", finalCity).findFirst(City.class);
                        if (first != null) {
                            Message message = Message.obtain();
                            message.what = 0;
                            message.arg1 = foundProvince.getProvinceCode();
                            message.arg2 = first.getCityCode();// 携带当前城市id，继续查询下面的县城
                            Bundle cityBundle = new Bundle();
                            cityBundle.putString("city", first.getCityName());
                            cityBundle.putString("county", location.getCounty());
                            message.setData(cityBundle);
                            mHandler.sendMessage(message);
                        } else {
                            //查询失败
                            mHandler.sendEmptyMessage(2);
                        }
                    }

                }
            });
        } else {
            //继续查询市区下的县城
            County foundCounty = DataSupport.where("countyName = ?", county).findFirst(County.class);
            if (foundCounty != null) {
                //获取县城的天气id
                mWeatherId= foundCounty.getWeatherId();
                requestWeather(mWeatherId);
            } else {
                //网络查询当前县城数据
                String address = "http://guolin.tech/api/china/" + foundProvince.getProvinceCode() + "/" + foundCity.getCityCode();
                final String finalCounty = county;
                HttpUtil.sendOkHttpRequest(address, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseText = response.body().string();
                        boolean result = Utility
                                .handleCountyResponse(responseText, foundCity.getCityCode());
                        if (result) {
                            County first = DataSupport.where("countyName = ?", finalCounty).findFirst(County.class);
                            if (first != null) {
                                Message message = Message.obtain();
                                message.what = 1;
                                message.obj = first.getWeatherId(); // 携带当前城市天气id，更新天气
                                mHandler.sendMessage(message);
                            } else {
                                County defaultCity = DataSupport.where("countyName = ?", foundCity.getCityName()).findFirst(County.class);
                                if (defaultCity != null) {
                                    Message message = Message.obtain();
                                    message.what = 1;
                                    message.obj = defaultCity.getWeatherId(); // 携带当前城市天气id，更新天气
                                    mHandler.sendMessage(message);
                                } else {
                                    //查询失败
                                    mHandler.sendEmptyMessage(2);
                                }
                            }
                        }

                    }
                });
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i ++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "权限未通过"+permissions[i], Toast.LENGTH_SHORT).show();
                        }
                    }
//                    requestLocation();
                    loadWeather();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }
}