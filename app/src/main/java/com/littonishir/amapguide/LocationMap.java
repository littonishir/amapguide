package com.littonishir.amapguide;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.littonishir.amapguide.overlay.DrivingARouteAOverlay;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LocationMap extends AppCompatActivity implements LocationSource, AMapLocationListener, RouteSearch.OnRouteSearchListener {

    private MapView mGMapView = null;
    private AMap agMap = null;
    private OnLocationChangedListener mListener = null;
    private AMapLocationClient mLocationClient = null;
    private boolean isFirstLoc = true;
    private int ACCESS_COARSE_LOCATION_CODE = 1;
    private int ACCESS_FINE_LOCATION_CODE = 2;
    private int WRITE_EXTERNAL_STORAGE_CODE = 3;
    private int READ_EXTERNAL_STORAGE_CODE = 4;
    private int READ_PHONE_STATE_CODE = 5;


    //路线规划
    private RouteSearch mRouteSearch;
    private DriveRouteResult mDriveRouteResult;
    private DrivingARouteAOverlay drivingARouteOverlay;
    private double latitude;
    private double longitude;
    private boolean isOverlay = false;
    private MarkerOptions markerOption;
    private BitmapDescriptor normal;
    private BitmapDescriptor pressed;
    private Marker myMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basicmap);
        normal = BitmapDescriptorFactory.fromResource(R.drawable.normal);
        pressed = BitmapDescriptorFactory.fromResource(R.drawable.pressed);
        initAmap(savedInstanceState);


    }

    private void initAmap(Bundle savedInstanceState) {
        //获取地图控件引用
        mGMapView = findViewById(R.id.map_view);
        mGMapView.onCreate(savedInstanceState);

        //SDK在Android 6.0下需要进行运行检测的权限如下：
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_COARSE_LOCATION_CODE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_CODE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_CODE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_CODE);
        }

        if (agMap == null) {
            agMap = mGMapView.getMap();
            //设置显示定位按钮 并且可以点击
            UiSettings settings = agMap.getUiSettings();
            settings.setMyLocationButtonEnabled(true);

            agMap.setLocationSource(this);//设置了定位的监听,这里要实现LocationSource接口
            // 是否显示定位按钮
            agMap.setMyLocationEnabled(true);//显示定位层并且可以触发定位,默认是flase
        }

        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        mLocationClient.setLocationListener(this);
        //初始化定位参数
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        //设置定位模式为Hight_Accuracy高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);
        //设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();


        //路线规划
        mRouteSearch = new RouteSearch(this);
        mRouteSearch.setRouteSearchListener(this);

        //添加覆盖物---------------------------------------------
        LatLng latLng = new LatLng(39.906901, 116.397972);
        markerOption = new MarkerOptions();
        markerOption.position(latLng);
        markerOption.draggable(false);//设置Marker可拖动
        markerOption.icon(normal);
        markerOption.setFlat(true);//设置marker平贴地图效果
        myMarker = agMap.addMarker(markerOption);
        //添加覆盖物---------------------------------------------

        //设置地图的点击事件
        agMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                isOverlay = false;
                myMarker.setIcon(normal);
                drivingARouteOverlay.removeFromMap();

            }
        });
        //设置Marker的点击事件
        agMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!isOverlay) {
                    myMarker.setIcon(pressed);
                    searchRouteResult(RouteSearch.DrivingDefault);
                }
                return true;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mGMapView.onDestroy();
        mLocationClient.stopLocation();//停止定位
        mLocationClient.onDestroy();//销毁定位客户端。
        //销毁定位客户端之后，若要重新开启定位请重新New一个AMapLocationClient对象。
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mGMapView.onSaveInstanceState(outState);
    }

    //激活定位
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mListener = null;
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                aMapLocation.getAccuracy();//获取精度信息

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(aMapLocation.getTime());
                df.format(date);//定位时间
                aMapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                aMapLocation.getCountry();//国家信息
                aMapLocation.getProvince();//省信息
                aMapLocation.getCity();//城市信息
                aMapLocation.getDistrict();//城区信息
                aMapLocation.getStreet();//街道信息
                aMapLocation.getStreetNum();//街道门牌号信息
                aMapLocation.getCityCode();//城市编码
                aMapLocation.getAdCode();//地区编码

                // 如果不设置标志位，此时再拖动地图时，它会不断将地图移动到当前的位置
                if (isFirstLoc) {
                    //设置缩放级别
                    agMap.moveCamera(CameraUpdateFactory.zoomTo(14));
                    //将地图移动到定位点
                    agMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude())));
                    //点击定位按钮 能够将地图的中心移动到定位点
                    mListener.onLocationChanged(aMapLocation);
                    //获取定位信息
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(aMapLocation.getCountry() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getCity() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getDistrict() + ""
                            + aMapLocation.getStreet() + ""
                            + aMapLocation.getStreetNum());
                    this.latitude = aMapLocation.getLatitude();
                    this.longitude = aMapLocation.getLongitude();
                    Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                    isFirstLoc = false;
                }
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
                Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * 开始搜索路径规划方案
     */
    public void searchRouteResult(int mode) {
        LatLonPoint mStartPoint = new LatLonPoint(latitude, longitude);
        LatLonPoint mEndPoint = new LatLonPoint(39.906901, 116.397972);
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(mStartPoint, mEndPoint);
        // 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
        RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, mode, null, null, "");
        // 异步路径规划驾车模式查询
        mRouteSearch.calculateDriveRouteAsyn(query);
    }


    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
        if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getPaths() != null) {
                if (result.getPaths().size() > 0) {
                    mDriveRouteResult = result;
                    final DrivePath drivePath = mDriveRouteResult.getPaths()
                            .get(0);
                    if (drivePath == null) {
                        return;
                    }
                    drivingARouteOverlay = new DrivingARouteAOverlay(
                            this, agMap, drivePath,
                            mDriveRouteResult.getStartPos(),
                            mDriveRouteResult.getTargetPos(), null);
                    drivingARouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                    drivingARouteOverlay.setIsColorfulline(false);//是否用颜色展示交通拥堵情况，默认true
                    drivingARouteOverlay.removeFromMap();
                    drivingARouteOverlay.addToMap();
                    drivingARouteOverlay.zoomToSpan();
                    isOverlay = true;
                } else if (result != null && result.getPaths() == null) {
                    Toast.makeText(this, "定位中，终点未no_result设置", Toast.LENGTH_SHORT).show();

                }

            } else {
                Toast.makeText(this, "定位中，终点未no_result设置", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "errorCode:" + errorCode, Toast.LENGTH_SHORT).show();

        }


    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }


    public void back(View view) {
        finish();
    }
}
