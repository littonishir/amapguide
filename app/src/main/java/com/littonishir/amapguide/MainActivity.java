package com.littonishir.amapguide;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 导航方式
    public static String NAVI_WAY = "NAVI_WAY";
    // 步行导航
    public static String NAVI_WALK = "NAVI_WALK";
    // 骑车导航
    public static String NAVI_RIDE = "NAVI_RIDE";
    // 驾车导航
    public static String NAVI_DRIVE = "NAVI_DRIVE";
    // 导航数据
    public static String NAVI_DATA = "NAVI_DATA";
    // true表示模拟导航，false表示真实GPS导航（默认true）
    public static boolean NAVI_TYPE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //基础地图
        Button basic = findViewById(R.id.basic);
        basic.setOnClickListener(this);
        //定位地图
        Button location = findViewById(R.id.location);
        location.setOnClickListener(this);
        //导航地图
        Button navigational = findViewById(R.id.navigational);
        navigational.setOnClickListener(this);

    }

    // 步行和骑车导航（无定位的）
    String s = "[{\"latitude\":39.904556,\"longitude\":116.427231},{\"latitude\":39.904556,\"longitude\":117.427231}]";
    // 步行和骑车导航（有定位的）
//    String s = "[null,{\"latitude\":39.904556,\"longitude\":116.427231}]";
    // 驾车导航（有定位的）中途无经过点
//    String s = "[null,[],{\"latitude\":39.904556,\"longitude\":116.427231}]";
    // 驾车导航（有定位的）中途有经过点
//    String s = "[null,[{\"latitude\":39.904556,\"longitude\":116.427231}],{\"latitude\":39.904556,\"longitude\":119.427231}]";
    // 驾车导航（无定位的）中途无经过点
//    String s = "[{\"latitude\":39.904556,\"longitude\":116.427231},{},{\"latitude\":39.904556,\"longitude\":117.427231}]";
    // 驾车导航（无定位的）中途有经过点
//    String s = "[{\"latitude\":39.904556,\"longitude\":116.427231},[{\"latitude\":39.904556,\"longitude\":117.427231}],{\"latitude\":39.904556,\"longitude\":119.427231}]";

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.basic:
                startActivity(new Intent(this, BasicMap.class));
                break;
            case R.id.location:
                startActivity(new Intent(this, LocationMap.class));
                break;
            case R.id.navigational:
                startActivity(new Intent(this, NavigationalMap.class).putExtra(NAVI_WAY, NAVI_DRIVE).putExtra(NAVI_DATA, s));
                break;
            default:
                break;
        }
    }
}
