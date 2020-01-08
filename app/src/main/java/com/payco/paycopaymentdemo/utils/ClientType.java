package com.payco.paycopaymentdemo.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ClientType {
    WEB, JAVASCRIPT, BLANK, APP;

    public static ClientType getClientType(String url) {
        Matcher httpMatcher = Pattern.compile(
                "^(https?):\\/\\/([^:\\/\\s]+)(/?)").matcher(url);
        if (httpMatcher.find()) {
            return ClientType.WEB;

        } else if (url.startsWith("javascript:")) {
            return ClientType.JAVASCRIPT;

        } else if (url.startsWith("about:blank")) {
            return ClientType.BLANK;

        } else {
            return ClientType.APP;
        }
    }
}
