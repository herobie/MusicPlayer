package com.example.musicplayer.cookieStore;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Cookie;
import okhttp3.HttpUrl;


/**
 * PersistentCookieStore
 * 对Cookie的持久化和从本地提取Cookie导入到请求中的操作都在这个类执行
 * 对Cookie的本地存储主要使用SharedPreferences来实现
 */

public class PersistentCookieStore implements CookieStore{

    private static final String LOG_TAG = "PersistentCookieStore";//Log的TAG
    private static final String COOKIE_PREFS = "CookiePrefsFile";//SP存储文件名
    private static final String HOST_NAME_PREFIX = "host_";
    private static final String COOKIE_NAME_PREFIX = "cookie_";
    private final HashMap<String, ConcurrentHashMap<String, Cookie>> cookies;//用哈希表临时存储cookie
    private final SharedPreferences cookiePrefs;
    private boolean omitNonPersistentCookies = false;

    //构造方法
    public PersistentCookieStore(Context context) {
        //初始化SP存储
        this.cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, 0);
        //初始化哈希表存储
        this.cookies = new HashMap<String, ConcurrentHashMap<String, Cookie>>();

        //从SP存储中获取Cookie
        Map tempCookieMap = new HashMap<Object, Object>(cookiePrefs.getAll());
        for (Object key : tempCookieMap.keySet()) {
            if (!(key instanceof String) || !((String) key).contains(HOST_NAME_PREFIX)) {
                continue;
            }

            String cookieNames = (String) tempCookieMap.get(key);
            if (TextUtils.isEmpty(cookieNames)) {
                continue;
            }

            if (!this.cookies.containsKey(key)) {
                this.cookies.put((String) key, new ConcurrentHashMap<String, Cookie>());
            }

            String[] cookieNameArr = cookieNames.split(",");
            for (String name : cookieNameArr) {
                String encodedCookie = this.cookiePrefs.getString("cookie_" + name, null);
                if (encodedCookie == null) {
                    continue;
                }

                Cookie decodedCookie = this.decodeCookie(encodedCookie);
                if (decodedCookie != null) {
                    this.cookies.get(key).put(name, decodedCookie);
                }
            }
        }
        tempCookieMap.clear();

