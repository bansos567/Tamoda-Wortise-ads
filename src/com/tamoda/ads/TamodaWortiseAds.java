package com.tamoda.ads;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;

@DesignerComponent(
    version = 2,
    description = "Tamoda Wortise Ads V2 - Native Reflection (App Open, Banner, Interstitial, Rewarded). Lolos compiler Ant!",
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
// KLUE: Wajib ada agar mentahan AAR Wortise ikut terbungkus ke dalam APK Kodular
@UsesLibraries(libraries = "wortise-1.0.5.aar")

public class TamodaWortiseAds extends AndroidNonvisibleComponent {

    private final Activity activity;
    private final Context context;

    // Objek iklan disimpan secara dinamis (Object) agar Ant tidak minta import class Wortise
    private Object interstitialAd;
    private Object rewardedAd;
    private Object appOpenAd;
    private Object bannerAd;

    public TamodaWortiseAds(ComponentContainer container) {
        super(container.$form());
        this.activity = (Activity) container.$context();
        this.context = container.$context();
    }

    // ==========================================
    // 1. INISIALISASI MESIN (Via Reflection)
    // ==========================================
    @SimpleFunction(description = "Nyalakan mesin Wortise. Masukkan App ID dari dashboard.")
    public void InitializeSdk(final String appId) {
        try {
            Class<?> sdkClass = Class.forName("com.wortise.ads.WortiseSdk");
            Class<?> listenerClass = Class.forName("com.wortise.ads.WortiseSdk$OnInitializationCompleteListener");

            Object listenerProxy = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("onInitializationComplete")) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() { SdkInitialized(); }
                            });
                        }
                        return null;
                    }
                }
            );

            Method initMethod = sdkClass.getMethod("initialize", Context.class, String.class, listenerClass);
            initMethod.invoke(null, context, appId, listenerProxy);

        } catch (Exception e) {
            // Abaikan atau lempar ke log jika SDK gagal diinisialisasi
        }
    }

    @SimpleEvent(description = "Sinyal: SDK berhasil nyala dan siap dipakai.")
    public void SdkInitialized() {
        EventDispatcher.dispatchEvent(this, "SdkInitialized");
    }

    // ==========================================
    // 2. REWARDED VIDEO (Via Reflection)
    // ==========================================
    @SimpleFunction(description = "Load Video ke memori HP.")
    public void LoadRewarded(String adUnitId) {
        try {
            Class<?> rewClass = Class.forName("com.wortise.ads.rewarded.RewardedAd");
            Constructor<?> constructor = rewClass.getConstructor(Context.class, String.class);
            rewardedAd = constructor.newInstance(context, adUnitId);

            Class<?> listenerClass = Class.forName("com.wortise.ads.rewarded.RewardedAd$Listener");
            Object listenerProxy = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String mName = method.getName();
                        if (mName.equals("onRewardedLoaded")) {
                            activity.runOnUiThread(new Runnable() { public void run() { RewardedLoaded(); } });
                        } else if (mName.equals("onRewardedFailedToLoad")) {
                            String errMsg = getErrorMessage(args, 1);
                            final String fErr = errMsg;
                            activity.runOnUiThread(new Runnable() { public void run() { RewardedFailed(fErr); } });
                        } else if (mName.equals("onRewardedClosed")) {
                            activity.runOnUiThread(new Runnable() { public void run() { RewardedClosed(); } });
                        } else if (mName.equals("onRewardedCompleted")) {
                            int amt = 0;
                            String lbl = "poin";
                            if (args.length > 1 && args[1] != null) {
                                try {
                                    Object rewardObj = args[1];
                                    Class<?> rewardClass = rewardObj.getClass();
                                    amt = (int) rewardClass.getMethod("getAmount").invoke(rewardObj);
                                    lbl = (String) rewardClass.getMethod("getLabel").invoke(rewardObj);
                                    if (lbl == null) lbl = "poin";
                                } catch (Exception ignored) {}
                            }
                            final int fAmt = amt;
                            final String fLbl = lbl;
                            activity.runOnUiThread(new Runnable() { public void run() { RewardedEarned(fAmt, fLbl); } });
                        }
                        return null;
                    }
                }
            );

            rewClass.getMethod("setListener", listenerClass).invoke(rewardedAd, listenerProxy);
            rewClass.getMethod("load").invoke(rewardedAd);

        } catch (Exception e) {
            RewardedFailed("Load Error: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Munculkan video yang sudah di-load.")
    public void ShowRewarded() {
        try {
            if (rewardedAd != null) {
                Class<?> rewClass = rewardedAd.getClass();
                boolean isAvail = (boolean) rewClass.getMethod("isAvailable").invoke(rewardedAd);
                if (isAvail) {
                    rewClass.getMethod("show").invoke(rewardedAd);
                } else {
                    RewardedFailed("Video belum ready.");
                }
            } else {
                RewardedFailed("Load video terlebih dahulu.");
            }
        } catch (Exception e) {
            RewardedFailed("Show Error: " + e.getMessage());
        }
    }

    @SimpleEvent(description = "Video sukses dimuat ke memori.")
    public void RewardedLoaded() { EventDispatcher.dispatchEvent(this, "RewardedLoaded"); }

    @SimpleEvent(description = "Video gagal dimuat atau gagal tayang.")
    public void RewardedFailed(String errorMessage) { EventDispatcher.dispatchEvent(this, "RewardedFailed", errorMessage); }

    @SimpleEvent(description = "Video selesai ditutup oleh user.")
    public void RewardedClosed() { EventDispatcher.dispatchEvent(this, "RewardedClosed"); }

    @SimpleEvent(description = "Hadiah diberikan setelah video selesai ditonton.")
    public void RewardedEarned(int amount, String rewardType) { 
        EventDispatcher.dispatchEvent(this, "RewardedEarned", amount, rewardType); 
    }

    // ==========================================
    // 3. INTERSTITIAL (Via Reflection)
    // ==========================================
    @SimpleFunction(description = "Load iklan Interstitial.")
    public void LoadInterstitial(String adUnitId) {
        try {
            Class<?> interClass = Class.forName("com.wortise.ads.interstitial.InterstitialAd");
            interstitialAd = interClass.getConstructor(Context.class, String.class).newInstance(context, adUnitId);

            Class<?> listenerClass = Class.forName("com.wortise.ads.interstitial.InterstitialAd$Listener");
            Object listenerProxy = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String mName = method.getName();
                        if (mName.equals("onInterstitialLoaded")) {
                            activity.runOnUiThread(new Runnable() { public void run() { InterstitialLoaded(); } });
                        } else if (mName.equals("onInterstitialFailedToLoad")) {
                            final String fErr = getErrorMessage(args, 1);
                            activity.runOnUiThread(new Runnable() { public void run() { InterstitialFailed(fErr); } });
                        } else if (mName.equals("onInterstitialClosed")) {
                            activity.runOnUiThread(new Runnable() { public void run() { InterstitialClosed(); } });
                        }
                        return null;
                    }
                }
            );

            interClass.getMethod("setListener", listenerClass).invoke(interstitialAd, listenerProxy);
            interClass.getMethod("load").invoke(interstitialAd);

        } catch (Exception e) {
            InterstitialFailed("Load Error: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Tampilkan Interstitial.")
    public void ShowInterstitial() {
        try {
            if (interstitialAd != null) {
                Class<?> interClass = interstitialAd.getClass();
                boolean isAvail = (boolean) interClass.getMethod("isAvailable").invoke(interstitialAd);
                if (isAvail) {
                    interClass.getMethod("show").invoke(interstitialAd);
                } else {
                    InterstitialFailed("Iklan belum siap.");
                }
            } else {
                InterstitialFailed("Load iklan terlebih dahulu.");
            }
        } catch (Exception e) {
            InterstitialFailed("Show Error: " + e.getMessage());
        }
    }

    @SimpleEvent(description = "Interstitial sukses dimuat.")
    public void InterstitialLoaded() { EventDispatcher.dispatchEvent(this, "InterstitialLoaded"); }

    @SimpleEvent(description = "Interstitial gagal dimuat.")
    public void InterstitialFailed(String errorMessage) { EventDispatcher.dispatchEvent(this, "InterstitialFailed", errorMessage); }

    @SimpleEvent(description = "Interstitial ditutup.")
    public void InterstitialClosed() { EventDispatcher.dispatchEvent(this, "InterstitialClosed"); }

    // ==========================================
    // 4. APP OPEN (Via Reflection)
    // ==========================================
    @SimpleFunction(description = "Load dan langsung tampilkan App Open.")
    public void ShowAppOpen(String adUnitId) {
        try {
            Class<?> openClass = Class.forName("com.wortise.ads.appopen.AppOpenAd");
            appOpenAd = openClass.getConstructor(Context.class, String.class).newInstance(context, adUnitId);

            Class<?> listenerClass = Class.forName("com.wortise.ads.appopen.AppOpenAd$Listener");
            Object listenerProxy = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String mName = method.getName();
                        if (mName.equals("onAppOpenLoaded")) {
                            if (args.length > 0 && args[0] != null) {
                                try {
                                    args[0].getClass().getMethod("show").invoke(args[0]);
                                } catch (Exception ignored) {}
                            }
                            activity.runOnUiThread(new Runnable() { public void run() { AppOpenLoaded(); } });
                        } else if (mName.equals("onAppOpenFailedToLoad")) {
                            final String fErr = getErrorMessage(args, 1);
                            activity.runOnUiThread(new Runnable() { public void run() { AppOpenFailed(fErr); } });
                        } else if (mName.equals("onAppOpenClosed")) {
                            activity.runOnUiThread(new Runnable() { public void run() { AppOpenClosed(); } });
                        }
                        return null;
                    }
                }
            );

            openClass.getMethod("setListener", listenerClass).invoke(appOpenAd, listenerProxy);
            openClass.getMethod("load").invoke(appOpenAd);

        } catch (Exception e) {
            AppOpenFailed("Load Error: " + e.getMessage());
        }
    }

    @SimpleEvent(description = "App Open sukses dimuat.")
    public void AppOpenLoaded() { EventDispatcher.dispatchEvent(this, "AppOpenLoaded"); }

    @SimpleEvent(description = "App Open gagal dimuat.")
    public void AppOpenFailed(String errorMessage) { EventDispatcher.dispatchEvent(this, "AppOpenFailed", errorMessage); }

    @SimpleEvent(description = "App Open ditutup.")
    public void AppOpenClosed() { EventDispatcher.dispatchEvent(this, "AppOpenClosed"); }

    // ==========================================
    // 5. BANNER (Via Reflection)
    // ==========================================
    @SimpleFunction(description = "Pasang banner di Horizontal / Vertical Arrangement.")
    public void ShowBanner(String adUnitId, AndroidViewComponent containerLayout) {
        try {
            View view = containerLayout.getView();
            if (view instanceof ViewGroup) {
                final ViewGroup viewGroup = (ViewGroup) view;
                viewGroup.removeAllViews();

                Class<?> bannerClass = Class.forName("com.wortise.ads.banner.BannerAd");
                bannerAd = bannerClass.getConstructor(Context.class).newInstance(context);

                bannerClass.getMethod("setAdUnitId", String.class).invoke(bannerAd, adUnitId);

                // Mengambil enum AdSize.HEIGHT_50 via Reflection
                Class<?> adSizeClass = Class.forName("com.wortise.ads.banner.AdSize");
                Object sizeEnum = adSizeClass.getField("HEIGHT_50").get(null);
                bannerClass.getMethod("setAdSize", adSizeClass).invoke(bannerAd, sizeEnum);

                Class<?> listenerClass = Class.forName("com.wortise.ads.banner.BannerAd$Listener");
                Object listenerProxy = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            String mName = method.getName();
                            if (mName.equals("onBannerLoaded")) {
                                activity.runOnUiThread(new Runnable() { public void run() { BannerLoaded(); } });
                            } else if (mName.equals("onBannerFailedToLoad")) {
                                final String fErr = getErrorMessage(args, 1);
                                activity.runOnUiThread(new Runnable() { public void run() { BannerFailed(fErr); } });
                            }
                            return null;
                        }
                    }
                );

                bannerClass.getMethod("setListener", listenerClass).invoke(bannerAd, listenerProxy);

                viewGroup.addView((View) bannerAd);
                bannerClass.getMethod("load").invoke(bannerAd);
            }
        } catch (Exception e) {
            BannerFailed("Banner Init Error: " + e.getMessage());
        }
    }

    @SimpleEvent(description = "Banner sukses dimuat.")
    public void BannerLoaded() { EventDispatcher.dispatchEvent(this, "BannerLoaded"); }

    @SimpleEvent(description = "Banner gagal dimuat.")
    public void BannerFailed(String errorMessage) { EventDispatcher.dispatchEvent(this, "BannerFailed", errorMessage); }

    // ==========================================
    // FUNGSI BANTUAN INTERNAL (Ambil Pesan Error Wortise)
    // ==========================================
    private String getErrorMessage(Object[] args, int index) {
        if (args != null && args.length > index && args[index] != null) {
            try {
                Object errObj = args[index];
                return (String) errObj.getClass().getMethod("getMessage").invoke(errObj);
            } catch (Exception ignored) {}
        }
        return "Unknown Ad Error";
    }
}
