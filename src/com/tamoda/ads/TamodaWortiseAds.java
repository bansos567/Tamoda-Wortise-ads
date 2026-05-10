package com.tamoda.ads;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;

// Import mesin SDK Wortise
import com.wortise.ads.WortiseSdk;
import com.wortise.ads.banner.BannerAd;
import com.wortise.ads.interstitial.InterstitialAd;
import com.wortise.ads.rewarded.RewardedAd;
import com.wortise.ads.appopen.AppOpenAd;

@DesignerComponent(
    version = 1,
    description = "Tamoda Wortise Ads V1 - (App Open, Banner, Interstitial, Rewarded Video)",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = ""
)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = 
    "android.permission.INTERNET, " +
    "android.permission.ACCESS_NETWORK_STATE, " +
    "com.google.android.gms.permission.AD_ID"
)
public class TamodaWortiseAds extends AndroidNonvisibleComponent {

    private final Activity activity;

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private AppOpenAd appOpenAd;
    private BannerAd bannerAd;

    public TamodaWortiseAds(ComponentContainer container) {
        super(container.$form());
        this.activity = (Activity) container.$context();
    }

    // ==========================================
    // 1. INISIALISASI MESIN (Tanpa Lambda agar Lolos Java 7)
    // ==========================================
    @SimpleFunction(description = "Nyalakan mesin Wortise. Masukkan App ID dari dashboard.")
    public void InitializeSdk(String appId) {
        WortiseSdk.initialize(activity, appId, new WortiseSdk.OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete() {
                SdkInitialized();
            }
        });
    }

    @SimpleEvent(description = "Sinyal: SDK berhasil nyala dan siap dipakai.")
    public void SdkInitialized() {
        EventDispatcher.dispatchEvent(this, "SdkInitialized");
    }

    // ==========================================
    // 2. REWARDED VIDEO
    // ==========================================
    @SimpleFunction(description = "Load Video ke memori HP.")
    public void LoadRewarded(String adUnitId) {
        rewardedAd = new RewardedAd(activity, adUnitId);
        rewardedAd.setListener(new RewardedAd.Listener() {
            @Override
            public void onRewardedLoaded(RewardedAd ad) { RewardedLoaded(); }
            @Override
            public void onRewardedFailedToLoad(RewardedAd ad, com.wortise.ads.AdError error) { RewardedFailed(error.getMessage()); }
            @Override
            public void onRewardedCompleted(RewardedAd ad, com.wortise.ads.rewarded.Reward reward) { 
                RewardedEarned(reward.getAmount(), reward.getLabel() != null ? reward.getLabel() : "poin"); 
            }
            @Override
            public void onRewardedClosed(RewardedAd ad) { RewardedClosed(); }
            @Override public void onRewardedShown(RewardedAd ad) {}
            @Override public void onRewardedFailedToShow(RewardedAd ad, com.wortise.ads.AdError error) {}
            @Override public void onRewardedClicked(RewardedAd ad) {}
        });
        rewardedAd.load();
    }

    @SimpleFunction(description = "Munculkan video yang sudah di-load.")
    public void ShowRewarded() {
        if (rewardedAd != null && rewardedAd.isAvailable()) {
            rewardedAd.show();
        } else {
            RewardedFailed("Video belum ready.");
        }
    }

    @SimpleEvent public void RewardedLoaded() { EventDispatcher.dispatchEvent(this, "RewardedLoaded"); }
    @SimpleEvent public void RewardedFailed(String errorMessage) { EventDispatcher.dispatchEvent(this, "RewardedFailed", errorMessage); }
    @SimpleEvent public void RewardedClosed() { EventDispatcher.dispatchEvent(this, "RewardedClosed"); }
    
    // TYPO SUDAH DI-FIX DISINI BOS
    @SimpleEvent 
    public void RewardedEarned(int amount, String rewardType) { 
        EventDispatcher.dispatchEvent(this, "RewardedEarned", amount, rewardType); 
    }

    // ==========================================
    // 3. INTERSTITIAL
    // ==========================================
    @SimpleFunction(description = "Load iklan Interstitial.")
    public void LoadInterstitial(String adUnitId) {
        interstitialAd = new InterstitialAd(activity, adUnitId);
        interstitialAd.setListener(new InterstitialAd.Listener() {
            @Override
            public void onInterstitialLoaded(InterstitialAd ad) { InterstitialLoaded(); }
            @Override
            public void onInterstitialFailedToLoad(InterstitialAd ad, com.wortise.ads.AdError error) { InterstitialFailed(error.getMessage()); }
            @Override
            public void onInterstitialClosed(InterstitialAd ad) { InterstitialClosed(); }
            @Override public void onInterstitialShown(InterstitialAd ad) {}
            @Override public void onInterstitialFailedToShow(InterstitialAd ad, com.wortise.ads.AdError error) {}
            @Override public void onInterstitialClicked(InterstitialAd ad) {}
        });
        interstitialAd.load();
    }

    @SimpleFunction(description = "Tampilkan Interstitial.")
    public void ShowInterstitial() {
        if (interstitialAd != null && interstitialAd.isAvailable()) {
            interstitialAd.show();
        } else {
            InterstitialFailed("Iklan belum siap.");
        }
    }

    @SimpleEvent public void InterstitialLoaded() { EventDispatcher.dispatchEvent(this, "InterstitialLoaded"); }
    @SimpleEvent public void InterstitialFailed(String errorMessage) { EventDispatcher.dispatchEvent(this, "InterstitialFailed", errorMessage); }
    @SimpleEvent public void InterstitialClosed() { EventDispatcher.dispatchEvent(this, "InterstitialClosed"); }

    // ==========================================
    // 4. APP OPEN
    // ==========================================
    @SimpleFunction(description = "Load dan tampilkan App Open.")
    public void ShowAppOpen(String adUnitId) {
        appOpenAd = new AppOpenAd(activity, adUnitId);
        appOpenAd.setListener(new AppOpenAd.Listener() {
            @Override
            public void onAppOpenLoaded(AppOpenAd ad) { 
                ad.show(); 
                AppOpenLoaded();
            }
            @Override
            public void onAppOpenFailedToLoad(AppOpenAd ad, com.wortise.ads.AdError error) { AppOpenFailed(error.getMessage()); }
            @Override
            public void onAppOpenClosed(AppOpenAd ad) { AppOpenClosed(); }
            @Override public void onAppOpenShown(AppOpenAd ad) {}
            @Override public void onAppOpenFailedToShow(AppOpenAd ad, com.wortise.ads.AdError error) {}
            @Override public void onAppOpenClicked(AppOpenAd ad) {}
        });
        appOpenAd.load();
    }

    @SimpleEvent public void AppOpenLoaded() { EventDispatcher.dispatchEvent(this, "AppOpenLoaded"); }
    @SimpleEvent public void AppOpenFailed(String errorMessage) { EventDispatcher.dispatchEvent(this, "AppOpenFailed", errorMessage); }
    @SimpleEvent public void AppOpenClosed() { EventDispatcher.dispatchEvent(this, "AppOpenClosed"); }

    // ==========================================
    // 5. BANNER
    // ==========================================
    @SimpleFunction(description = "Pasang banner di Horizontal / Vertical Arrangement.")
    public void ShowBanner(String adUnitId, AndroidViewComponent containerLayout) {
        View view = containerLayout.getView();
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            viewGroup.removeAllViews();
            
            bannerAd = new BannerAd(activity);
            bannerAd.setAdUnitId(adUnitId);
            bannerAd.setAdSize(com.wortise.ads.banner.AdSize.HEIGHT_50);
            
            bannerAd.setListener(new BannerAd.Listener() {
                @Override
                public void onBannerLoaded(BannerAd ad) { BannerLoaded(); }
                @Override
                public void onBannerFailedToLoad(BannerAd ad, com.wortise.ads.AdError error) { BannerFailed(error.getMessage()); }
                @Override public void onBannerClicked(BannerAd ad) {}
            });
            
            viewGroup.addView(bannerAd);
            bannerAd.load();
        }
    }

    @SimpleEvent public void BannerLoaded() { EventDispatcher.dispatchEvent(this, "BannerLoaded"); }
    @SimpleEvent public void BannerFailed(String errorMessage) { EventDispatcher.dispatchEvent(this, "BannerFailed", errorMessage); }
}
