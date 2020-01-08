package com.payco.paycopaymentdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.payco.paycopaymentdemo.utils.ClientType;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // 테스트를 위한 샘플 가맹점 url
    private static final String SHOP_URL = "https://devcenter.payco.com/demo/easyPay2";

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebView = (WebView) findViewById(R.id.web_view);

        setupWebView();
        mWebView.loadUrl(SHOP_URL);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 페이코 결제 연동을 위한 웹뷰 설정
     */
    private void setupWebView() {
        mWebView.clearCache(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        configurationWebView(mWebView);
    }

    /**
     * 웹뷰의 WebSetting 설정하기
     * @param webView 설정하고자 하는 WebView 인스턴스
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void configurationWebView(@NonNull WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // http -> https 호출 허용.
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

            // 서드파티 쿠키 허용.
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new PaycoPaymentWebViewClient());
    }


    private class PaycoPaymentWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            ClientType clientType = ClientType.getClientType(url);

            // about:blank, javascript: 로 시작하는 url을 처리
            if (clientType.equals(ClientType.BLANK) || clientType.equals(ClientType.JAVASCRIPT)) {
                return true;
            }

            // http, https 프로토콜로 시작하는 일반적인 웹 주소를 처리
            if (clientType.equals(ClientType.WEB)) {
                return false;
            }

            // url에 대한 처리
            return handleAppUrl(url);
        }
    }

    private boolean handleAppUrl(String url) {
        // 전화 걸기 처리
        if (handleTelShouldOverrideUrlLoading(url)) {
            return true;
        }

        // intent 처리
        if (handleIntentShouldOverrideUrlLoading(url)) {
            return true;
        }

        // Play Store url 처리
        if (storeShouldOverrideUrlLoading(url)) {
            return true;
        }

        // 기타 url 처리
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        } catch (Exception e) {
            // 앱이 설치되어 있지 않을 경우 예외가 발생합니다. 이 부분은 업체에 맞게 구현해야 합니다.
            Toast.makeText(MainActivity.this, getString(R.string.install_application_message), Toast.LENGTH_LONG).show();
        }

        return false;
    }

    /**
     * 전화걸기 uri 처리 - ARS 인증을 위한 전화 연결 등
     * @param url     처리하고자 하는 url
     * @return        uri 처리 여부. WebViewClient.shouldOverrideUrlLoading가 반환할 값을 반환
     */
    private boolean handleTelShouldOverrideUrlLoading(String url) {
        if (url.toLowerCase().startsWith("tel:")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
        return false;
    }

    /**
     * intent 프로토콜로 시작하는 uri를 처리하는 메소드로 앱 설치 여부를 확인하고, 설치된 앱이 없을 경우
     * 플레이 스토어로 이동합니다.
     * @param url   처리할 uri
     * @return      url 처리 결과
     */
    private boolean handleIntentShouldOverrideUrlLoading(String url) {
        if (url.startsWith("intent")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    // 처리할 수 있는 패키지(실행할 수 있는 앱)이 있을 경우
                    startActivity(intent);
                    return true;
                }

                // 처리할 수 없는 패키지일 경우 플레이 스토어로 이동
                // 단 intent의 getPackage() 메소드로 패키지 정보가 가져올 수 있을 때에만 플레이 스토어로
                // 이동할 수 있습니다.
                Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                if (marketIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(marketIntent);
                    return true;
                }

            } catch (URISyntaxException uriEx) {
                Log.e(TAG, "URISyntaxException=[" + uriEx.getMessage() + "]");
                return false;
            }
        }

        return false;
    }

    /**
     * market 프로토콜로 시작하는 url의 처리. 플레이스토어 이동으로 사용하는 url을 처리합니다.
     * @param url   처리할 url
     * @return      url 처리 결과
     */
    private boolean storeShouldOverrideUrlLoading(String url) {
        if (url.startsWith("market")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            return true;
        }
        return false;
    }

}