        clearExpired();
    }

    /** 移除失效cookie */
    private void clearExpired() {
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();

        for (String key : this.cookies.keySet()) {
            boolean changeFlag = false;

            for (ConcurrentHashMap.Entry<String, Cookie> entry : cookies.get(key).entrySet()) {
                String name = entry.getKey();
                Cookie cookie = entry.getValue();
                if (isCookieExpired(cookie)) {
                    //移除哈希表中的cookie
                    cookies.get(key).remove(name);

                    //移除SP存储中的Cookie
                    prefsWriter.remove(COOKIE_NAME_PREFIX + name);

                    // 标记，是否完成一个或多个Cookie的清理
                    changeFlag = true;
                }
            }

            // 完成清理后更新SP存储
            if (changeFlag) {
                prefsWriter.putString(key, TextUtils.join(",", cookies.keySet()));
            }
        }

        prefsWriter.apply();
    }

    @Override
    public void add(HttpUrl httpUrl, Cookie cookie) {
        if (omitNonPersistentCookies && !cookie.persistent()) {
            return;
        }

        String name = this.cookieName(cookie);
        String hostKey = this.hostName(httpUrl);

        // 将Cookie保存到本地，且删除过期的cookie
        if(!this.cookies.containsKey(hostKey)) {
            this.cookies.put(hostKey, new ConcurrentHashMap<String, Cookie>());
        }
        cookies.get(hostKey).put(name, cookie);

        // Save cookie into persistent store
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
        // 保存httpUrl对应的所有cookie的name
        prefsWriter.putString(hostKey, TextUtils.join(",", cookies.get(hostKey).keySet()));
        // 保存cookie
        prefsWriter.putString(COOKIE_NAME_PREFIX + name, encodeCookie(new SerializableCookie(cookie)));
        prefsWriter.apply();
    }

    @Override
    public void add(HttpUrl httpUrl, List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            if (isCookieExpired(cookie)) {
                continue;
            }
            this.add(httpUrl, cookie);
        }
    }



    @Override
    public List<Cookie> get(HttpUrl httpUrl) {
        return this.get(this.hostName(httpUrl));
    }

    @Override
    public List<Cookie> getCookies() {
        ArrayList<Cookie> result = new ArrayList<Cookie>();
        for (String hostKey : this.cookies.keySet()) {
            result.addAll(this.get(hostKey));
        }
        return result;
    }

    /** 通过url获取cookie集合 */
    private List<Cookie> get(String hostKey) {
        ArrayList<Cookie> result = new ArrayList<Cookie>();

        if (this.cookies.containsKey(hostKey)) {
            Collection<Cookie> cookies = this.cookies.get(hostKey).values();
            for (Cookie cookie : cookies) {
                if (isCookieExpired(cookie)) {
                    this.remove(hostKey, cookie);
                }
                else {
                    result.add(cookie);
                }
            }
        }
        return result;
    }

    @Override
    public boolean remove(HttpUrl httpUrl, Cookie cookie) {
        return this.remove(this.hostName(httpUrl), cookie);
    }

    /** 从缓存中移除cookie */
    private boolean remove(String hostKey, Cookie cookie) {
        String name = this.cookieName(cookie);
        if (this.cookies.containsKey(hostKey) && this.cookies.get(hostKey).containsKey(name)) {
            // 从内存中移除httpUrl对应的cookie
            this.cookies.get(hostKey).remove(name);

            SharedPreferences.Editor prefsWriter = cookiePrefs.edit();

            // 从本地缓存中移出对应cookie
            prefsWriter.remove(COOKIE_NAME_PREFIX + name);

            // 保存httpUrl对应的所有cookie的name
            prefsWriter.putString(hostKey, TextUtils.join(",", this.cookies.get(hostKey).keySet()));

            prefsWriter.apply();
            return true;
        }
        return false;
    }

    //删除所有Cookie
    @Override
    public boolean removeAll() {
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
        prefsWriter.clear();
        prefsWriter.apply();
        this.cookies.clear();
        return true;
    }

    public void setOmitNonPersistentCookies(boolean omitNonPersistentCookies) {
        this.omitNonPersistentCookies = omitNonPersistentCookies;
    }

    /** 判断cookie是否失效  */
    private boolean isCookieExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    private String hostName(HttpUrl httpUrl) {
        return httpUrl.host().startsWith(HOST_NAME_PREFIX) ? httpUrl.host() : HOST_NAME_PREFIX + httpUrl.host();
    }

    private String cookieName(Cookie cookie) {
        return cookie == null ? null : cookie.name() + cookie.domain();
    }

    //将2进制的Cookie转换成String类型的Cookie，便于使用SP存储进行保存
    protected String encodeCookie(SerializableCookie cookie) {
        if (cookie == null)
            return null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            outputStream.writeObject(cookie);
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in encodeCookie", e);
            return null;
        }

        return byteArrayToHexString(os.toByteArray());
    }


    //将本地存储的String类型的Cookie进行解码（转换成2进制后通过getCookie方法转化成Cookie类型）
    protected Cookie decodeCookie(String cookieString) {
        byte[] bytes = hexStringToByteArray(cookieString);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Cookie cookie = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            cookie = ((SerializableCookie) objectInputStream.readObject()).getCookie();
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in decodeCookie", e);
        } catch (ClassNotFoundException e) {
            Log.d(LOG_TAG, "ClassNotFoundException in decodeCookie", e);
        }
        return cookie;
    }


    //2进制转16进制的String类型
    protected String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte element : bytes) {
            int v = element & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    //16进制转2进制
    protected byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }



}
