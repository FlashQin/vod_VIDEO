package cn.mahua.vod.jiexi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cn.mahua.vod.App;
import cn.mahua.vod.bean.JieXiPlayBean;
import cn.mahua.vod.utils.AgainstCheatUtil;
import cn.mahua.vod.utils.OkHttpUtils;
import okhttp3.Call;
import okhttp3.Response;

@SuppressLint("SetJavaScriptEnabled")
public class JieXiWebView extends WebView {
    private static final String TAG = "JieXi--";
    private int index = 0;
    private String[] parseUrls;
    private String url;
    private Map<String, String> headers = new Map<String, String>() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(@Nullable Object o) {
            return false;
        }

        @Override
        public boolean containsValue(@Nullable Object o) {
            return false;
        }

        @Nullable
        @Override
        public String get(@Nullable Object o) {
            return null;
        }

        @Nullable
        @Override
        public String put(String s, String s2) {
            return null;
        }

        @Nullable
        @Override
        public String remove(@Nullable Object o) {
            return null;
        }

        @Override
        public void putAll(@NonNull Map<? extends String, ? extends String> map) {

        }

        @Override
        public void clear() {

        }

        @NonNull
        @Override
        public Set<String> keySet() {
            return null;
        }

        @NonNull
        @Override
        public Collection<String> values() {
            return null;
        }

