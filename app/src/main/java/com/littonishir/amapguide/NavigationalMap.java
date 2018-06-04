package com.littonishir.amapguide;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewListener;
import com.amap.api.navi.AMapNaviViewOptions;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.SlidingUpPanelLayout.LayoutParams;
import com.autonavi.tbt.TrafficFacilityInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NavigationalMap extends AppCompatActivity implements AMapNaviViewListener, AMapNaviListener, AMapLocationListener {

    private static final String TAG = "AAA";

    /**
     * 3D导航地图对象
     */
    private AMapNaviView mAMapNaviView;

    /**
     * 导航对象
     */
    private AMapNavi mAMapNavi;

    /**
     * 语音对象
     */
    private TTSController mTtsManager;

    /**
     * 起点坐标
     */
    private final List<NaviLatLng> startList = new ArrayList<>();

    /**
     * 终点坐标
     */
    private final List<NaviLatLng> endList = new ArrayList<>();

    /**
     * 途经点坐标
     */
    private List<NaviLatLng> mWayPointList = new ArrayList<>();

    /**
     * 声明mLocationOption对象
     */
    private AMapLocationClientOption mLocationOption = null;

    /**
     * 声明mlocationClient对象
     */
    private AMapLocationClient mlocationClient = null;

    /**
     * 导航方式
     */
    private String naviWay;

    /**
     * 导航参数
     */
    private JSONArray naviData;

    /**
     * 线程句柄
     */
    private Handler handler = new Handler();

    protected NaviLatLng mEndLatlng = new NaviLatLng(40.084894, 116.603039);
    protected NaviLatLng mStartLatlng = new NaviLatLng(39.825934, 116.342972);
    protected final List<NaviLatLng> sList = new ArrayList<>();
    protected final List<NaviLatLng> eList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_navigationalmap);

        sList.add(mStartLatlng);
        eList.add(mEndLatlng);

        mAMapNaviView = new AMapNaviView(this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mAMapNaviView.setLayoutParams(layoutParams);
        setContentView(mAMapNaviView);

        naviWay = (String) getIntent().getSerializableExtra(MainActivity.NAVI_WAY);
        try {
            naviData = new JSONArray(getIntent().getStringExtra(MainActivity.NAVI_DATA));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            finish();
        }

        initNaviData();
        try {
            // 起点坐标为空,则获取设备当前坐标作为起点
            JSONObject startPos = naviData.getJSONObject(0);
            startList.add(new NaviLatLng(startPos.optDouble("latitude"), startPos.optDouble("longitude")));
            initNavi();
        } catch (Exception e) {
            // 使用定位,获取当前坐标
            Log.e(TAG, "起点坐标为空,获取设备定位坐标作为起点.");
            initLocation();
        }

        mAMapNaviView.onCreate(savedInstanceState);
        setAmapNaviViewOptions();
        mAMapNaviView.setAMapNaviViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAMapNaviView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mAMapNaviView.onPause();
            // 仅仅是停止你当前在说的这句话，一会到新的路口还是会再说的
            mTtsManager.stopSpeaking();
            // 停止导航之后，会触及底层stop，然后就不会再有回调了，但是讯飞当前还是没有说完的半句话还是会说完
            // mAMapNavi.stopNavi();
        } catch (Exception e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mAMapNaviView.onDestroy();
            // since 1.6.0 不再在naviview destroy的时候自动执行AMapNavi.stopNavi();请自行执行
            mAMapNavi.stopNavi();
            mAMapNavi.destroy();
            mTtsManager.destroy();
            if (null != mlocationClient) {
                /**
                 * 如果AMapLocationClient是在当前Activity实例化的，
                 * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
                 */
                mlocationClient.onDestroy();
                mlocationClient = null;
                mLocationOption = null;
            }
        } catch (Exception e) {
        }
    }

    /**
     * 初始化导航起始点、途经点参数
     */
    public void initNaviData() {
        JSONObject endPos;
        if (naviWay.equals(MainActivity.NAVI_DRIVE)) {
            // 驾车导航方式
            // 途经点,最多设置3个
            JSONArray wayPosArray = naviData.optJSONArray(1) == null ? new JSONArray() : naviData.optJSONArray(1);
            int length = wayPosArray.length() > 3 ? 3 : wayPosArray.length();
            for (int i = 0; i < length; i++) {
                JSONObject wayPos = wayPosArray.optJSONObject(i);
                mWayPointList.add(new NaviLatLng(wayPos.optDouble("latitude"), wayPos.optDouble("longitude")));
            }
        } else {
            // 步行和骑车导航方式
            endPos = naviData.optJSONObject(1);
            endList.add(new NaviLatLng(endPos.optDouble("latitude"), endPos.optDouble("longitude")));
        }
    }

    /**
     * 初始化导航
     */
    public void initNavi() {
        // 实例化语音引擎
        mTtsManager = TTSController.getInstance(getApplicationContext());
        mTtsManager.init();
        mTtsManager.startSpeaking();

        mAMapNavi = AMapNavi.getInstance(getApplicationContext());
        mAMapNavi.addAMapNaviListener(this);
        mAMapNavi.addAMapNaviListener(mTtsManager);

        // 设置模拟导航的行车速度
//        mAMapNavi.setEmulatorNaviSpeed(75);
    }

    /**
     * 设置导航参数
     */
    private void setAmapNaviViewOptions() {
        if (mAMapNaviView == null) {
            return;
        }
        AMapNaviViewOptions viewOptions = new AMapNaviViewOptions();
        viewOptions.setSettingMenuEnabled(false);//设置菜单按钮是否在导航界面显示
        viewOptions.setNaviNight(false);//设置导航界面是否显示黑夜模式
        viewOptions.setReCalculateRouteForYaw(true);//设置偏航时是否重新计算路径
        viewOptions.setReCalculateRouteForTrafficJam(true);//前方拥堵时是否重新计算路径
        viewOptions.setTrafficInfoUpdateEnabled(true);//设置交通播报是否打开
        viewOptions.setCameraInfoUpdateEnabled(true);//设置摄像头播报是否打开
        viewOptions.setScreenAlwaysBright(true);//设置导航状态下屏幕是否一直开启。
        viewOptions.setTrafficBarEnabled(false);  //设置 返回路况光柱条是否显示（只适用于驾车导航，需要联网）
        viewOptions.setMonitorCameraEnabled(true); //设置摄像头图标是否显示 是
        // viewOptions.setLayoutVisible(false);  //设置导航界面UI是否显示
        //viewOptions.setNaviViewTopic(mThemeStle);//设置导航界面的主题
        //viewOptions.setZoom(16);
        viewOptions.setTilt(0);  //2D显示
        mAMapNaviView.setViewOptions(viewOptions);
    }

    /**
     * 获取定位坐标
     */
    public void initLocation() {
        mlocationClient = new AMapLocationClient(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位监听
        mlocationClient.setLocationListener(this);
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(true);
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。
        //如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会。
        mLocationOption.setOnceLocationLatest(true);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //设置定位参数
        mlocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        mlocationClient.startLocation();
    }

    /**
     * 界面右下角功能设置按钮的回调接口
     */
    @Override
    public void onNaviSetting() {

    }

    /**
     * 导航页面左下角返回按钮点击后弹出的『退出导航对话框』中选择『确定』后的回调接口
     */
    @Override
    public void onNaviCancel() {
        Log.e(TAG, "导航结束.");
        finish();
    }

    /**
     * 导航页面左下角返回按钮的回调接口 false-由SDK主动弹出『退出导航』对话框，true-SDK不主动弹出『退出导航对话框』，由用户自定义
     *
     * @return
     */
    @Override
    public boolean onNaviBackClick() {
        return false;
    }

    /**
     * 导航界面地图状态的回调
     * i - 地图状态，0:车头朝上状态；1:非锁车状态,即车标可以任意显示在地图区域内。
     *
     * @param i
     */
    @Override
    public void onNaviMapMode(int i) {

    }

    /**
     * 转弯view的点击回调
     */
    @Override
    public void onNaviTurnClick() {

    }

    /**
     * 下一个道路View点击回调
     */
    @Override
    public void onNextRoadClick() {

    }

    /**
     * 全览按钮点击回调
     */
    @Override
    public void onScanViewButtonClick() {

    }

    /**
     * 锁地图状态发生变化时回调
     *
     * @param b
     */
    @Override
    public void onLockMap(boolean b) {

    }

    @Override
    public void onNaviViewLoaded() {
        Log.e(TAG, "导航页面加载成功");
        Log.e(TAG, "请不要使用AMapNaviView.getMap().setOnMapLoadedListener();会overwrite导航SDK内部画线逻辑");
    }

    /**
     * 导航创建失败时的回调函数
     */
    @Override
    public void onInitNaviFailure() {
        Log.e(TAG, "导航创建失败");
    }

    /**
     * 初始化成功
     */
    @Override
    public void onInitNaviSuccess() {
        /**
         * 方法: int strategy=mAMapNavi.strategyConvert(congestion,
         * avoidhightspeed, cost, hightspeed, multipleroute); 参数:
         *
         * @congestion 躲避拥堵
         * @avoidhightspeed 不走高速
         * @cost 避免收费
         * @hightspeed 高速优先
         * @multipleroute 多路径
         *
         *  说明:
         *      以上参数都是boolean类型，其中multipleroute参数表示是否多条路线，如果为true则此策略会算出多条路线。
         *      注意: 不走高速与高速优先不能同时为true 高速优先与避免收费不能同时为true
         */
        int strategy = 0;
        try {
            strategy = mAMapNavi.strategyConvert(true, false, false, true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (naviWay.contains(MainActivity.NAVI_WALK)) {
            mAMapNavi.calculateWalkRoute(startList.get(0), endList.get(0)); // 步行导航
        } else if (naviWay.contains(MainActivity.NAVI_RIDE)) {
            mAMapNavi.calculateRideRoute(startList.get(0), endList.get(0));// 骑车导航
        } else if (naviWay.contains(MainActivity.NAVI_DRIVE)) {
            mAMapNavi.calculateDriveRoute(sList, eList, mWayPointList, strategy);// 驾车导航
        }
    }

    /**
     * 开始导航回调
     * i - 导航类型，1 ： 实时导航，2 ：模拟导航
     *
     * @param i
     */
    @Override
    public void onStartNavi(int i) {
        Log.e(TAG, "启动导航后回调函数=" + i);
    }

    /**
     * 当前方路况光柱信息有更新时回调函数。
     */
    @Override
    public void onTrafficStatusUpdate() {

    }

    /**
     * 当前位置回调
     *
     * @param aMapNaviLocation
     */
    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    /**
     * 导航播报信息回调函数
     * i - 播报类型，包含导航播报、前方路况播报和整体路况播报，类型请见NaviTTSType
     * s - 播报文字
     *
     * @param i
     * @param s
     */
    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onGetNavigationText(String s) {

    }

    /**
     * 结束模拟导航
     */
    @Override
    public void onEndEmulatorNavi() {

    }

    /**
     * 到达目的地后回调函数
     */
    @Override
    public void onArriveDestination() {
        Log.e(TAG, "到达目的地");
    }

    /**
     * 步行或者驾车路径规划失败后的回调函数
     *
     * @param i
     */
    @Override
    public void onCalculateRouteFailure(int i) {
        Toast.makeText(NavigationalMap.this, "路径规划失败=" + i + ",失败原因查看官方错误码对照表", Toast.LENGTH_LONG).show();
        Log.e(TAG, "路径规划失败=" + i);
    }

    /**
     * 步行或驾车导航时,出现偏航后需要重新计算路径的回调函数
     */
    @Override
    public void onReCalculateRouteForYaw() {

    }

    /**
     * 驾车导航时，如果前方遇到拥堵时需要重新计算路径的回调
     */
    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    /**
     * 驾车路径导航到达某个途经点的回调函数
     * i - 到达途径点的编号，标号从1开始，依次累加。 模拟导航下不工作
     *
     * @param i
     */
    @Override
    public void onArrivedWayPoint(int i) {

    }

    /**
     * 用户手机GPS设置是否开启的回调函数
     * b - true,开启;false,未开启
     *
     * @param b
     */
    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    /**
     * 导航数据回调  ********重点
     * 导航引导信息回调 naviInfo 是导航信息类
     * naviInfo - 导航信息对象。
     *
     * @param naviInfo
     */
    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    /**
     * 过时
     *
     * @param aMapNaviInfo
     */
    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }

    /**
     * 显示路口放大图回调
     * aMapNaviCross - 路口放大图类，可以获得此路口放大图bitmap
     *
     * @param aMapNaviCross
     */
    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    /**
     * 关闭路口放大图回调
     */
    @Override
    public void hideCross() {

    }

    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    /**
     * 显示道路信息回调
     * aMapLaneInfos - 道路信息数组，可获得各条道路分别是什么类型，可用于用户使用自己的素材完全自定义显示。
     * bytes - 道路背景数据数组，可用于装载官方的DriveWayView，并显示。
     * bytes1 - 道路推荐数据数组，可用于装载官方的DriveWayView，并显示。
     *
     * @param aMapLaneInfos
     * @param bytes
     * @param bytes1
     */
    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    /**
     * 隐藏车道信息
     */
    @Override
    public void hideLaneInfo() {

    }

    /**
     * 路线计算成功
     *
     * @param ints
     */
    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        Log.e(TAG, "路径规划完毕,开始导航.");
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                // true表示模拟导航，false表示真实GPS导航（默认true）
                if (MainActivity.NAVI_TYPE) {
                    mAMapNavi.startNavi(NaviType.EMULATOR);
                } else {
                    mAMapNavi.startNavi(NaviType.GPS);
                }
            }
        }, 1000);
    }

    /**
     * 通知当前是否显示平行路切换
     * parallelRoadType - 0表示隐藏 1 表示显示主路 2 表示显示辅路
     *
     * @param i
     */
    @Override
    public void notifyParallelRoad(int i) {

    }

    /**
     * 巡航模式（无路线规划）下，道路设施信息更新回调
     * aMapNaviTrafficFacilityInfo - 道路设施信息
     *
     * @param aMapNaviTrafficFacilityInfo
     */
    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    /**
     * 更新交通设施信息
     *
     * @param aMapNaviTrafficFacilityInfos
     */
    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    /**
     * 已过时
     *
     * @param trafficFacilityInfo
     */
    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    /**
     * 巡航模式（无路线规划）下，统计信息更新回调 连续5个点大于15km/h后开始回调
     * aimLessModeStat - 巡航模式（无路线规划）下统计信息
     *
     * @param aimLessModeStat
     */
    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    /**
     * 巡航模式（无路线规划）下，统计信息更新回调 当拥堵长度大于500米且拥堵时间大于5分钟时回调
     *
     * @param aimLessModeCongestionInfo
     */
    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    /**
     * 定位回调接口实现
     */
    @SuppressLint("SimpleDateFormat")
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                startList.add(new NaviLatLng(amapLocation.getLatitude(), amapLocation.getLongitude()));
                initNavi();
                onInitNaviSuccess();
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:" + amapLocation.getErrorCode() +
                        ", errInfo:" + amapLocation.getErrorInfo());
                Toast.makeText(NavigationalMap.this, "location Error, ErrCode:" + amapLocation.getErrorCode() +
                        ", errInfo:" + amapLocation.getErrorInfo(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
