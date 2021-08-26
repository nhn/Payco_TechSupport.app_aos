package com.payco.paycopaymentdemo

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.payco.paycopaymentdemo.utils.ClientType
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        // 테스트를 위한 샘플 가맹점 url
        private const val SHOP_URL = "https://devcenter.payco.com/demo/easyPay2"
    }

    private lateinit var mWebView: WebView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mWebView = findViewById<View>(R.id.web_view) as WebView
        setupWebView()

        mWebView.loadUrl(SHOP_URL)
    }

    override fun onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * 페이코 결제 연동을 위한 웹뷰 설정
     */
    private fun setupWebView() {
        mWebView.clearCache(true)
        mWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        configurationWebView(mWebView)
    }

    /**
     * 웹뷰의 WebSetting 설정하기
     * @param webView 설정하고자 하는 WebView 인스턴스
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun configurationWebView(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.builtInZoomControls = true
        webView.settings.setSupportZoom(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // http -> https 호출 허용.
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // 서드파티 쿠키 허용.
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        mWebView.webChromeClient = WebChromeClient()
        mWebView.webViewClient = PaycoPaymentWebViewClient()
    }

    private inner class PaycoPaymentWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String) =
            when (ClientType.getClientType(url)) {
                // about:blank, javascript: 로 시작하는 url을 처리
                ClientType.BLANK,
                ClientType.JAVASCRIPT -> {
                    true
                }

                // http, https 프로토콜로 시작하는 일반적인 웹 주소를 처리
                ClientType.WEB -> {
                    false
                }

                // url에 대한 처리
                else -> {
                    handleAppUrl(url)
                }
            }
    }

    private fun handleAppUrl(url: String): Boolean {
        // 전화 걸기 처리
        if (handleTelShouldOverrideUrlLoading(url)) {
            return true
        }

        // intent 처리
        if (handleIntentShouldOverrideUrlLoading(url)) {
            return true
        }

        // Play Store url 처리
        if (storeShouldOverrideUrlLoading(url)) {
            return true
        }

        // 기타 url 처리
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return true
        } catch (e: Exception) {
            // 앱이 설치되어 있지 않을 경우 예외가 발생합니다. 이 부분은 업체에 맞게 구현해야 합니다.
            Toast.makeText(this@MainActivity, getString(R.string.install_application_message), Toast.LENGTH_LONG).show()
        }
        return false
    }

    /**
     * 전화걸기 uri 처리 - ARS 인증을 위한 전화 연결 등
     * @param url     처리하고자 하는 url
     * @return        uri 처리 여부. WebViewClient.shouldOverrideUrlLoading가 반환할 값을 반환
     */
    private fun handleTelShouldOverrideUrlLoading(url: String): Boolean {
        if (url.toLowerCase().startsWith("tel:")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            return true
        }

        return false
    }

    /**
     * intent 프로토콜로 시작하는 uri를 처리하는 메소드로 앱 설치 여부를 확인하고, 설치된 앱이 없을 경우
     * 플레이 스토어로 이동합니다.
     * @param url   처리할 uri
     * @return      url 처리 결과
     */
    private fun handleIntentShouldOverrideUrlLoading(url: String): Boolean {
        if (url.startsWith("intent")) {
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                if (intent.resolveActivity(packageManager) != null) {
                    // 처리할 수 있는 패키지(실행할 수 있는 앱)이 있을 경우
                    startActivity(intent)
                    return true
                }

                // 처리할 수 없는 패키지일 경우 플레이 스토어로 이동
                // 단 intent의 getPackage() 메소드로 패키지 정보가 가져올 수 있을 때에만 플레이 스토어로
                // 이동할 수 있습니다.
                val marketIntent = Intent(Intent.ACTION_VIEW)
                marketIntent.data = Uri.parse("market://details?id=" + intent.getPackage())
                if (marketIntent.resolveActivity(packageManager) != null) {
                    startActivity(marketIntent)
                    return true
                }
            } catch (uriEx: URISyntaxException) {
                Log.e(TAG, "URISyntaxException=[" + uriEx.message + "]")
                return false
            }
        }

        return false
    }

    /**
     * market 프로토콜로 시작하는 url의 처리. 플레이스토어 이동으로 사용하는 url을 처리합니다.
     * @param url   처리할 url
     * @return      url 처리 결과
     */
    private fun storeShouldOverrideUrlLoading(url: String): Boolean {
        if (url.startsWith("market")) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            return true
        }

        return false
    }

}