        @NonNull
        @Override
        public Set<Entry<String, String>> entrySet() {
            return null;
        }
    };
    private BackListener mBackListener = new BackListener() {
        @Override
        public void onSuccess(String url, int curParseIndex, Map<String, String> headers) {

        }

        @Override
        public void onError() {

        }

        @Override
        public void onProgressUpdate(String msg) {

        }
    };
    private int time = 8;//???????????????8s
    private Timer timer;
    private TimerTask task;
    private Handler handler = new Handler();
    private Context mContext;
    static Handler mainHanlder = new Handler(Looper.getMainLooper());

    public JieXiWebView(Context context, String parses, String url, int curParseIndex, BackListener backListener) {
        super(context);
        mContext = context;
        if (parses.contains(",")) {
            parseUrls = parses.split(",");
        } else {
            parseUrls = new String[1];
            parseUrls[0] = parses;
        }
        this.url = url;
        if (curParseIndex >= parseUrls.length) {
            backListener.onError();
        } else {
            index = curParseIndex;
            mBackListener = backListener;
            initSetting();
            if (timer == null) {
                timer = new Timer();
                task = new TimerTask() {
                    @Override
                    public void run() {
                        if (mBackListener != null) {
                            mBackListener.onProgressUpdate("?????????????????? " + (8 - time + 1) + "s");
                        }
                        if (--time <= 0) {
                            time = 8;
                            parstNextOrComplete();
                        }
                    }
                };
                timer.schedule(task, 0, 1000);
            }
        }

    }

    @Override
    public void destroy() {
        super.destroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        mBackListener = null;
    }

    public void startParse() {
        if (isWifiProxy() || isVpnUsed()) {
            AgainstCheatUtil.INSTANCE.showWarn(null);
            return;
        }
        String realUrl = parseUrls[index] + url;
        if (realUrl.contains("json") || realUrl.contains("..")) {
            realUrl = realUrl.replaceFirst("\\.\\.", "\\.");
            Log.d(TAG, "startParse: ??????json??????" + url);
            getJsonResult(realUrl);
        } else {
            Log.d(TAG, "startParse: ??????webview??????" + url);
            String finalRealUrl = realUrl;
            handler.post(() -> loadUrl(finalRealUrl));
        }
    }

    private void initSetting() {
        setClickable(true);
        setWebViewClient(webViewClient);
        WebSettings webSetting = getSettings();
        webSetting.setJavaScriptEnabled(true);
        webSetting.setBuiltInZoomControls(true);
        webSetting.setJavaScriptCanOpenWindowsAutomatically(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setAllowFileAccess(true);
        webSetting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        webSetting.setSupportZoom(true);
        webSetting.setUseWideViewPort(true);
        webSetting.setSupportMultipleWindows(true);
        webSetting.setAppCacheEnabled(false);
        webSetting.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSetting.setGeolocationEnabled(true);
        webSetting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSetting.setSupportMultipleWindows(true);
    }


    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            headers = request.getRequestHeaders();
            Log.i(TAG, "?????????url???" + url);
            if (url.contains(".mp4") || url.contains(".m3u8") || url.contains("/m3u8?") || url.contains(".flv")) {
                if(url.indexOf("=http")==-1 && url.indexOf("=https")==-1 && url.indexOf("=https%3A%2F")==-1 && url.indexOf("=http%3A%2F")==-1) {
                        //ToastUtils.showShort("webview1????????????????????????"+ url);
                    Log.i(TAG, "webview????????????????????????" + url);
                    if (mBackListener != null) {
                        timer.cancel();
                        timer = null;
                        time = 8;
                        mBackListener.onSuccess(url, index,headers);
                        mBackListener = null;//???????????????????????????????????????
                    }
                }
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            //super.onReceivedSslError(view, handler, error);
            handler.proceed();// ???????????????????????????
        }
    };

    private void parstNextOrComplete() {
        if (++index < parseUrls.length) {
            startParse();
        } else {
            time = 8;
            timer.cancel();
            task = null;
            timer = null;
            if (mBackListener != null)
                mBackListener.onError();
        }
    }


    private void getJsonResult(String url) {
        OkHttpUtils.getInstance()
                .getDataAsynFromNet(url, new OkHttpUtils.MyNetCall() {
                    @Override
                    public void success(Call call, Response response) throws IOException {
                        boolean isSuccessful = response.isSuccessful();
                        if (isSuccessful) {
                            try {
                                JieXiPlayBean playBean = GsonUtils.fromJson(response.body().string(), JieXiPlayBean.class);
                                if (playBean.getCode().equals("200")) {
                                    Log.i(TAG, "json????????????????????????" + url + "playurl==>" + playBean.getUrl());
                                    mainHanlder.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mBackListener != null) {
                                                if (timer != null) {
                                                    timer.cancel();
                                                    timer = null;
                                                }
                                                time = 8;
                                                if (playBean.getUrl().contains(".m3u8") || playBean.getUrl().contains("/m3u8?") || playBean.getUrl().contains(".mp4") || playBean.getUrl().contains(".flv")) {
                                                    if(playBean.getUrl().indexOf("=http")==-1 && playBean.getUrl().indexOf("=https")==-1 && playBean.getUrl().indexOf("=https%3A%2F")==-1 && playBean.getUrl().indexOf("=http%3A%2F")==-1) {
                                                        String playUrl = playBean.getUrl();
                                                    if (playUrl.contains("http") || playUrl.contains("https")) {
                                                        playBean.setUrl(playUrl);
                                                    } else {
                                                        playBean.setUrl("https:" + playUrl);
                                                    }
                                                    Log.i(TAG, "json?????????????????????22???" + url + "playurl==>" + playBean.getUrl());
                                                }
                                            }

                                                mBackListener.onSuccess(playBean.getUrl(), index,null);
                                                mBackListener = null;//???????????????????????????????????????
                                            }
                                        }
                                    });
                                } else {
                                    Log.i(TAG, "json????????? ???????????????");
                                    time = 0;
                                }
                            } catch (Exception e) {
                                Log.i(TAG, "json???????????????" + e);
                                time = 0;
                            }
                        } else {
                            Log.i(TAG, "json??????????????????200");
                            time = 0;
                        }
                    }

                    @Override
                    public void failed(Call call, IOException e) {
                        Log.i(TAG, "json???????????????" + e);
                    }
                });
    }

    private boolean isWifiProxy() {
        final boolean IS_ICS_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
        String proxyAddress;
        int proxyPort;
        if (IS_ICS_OR_LATER) {
            proxyAddress = System.getProperty("http.proxyHost");
            String portStr = System.getProperty("http.proxyPort");
            proxyPort = Integer.parseInt((portStr != null ? portStr : "-1"));
        } else {
            proxyAddress = android.net.Proxy.getHost(App.getInstance().getContext());
            proxyPort = android.net.Proxy.getPort(App.getInstance().getContext());
        }
        return (!TextUtils.isEmpty(proxyAddress)) && (proxyPort != -1);
        //return false;
    }

    /**
     * ????????????????????????VPN????????????????????????true,????????????flase
     */
    public static boolean isVpnUsed() {
        try {
            Enumeration niList = NetworkInterface.getNetworkInterfaces();
            if (niList != null) {
                for (Object f : Collections.list(niList)) {
                    NetworkInterface intf = (NetworkInterface) f;
                    if (!intf.isUp() || intf.getInterfaceAddresses().size() == 0) {
                        continue;
                    }
                    Log.d("-----", "isVpnUsed() NetworkInterface Name: " + intf.getName());
                    if ("tun0".equals(intf.getName()) || "ppp0".equals(intf.getName())) {
                        return true; // The VPN is up
                        //return false;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

}
