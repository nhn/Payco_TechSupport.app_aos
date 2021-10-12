# 안드로이드 PAYCO 결제 데모

PAYCO APP 결제 연동을 위한 안드로이드 데모 앱과 가이드 입니다.
* Github: https://github.com/nhn/Payco_TechSupport.app_aos

## 안드로이드 적용 가이드

### AndroidManifest.xml

1. 인터넷 사용을 위해 인터넷 권한이 필요합니다. 

    ```xml
    <uses-permission android:name="android.permission.INTERNET"/>
    ```

2. `http://`로 제공하는 웹사이트의 열람을 위해 `<application/>` 태그에 `android:usesCleartextTraffic="true"` 속성을 설정합니다.
    안드로이드 9(Pie)부터는 모든 HTTP 트래픽을 강제 차단합니다.
    
    * https://android-developers.googleblog.com/2018/04/protecting-users-with-tls-by-default-in.html

    ```xml
    <application
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:usesCleartextTraffic="true"
        android:theme="@style/AppTheme">
        ...
    </application>
    ```

3. targetSdkVersion=30인 앱에서는 `<queris>` 속성을 설정합니다.

    * https://developer.android.com/training/basics/intents/package-visibility

    ```xml
    <queries>
        <package android:name="com.nhnent.payapp" />
    </queries>
    ```

    안드로이드11 기기 & targetSdkVersion=30인 앱에서 위 속성 선언이 없는 경우,
    페이코 앱이 설치되어있는 기기에서 페이코 결제 링크를 클릭하면 연결할 수 없는 링크로 판단하여 페이코 앱 설치를 안내하는 스토어로 링크가 열립니다.

    따라서, targetSdkVersion=30인 앱에서는 반드시 위 속성을 설정해야합니다.



### MainActivity

데모 예제에서 `MainActivity`는 상점의 url을 표시하는 `WebView`를 포함한 액티비티입니다. 이 액티비티는 PAYCO APP 결제 연동을 할 수 있도록 `WebView` 인스턴스를 설정하는 방법을 다룹니다.

#### WebView 설정

1. `WebView`의 `WebSettings` 설정
    
    * `WebView`가 자바스크립트를 사용할 수 있도록 설정합니다.
        ```java
        webView.getSettings().setJavaScriptEnabled(true);
        ```
    
    * http에서 https를 호출할 수 있도록 설정합니다.
        ```java
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ```

2. 서드파티 쿠키 사용 설정

    ```java
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptCookie(true);
    cookieManager.setAcceptThirdPartyCookies(webView, true);
    ```

#### url 처리

url을 처리할 수 있도록 `WebViewClient` 클래스를 상속받은 클래스를 새로 작성합니다. 그리고 `shouldOverrideUrlLoading()` 메소드를 오버라이드 하여 웹뷰로 인입이 될 url을 처리할 수 있도록 합니다.

아래의 코드 예제들은 오버라이드 한 `shouldOverrideUrlLoading()` 메소드 내부에 작성하는 코드입니다. 자세한 것은 데모 코드를 참고하세요.

> **참고:** `ClientType` 클래스는 코드 작성의 편의를 위해 만든 `enum` 입니다. `about:blank`나 `javascript:` 로 시작하는 유효하지 않은 url을 처리하기 위한 것입니다.

1. `javascript:` 또는 `about:blank`의 url을 처리합니다.

    ```java
    // about:blank, javascript: 로 시작하는 url을 처리
    if (clientType.equals(ClientType.BLANK) || clientType.equals(ClientType.JAVASCRIPT)) {
        return true;
    }
    ```

2. `http` 또는 `https`로 시작하는 url을 처리합니다.

    ```java
    // http, https 프로토콜로 시작하는 일반적인 웹 주소를 처리
    if (clientType.equals(ClientType.WEB)) {
        return false;
    }
    ```

3. 그외 url을 처리하는 `handleAppUrl(url)` 메소드

    이 메소드는 다음과 같은 url 형태들을 처리할 수 있도록 되어 있습니다. 데모의 코드는 페이코 앱과 연동을 하기 위한 기본적인 기능들을 구현하였습니다.
    이 메소드 내용을 참고하여 상점 앱의 필요에 맞게 추가 및 수정하여 구현해주세요.

    * 전화걸기(`tel:`)
    * intent(`intent:`)
    * Play Store 관련(`market:`)
    * 기타
    * 예외 처리
