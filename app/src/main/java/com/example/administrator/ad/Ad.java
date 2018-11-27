package com.example.administrator.ad;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

    class Ad {
        private static String typeAd;
        private static boolean isInitAd = false;
        private ViewGroup layoutWrapperBannerAd;
        private ViewGroup layoutMain;
        private Context context;
        private GDPR gdpr;
        AdHandler handler;
        private WaitStatus waitStatusTypeAd;
        private Thread threadAdWaitStatus;
        private InterstitialAd objInterstitialAd;
        private boolean bannerLoad = false;
        private boolean interstitialErrorLoad;
        private int bannerId;

        private final static int BANNER = 1;
        private final static int INTRESTITIAL = 2;
        private final static int INTRESTITIAL_SHOW = 3;
        private final static int NEXT_ACTIVITY_SHOW = 4;
        private Intent nextIntent;


        Ad(Context context, ViewGroup layoutMain, ViewGroup layoutWrapperBannerAd) {
            this.context = context;
            this.layoutWrapperBannerAd = layoutWrapperBannerAd;
            this.layoutMain = layoutMain;
            if(!GDPR.isGDPRrequired || notShowAds()){
                if(Ad.getTypeAd() == null){
                    gdpr = new GDPR(context, layoutMain);
                }else {
                    gdpr = new GDPR(context, layoutMain, this);
                }
            }else{
                if(layoutMain.getVisibility()==View.INVISIBLE){
                    layoutMain.setVisibility(View.VISIBLE);
                }
            }
            if(Ad.getTypeAd()==null){
                waitStatusTypeAd = new WaitStatus();
                threadAdWaitStatus = new Thread(waitStatusTypeAd);
            }
        }
        public InterstitialAd getObjInterstitialAd() {
            return objInterstitialAd;
        }

        public void prepareInterstitialAd(){
            if(objInterstitialAd != null){
                if(!objInterstitialAd.isLoaded()) {quickReloadIntestitial();}

            }else {
                objInterstitialAd = new InterstitialAd(context);
                loadInterstitialAd();
            }

        }

        private void quickReloadIntestitial(){
            if(objInterstitialAd != null){
                if(Ad.this.objInterstitialAd.getAdUnitId()==null){Ad.this.objInterstitialAd.setAdUnitId(context.getResources().getString(R.string.admob_interstitial));}
                Ad.this.objInterstitialAd.loadAd(Ad.this.getAdRequest());
            }else{
                prepareInterstitialAd();
            }

        }
        private void loadInterstitialAd() {
            if(getTypeAd()==null){
                if(handler == null) handler = new AdHandler(this);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Thread curThread = Thread.currentThread();
                        while (true){
                            try {
                                Thread.sleep(100);
                                if(Ad.getTypeAd()!=null){
                                    handler.sendEmptyMessage(INTRESTITIAL);
                                    return;
                                }
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                    }
                }).start();
            }else{
                objInterstitialAd.setAdUnitId(context.getResources().getString(R.string.admob_interstitial));
                AdRequest adRequest = getAdRequest();
                objInterstitialAd.loadAd(adRequest);
                objInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {

                        interstitialErrorLoad = true;
                        quickReloadIntestitial();

                        if(Ad.this.nextIntent != null){

                            gotoActivityAfterIntestitial();
                        }

                    }
                    @Override
                    public void onAdClosed() {
                        quickReloadIntestitial();
                        gotoActivityAfterIntestitial();
                    }
                });
            }
        }
        void interstitialShow(Intent nextIntent){
            this.nextIntent = nextIntent;
            interstitialErrorLoad=false;
            layoutMain.setVisibility(View.INVISIBLE);
            if(objInterstitialAd.isLoaded()){
                if(!notShowAds()){
                    objInterstitialAd.show();
                }else{

                    gotoActivityAfterIntestitial();
                }

            }else {
                if(handler == null) handler = new AdHandler(Ad.this);
                Thread interstitial = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Thread threadCurrent = Thread.currentThread();
                        handler.setInterstitialWaitThread(threadCurrent);
                        int limitTimeWaitingAd = 100;
                        while (limitTimeWaitingAd>0){
                            handler.sendEmptyMessage(Ad.INTRESTITIAL_SHOW);

                            if(!threadCurrent.isInterrupted()){

                                if(Ad.this.interstitialErrorLoad){

                                    return;
                                }
                                try {
                                    Thread.sleep(100);

                                } catch (InterruptedException e) {

                                    return;
                                }
                            }else {
                                return;
                            }
                            limitTimeWaitingAd--;
                        }
                        handler.sendEmptyMessage(NEXT_ACTIVITY_SHOW);


                    }
                });
                interstitial.start();
            }
        }
        void bannerShow(int bannerId){
            this.bannerId = bannerId;
            if(isBannerLoad()){
                return;
            }
            bannerShow();
        }
        void bannerShow(){
            if(Ad.getTypeAd()==null){
                if(handler == null) handler = new AdHandler(this);
                try{
                    if(!threadAdWaitStatus.isAlive()){
                        threadAdWaitStatus.start();
                    }
                }catch (NullPointerException e){
                    waitStatusTypeAd = new WaitStatus();
                    threadAdWaitStatus = new Thread(waitStatusTypeAd);
                    threadAdWaitStatus.start();
                }
            }else{
                if(!GDPR.isGDPRrequired){
                    gdpr = new GDPR(context, layoutMain, this);
                    return;
                }

                if(!notShowAds()){
                    AdView adView;
                    if(layoutWrapperBannerAd.findViewById(bannerId) != null){
                        adView = (AdView)layoutWrapperBannerAd.findViewById(bannerId);
                        AdRequest adRequest = getAdRequest();
                        adView.loadAd(adRequest);
                        return;
                    }

                    adView = new AdView(context);
                    adView.setAdSize(AdSize.BANNER);
                    if(bannerId != 0){
                        adView.setId(bannerId);
                    }
                    adView.setAdUnitId(context.getResources().getString(R.string.admob_banner));
                    layoutWrapperBannerAd.addView(adView);

                    AdRequest adRequest = getAdRequest();
                    adView.loadAd(adRequest);
                    adView.setAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            bannerLoad = true;
                        }
                        //@Override public void onAdFailedToLoad(int errorCode) {}@Override public void onAdOpened() {}@Override public void onAdLeftApplication() {}@Override public void onAdClosed() {}
                    });
                }else {
                    gdpr = new GDPR(context, layoutMain, this);
                }
            }

        }
        private boolean notShowAds() {
            return GDPR.getCheckedGDPR(context);
        }
        public static void setTypeAd(String typeAd) {
            Ad.typeAd = typeAd;
        }

        static String getTypeAd() {
            return typeAd;
        }

        public static void initialize(Context context) {
            if(!isInitAd){
                MobileAds.initialize(context, context.getResources().getString(R.string.admob_publisher_id));
                isInitAd = true;
            }
        }
        private AdRequest getAdRequest(){
            AdRequest adRequest = new AdRequest.Builder().build();
            if(getTypeAd().equals(GDPR.NON_PERSONAL_ADS)){
                Bundle extras = new Bundle();
                extras.putString("npa", "1");

                adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                        .build();
            }
            return adRequest;

        }

        static void gotoActivity(Context context, Intent intent , Ad ad){
            context.startActivity(intent);
        }
        private boolean isBannerLoad(){
            return bannerLoad;
        }
        private void  gotoActivityAfterIntestitial(){

            if(nextIntent != null){
                context.startActivity(nextIntent);
                nextIntent = null;
            }
            Handler handlerInner = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    layoutMain.setVisibility(View.VISIBLE);
                }
            };

            handlerInner.postDelayed(runnable, 700);
        }

        class WaitStatus implements Runnable{
            @Override
            public void run() {
                Thread currentThread = Thread.currentThread();
                while(true){
                    if(!currentThread.isInterrupted()){
                        try {

                            Thread.sleep(100);
                            if(Ad.getTypeAd()!=null){
                                handler.sendEmptyMessage(BANNER);
                                return;
                            }
                        } catch (InterruptedException e) {
                            return;
                        }
                    }else {
                        return;
                    }
                }
            }
        }
        static class AdHandler extends Handler{

            Ad adObj;
            Thread interstitialWaitThread;
            AdHandler(Ad adObj) {
                this.adObj = adObj;
            }
            public void setInterstitialWaitThread(Thread interstitialWaitThread) {
                this.interstitialWaitThread = interstitialWaitThread;
            }
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == BANNER) {adObj.bannerShow();}
                if(msg.what == INTRESTITIAL_SHOW){
                    if(adObj.objInterstitialAd.isLoaded()){
                        interstitialWaitThread.interrupt();
                        if(!adObj.notShowAds()){
                            adObj.objInterstitialAd.show();
                        }else{
                            adObj.gotoActivityAfterIntestitial();
                        }
                    }
                    return;
                }
                if(msg.what == NEXT_ACTIVITY_SHOW){
                    adObj.quickReloadIntestitial();
                    adObj.gotoActivityAfterIntestitial();
                }
                if(msg.what == INTRESTITIAL){adObj.loadInterstitialAd();}
                adObj.handler=null;
            }

        }

    }

