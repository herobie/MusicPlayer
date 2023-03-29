package com.example.musicplayer.cookieStore;

import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * CookieJarImpl
 * 重写了CookieJar用于接管对Cookie的持久化操作（仅进行接管操作，对Cookie的持久化的实现在PersistentCookieStore类中进行）
 */
public class CookieJarImpl implements CookieJar {

    private CookieStore cookieStore;

    //构造方法，获取CookieStore接口对象
    public CookieJarImpl(CookieStore cookieStore) {
        if(cookieStore == null) {
            throw new IllegalArgumentException("cookieStore can not be null.");
        }
        this.cookieStore = cookieStore;
    }

    //用CookieStore接管对Cookie的保存功能
    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        this.cookieStore.add(url, cookies);
    }

    //接管对Cookie的导入功能，即在请求是自动按url使用对应cookie
    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        return this.cookieStore.get(url);
    }

    public CookieStore getCookieStore() {
        return this.cookieStore;
    }
}
