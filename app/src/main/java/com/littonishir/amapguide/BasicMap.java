package com.littonishir.amapguide;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.amap.api.maps.MapView;

public class BasicMap extends AppCompatActivity {
    private MapView mMapView = null;

    /**
     * 基础地图
     * 在activity的各个生命周期中实现地图生命周期管理
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basicmap);
        mMapView = findViewById(R.id.map_view);
        mMapView.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    public void back(View view) {
        finish();
    }
}
