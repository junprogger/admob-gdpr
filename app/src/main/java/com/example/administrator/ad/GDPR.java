package com.example.administrator.ad;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.consent.DebugGeography;

import java.net.MalformedURLException;
import java.net.URL;

public class GDPR {
    private final static String FILE_NAME = "settings_ad";
    private String[] publishIds;
    public static final String PERSONAL_ADS = "0";
    public static final String NON_PERSONAL_ADS = "1";
    public final static String EAA_USER = "1";
    public final static String NOT_EAA_USER = "2";
    public final static String UNKNOW_USER = "3";
    private static String userEAA = UNKNOW_USER;
    //for test
    private static final String ID_DEVICE = "";

    static boolean isGDPRrequired = false;

    private static boolean statusConsentError = false;
    private URL urlAppPolicy = null;
    private Context context;
    private ViewGroup contentView;
    private Ad ad;
    private static boolean formOpen = false;
    ConsentInformation consentInformation;
    ConsentForm form;

    GDPR(Context context, ViewGroup contentView, Ad ad){
        this(context);
        this.contentView = contentView;
        this.ad = ad;
    }
    GDPR(Context context, ViewGroup contentView){
        this(context);
        this.contentView = contentView;
    }
    GDPR(Context context) {
        this.context = context;
        publishIds = new String[]{context.getResources().getString(R.string.admob_publisher_id)};
        consentInformation = ConsentInformation.getInstance(context);
        checkConsentStatus();
    }
    /*sharedPreference*/
    static void setEaaUser(Context context, String eaaStaus){
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("isEaaUser", eaaStaus);
        editor.apply();
    }
    static String getEaaUser(Context context){
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);

        return sp.getString("isEaaUser", GDPR.UNKNOW_USER);
    }
    void checkConsentStatus(){
        consentInformation.requestConsentInfoUpdate(publishIds, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                statusConsentError = false;

                if(ConsentInformation.getInstance(context).isRequestLocationInEeaOrUnknown()){
                    userEAA = EAA_USER;
                    setEaaUser(context, EAA_USER);
                    if(consentStatus.equals(ConsentStatus.PERSONALIZED)){
                        setPersonalAdsType();
                        GDPR.isGDPRrequired = true;
                        if(ad != null){ad.bannerShow();}
                    }
                    if(consentStatus.equals(ConsentStatus.NON_PERSONALIZED)){
                        setNonPersonalAdsType();
                        GDPR.isGDPRrequired = true;
                        if(ad != null){ad.bannerShow();}
                    }
                    if(consentStatus.equals(ConsentStatus.UNKNOWN)){
                        if(!formOpen){
                            creteFormGDPR();
                        }
                    }
                }else {
                    userEAA = NOT_EAA_USER;
                    setEaaUser(context, NOT_EAA_USER);
                    setPersonalAdsType();
                    GDPR.isGDPRrequired = true;
                    if(ad != null){ad.bannerShow();}

                }
            }
            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                errorHandlerGDPR();

            }
        });
    }

    public void creteFormGDPR() {
        try {
            urlAppPolicy = new URL(context.getResources().getString(R.string.admob_policy));

        } catch (MalformedURLException e) {
            errorHandlerGDPR();
            return;
        }
        if (form == null) {
            form = new ConsentForm.Builder(context, urlAppPolicy)
                    .withListener(new ConsentFormListener() {
                        @Override
                        public void onConsentFormLoaded() {
                            GDPR.this.form.show();
                        }

                        @Override
                        public void onConsentFormOpened() {
                            formOpen = true;
                        }

                        @Override
                        public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                            formOpen = false;
                            GDPR.isGDPRrequired = true;

                            if(ad != null){ad.bannerShow();}
                            if(consentStatus.equals(ConsentStatus.PERSONALIZED)){
                                setPersonalAdsType();
                            }
                            if(consentStatus.equals(ConsentStatus.NON_PERSONALIZED)){
                                setNonPersonalAdsType();
                            }
                            if(consentStatus.equals(ConsentStatus.UNKNOWN)){
                                errorHandlerGDPR();
                            }
                        }

                        @Override
                        public void onConsentFormError(String errorDescription) {
                            errorHandlerGDPR();
                        }
                    })
                    .withPersonalizedAdsOption()
                    .withNonPersonalizedAdsOption()
                    .build();
        }
        form.load();
    }

    private void setPersonalAdsType() {
        Ad.setTypeAd(PERSONAL_ADS);
        if(contentView !=null) contentView.setVisibility(View.VISIBLE);
    }
    private void setNonPersonalAdsType() {
        Ad.setTypeAd(NON_PERSONAL_ADS);
        if(contentView !=null) contentView.setVisibility(View.VISIBLE);
    }
    private void testGDPR(boolean eaa){
        ConsentInformation.getInstance(context).addTestDevice(ID_DEVICE);
        if(eaa) ConsentInformation.getInstance(context).setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        else ConsentInformation.getInstance(context).setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA);
    }
    private void errorHandlerGDPR(){
        statusConsentError = true;
        if (getEaaUser(context).equals(NOT_EAA_USER)){
            setPersonalAdsType();
        }else{
            setNonPersonalAdsType();
        }
    }

    public static String getUserEAA(Context context) {
        if(userEAA.equals(UNKNOW_USER)){
            return getEaaUser(context);
        }
        return userEAA;
    }
    public static boolean getCheckedGDPR(Context context){
        if((getUserEAA(context).equals(UNKNOW_USER) || getUserEAA(context).equals(EAA_USER)) && statusConsentError){
            return true;
        }
        return false;
    }
}
