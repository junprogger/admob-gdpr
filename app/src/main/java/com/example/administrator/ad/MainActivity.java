package com.example.administrator.ad;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;


public class MainActivity extends AppCompatActivity {
    Ad ad;
    LinearLayout linearLayoutMain;
    LinearLayout linearLayoutForAd;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linearLayoutMain =  (LinearLayout) findViewById(R.id.main);
        linearLayoutForAd = (LinearLayout) findViewById(R.id.wrap_ad);
        initAd();
    }
    public void gotob(View view){
        Intent intent = new Intent(this, TwoActivity.class);
        ad.interstitialShow(intent);
    }
    private void initAd(){
        Ad.initialize(this);
        ad = new Ad(this, linearLayoutMain, linearLayoutForAd);
        ad.prepareInterstitialAd();
    }
    protected void onStart() {
        ad.bannerShow(R.id.banner_main_activity);
        super.onStart();
    }


}
