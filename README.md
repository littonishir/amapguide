## amapguide
高德地图

### 定位

定位主要实现

```
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
```

路线规划

```
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

```



### 使用导航组件


[高德导航API](https://lbs.amap.com/api/android-navi-sdk/guide/navi-component/use-navi-component)

按照官网一步步做即可实现导航，这里记录一些自己遇到的问题。打正式包后没有语音播报，why！！！一通操作猛如虎，敢不给我播报，
你你居然是这样的高德导航。第一时间想到的是混淆问题，没错就是混淆问题。在你的混淆配置文件里配置一下高德导航官网的混淆配置。

```
#3D 地图 V5.0.0之后：
-keep   class com.amap.api.maps.**{*;}
-keep   class com.autonavi.**{*;}
-keep   class com.amap.api.trace.**{*;}

#定位
-keep class com.amap.api.location.**{*;}
-keep class com.amap.api.fence.**{*;}
-keep class com.autonavi.aps.amapapi.model.**{*;}

#搜索
-keep   class com.amap.api.services.**{*;}

#导航
-keep class com.amap.api.navi.**{*;}
-keep class com.autonavi.**{*;}

#内置语音 V5.6.0之后
-dontwarn com.amap.api.col.n3.l
-keep class com.alibaba.idst.nls.** {*;}
-keep class com.google.**{*;}
-keep class com.nlspeech.nlscodec.** {*;}
```

导航主要实现

```
 LatLng startLatLng = new LatLng(39.773801, 116.368984);//新三余公园(南5环)
                LatLng endLatLng = new LatLng(40.041986, 116.414496);//立水桥(北5环)
                Poi start = new Poi("立水桥(北5环)", endLatLng, "");//起点
                Poi end = new Poi("新三余公园(南5环)", startLatLng, "");//终点
                AmapNaviParams amapNaviParams = new AmapNaviParams(start, null, end, AmapNaviType.DRIVER, AmapPageType.NAVI);
                amapNaviParams.setUseInnerVoice(true);
                AmapNaviPage.getInstance().showRouteActivity(getApplicationContext(), amapNaviParams, new INaviInfoCallback() {
                    @Override
                    public void onInitNaviFailure() {

                    }

                    @Override
                    public void onGetNavigationText(String s) {

                    }

                    @Override
                    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

                    }

                    @Override
                    public void onArriveDestination(boolean b) {

                    }

                    @Override
                    public void onStartNavi(int i) {

                    }

                    @Override
                    public void onCalculateRouteSuccess(int[] ints) {

                    }

                    @Override
                    public void onCalculateRouteFailure(int i) {

                    }

                    @Override
                    public void onStopSpeaking() {

                    }

                    @Override
                    public void onReCalculateRoute(int i) {

                    }

                    @Override
                    public void onExitPage(int i) {

                    }

                    @Override
                    public void onStrategyChanged(int i) {

                    }

                    @Override
                    public View getCustomNaviBottomView() {
                        return null;
                    }

                    @Override
                    public View getCustomNaviView() {
                        return null;
                    }

                    @Override
                    public void onArrivedWayPoint(int i) {

                    }
                });
```
