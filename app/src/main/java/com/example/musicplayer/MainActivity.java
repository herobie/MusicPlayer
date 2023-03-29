package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.Sqlite.UserDataBase;
import com.example.musicplayer.locals.LocalFragment;
import com.example.musicplayer.me.MeFragment;
import com.example.musicplayer.players.OnPlayingFragment;
import com.example.musicplayer.profile.LoginActivity;
import com.example.musicplayer.profile.ProfileBean;
import com.example.musicplayer.profile.ProfileFragment;
import com.example.musicplayer.recommendPlaylist.RecommendPlaylistFragment;
import com.example.musicplayer.search.SearchActivity;
import com.example.musicplayer.search.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    private Toolbar toolbar;
    private BottomNavigationView navigation;
    private NavigationView side_navigation;
    private DrawerLayout mDrawerLayout;
    private ChangeModeReceiver changeModeReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private IntentFilter changeModeFilter;
    private LocalFragment localFragment;
    private MeFragment meFragment;
    public static OnPlayingFragment onPlayingFragment;
    private RecommendPlaylistFragment recommendPlaylistFragment;
    private ProfileFragment profileFragment;
    private SearchFragment searchFragment;
    private CircleImageView navigation_profile;
    private TextView navigation_brief , navigation_username;
    private LinearLayout navigation_background;
    private final int NETWORK_ERROR = 1;
    private final int LOGOUT_SUCCEED = 2;
    private final int LOGIN_FAILED = 3;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case NETWORK_ERROR:
                    Toast.makeText(MainActivity.this, "操作失败，请检查网络！", Toast.LENGTH_SHORT).show();
                    break;
                case LOGOUT_SUCCEED:
                    Constant.isLogin = false;
                    mDrawerLayout.closeDrawers();
                    Toast.makeText(MainActivity.this, "操作成功！", Toast.LENGTH_SHORT).show();
                    updateUI();
                    //如果在个人信息界面，发送广播更新UI
                    Intent intent = new Intent("refreshProfileUI");
                    sendBroadcast(intent);
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        acquirePermissions();
        initSQL();
        initView();
        initReceiver();
//        autoLogin();
        Log.d("MainActivity" , "主页面加载完毕");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(changeModeReceiver);
        Log.d("MainActivity" , "退出");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void initView(){
        toolbar = findViewById(R.id.playlist_bar);
        setSupportActionBar(toolbar);
        mDrawerLayout = findViewById(R.id.mDrawerLayout);
        side_navigation = findViewById(R.id.side_navigation);
        View headerLayout = side_navigation.inflateHeaderView(R.layout.navigation);
        navigation_profile = headerLayout.findViewById(R.id.navigation_profile);
        navigation_username = headerLayout.findViewById(R.id.navigation_username);
        navigation_brief = headerLayout.findViewById(R.id.navigation_brief);
        navigation_background = headerLayout.findViewById(R.id.navigation_background);
        side_navigation.setNavigationItemSelectedListener(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24);
        }
        //加载底部导航栏的选项
        initBottomNavigation();
        localFragment = new LocalFragment();
        meFragment = new MeFragment();
        searchFragment = new SearchFragment();
        recommendPlaylistFragment = new RecommendPlaylistFragment();
        profileFragment = new ProfileFragment();
        initOnPlayingFragment();
    }

    public void initReceiver(){
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        changeModeReceiver = new ChangeModeReceiver();
        changeModeFilter = new IntentFilter();
        changeModeFilter.addAction("changeMode");
        registerReceiver(changeModeReceiver , changeModeFilter);
    }

    public void acquirePermissions(){
        if(ContextCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE} , 1);
        }
        if(ContextCompat.checkSelfPermission(this , Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE} , 1);
        }
        if(ContextCompat.checkSelfPermission(this , Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.INTERNET} , 1);
        }
    }

    public void initSQL(){
        new CRUD("lastPlay" , this).retrieveLastPlaylist();
    }

    public void initMediaPlayer(){
        Intent in = new Intent(MainActivity.this , PlayMusicService.class);
        in.putExtra("action" , PlayMusicService.MUSIC_PREPARE);
        startService(in);
    }


    public void initBottomNavigation(){
        navigation = findViewById(R.id.navigation);
        for (int i = 0; i < Constant.itemsDisplayOnTheBottom.length ; i++){
            initCustomizeBottomNavigation(Constant.itemsDisplayOnTheBottom[i]);
        }
        navigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent changeModeIntent = new Intent("changeMode");
                switch (item.getItemId()){
                    case 0:
                        break;
                    case 1:
                        changeModeIntent.putExtra("mode" , "playlist");
                        sendBroadcast(changeModeIntent);
                        break;
                    case 2:
                        changeModeIntent.putExtra("mode" , "me");
                        sendBroadcast(changeModeIntent);
                        break;
                    case 3:
                       changeModeIntent.putExtra("mode" , "local");
                       sendBroadcast(changeModeIntent);
                       break;
                    case 4:
                        changeModeIntent.putExtra("mode" , "profile");
                        sendBroadcast(changeModeIntent);
                        break;
                }
                return true;
            }
        });
    }

    //初始化自定义底部导航栏里的组件，id表见Constant类
    public void initCustomizeBottomNavigation(int i){
        switch (i){
            case 0:
                navigation.getMenu().add(0 ,i , 0 , "发现");
                navigation.getMenu().findItem(i).setIcon(R.drawable.ic_baseline_explore_24);
                break;
            case 1:
                navigation.getMenu().add(0 , i , 0 , "歌单");
                navigation.getMenu().findItem(i).setIcon(R.drawable.ic_baseline_queue_music_24);
                break;
            case 2:
                navigation.getMenu().add(0 , i , 0 , "我的");
                navigation.getMenu().findItem(i).setIcon(R.drawable.ic_baseline_music_note_24);
                break;
            case 3:
                navigation.getMenu().add(0 , i , 0 , "本地");
                navigation.getMenu().findItem(i).setIcon(R.drawable.ic_baseline_star_outline_24);
                break;
            case 4:
                navigation.getMenu().add(0 , i , 0 , "资料");
                navigation.getMenu().findItem(i).setIcon(R.drawable.ic_baseline_person_24);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.toolbar , menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.toolbar_search:
                Intent changeModeIntent = new Intent(MainActivity.this , SearchActivity.class);
                startActivity(changeModeIntent);
                break;
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
        }
        return true;
    }

    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragContainer , fragment);
        transaction.commit();
    }

    @SuppressLint("Range")
    public void initOnPlayingFragment(){//加载底部音乐播放界面
        //如果是第一次使用，很可能出现找不到这一行(上次播放的音乐）的情况
        try{
            replaceOnPlayingFragment(onPlayingFragment);
        }catch (Exception e){
            //找不到就创建一个
            onPlayingFragment = new OnPlayingFragment("暂无播放歌曲" , "未知" , null);
            replaceOnPlayingFragment(onPlayingFragment);
        }
    }

    //加载底部的歌曲播放碎片
    public void replaceOnPlayingFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.musicFragContainer , fragment);
        transaction.commit();
    }

    @SuppressLint("ResourceType")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.navigation_info:
                //点击后等同于按下“资料”按钮
                mDrawerLayout.closeDrawer(GravityCompat.START);
                //动态点击“资料按钮”
                navigation.setSelectedItemId(4);
                break;
            case R.id.navigation_log:
                //如果没有登录，则跳转至登录界面
                if (!Constant.isLogin){
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    //动态点击“资料按钮”
                    navigation.setSelectedItemId(4);
                    Intent in = new Intent(this , LoginActivity.class);
                    startActivity(in);
                }else{
                    logout();
                }
                break;
            case R.id.navigation_quit:
                System.exit(0);
                break;
        }
        return false;
    }

    public void updateUI(){
        //登录成功后刷新滑动菜单栏中的内容
        if (Constant.isLogin){
            int follows = Constant.profileBean.getProfile().getFollows();
            int followeds = Constant.profileBean.getProfile().getFolloweds();
            navigation_brief.setText(followeds + "关注  |  " + follows + "粉丝");
            navigation_username.setText(Constant.profileBean.getProfile().getNickname());
            Glide.with(this).load(Constant.profileBean.getProfile().getAvatarUrl())
                    .placeholder(R.drawable.ic_baseline_person_24)
                    .error(R.drawable.ic_baseline_person_24)
                    .into(navigation_profile);
            Glide.with(this).asBitmap().load(Constant.profileBean.getProfile().getBackgroundUrl())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Drawable drawable = new BitmapDrawable(resource);
                            navigation_background.setBackground(drawable);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }else{
            navigation_brief.setText("0关注  |  " + "0粉丝");
            navigation_username.setText("未登录");
            navigation_profile.setImageResource(R.drawable.ic_baseline_person_24);
            navigation_background.setBackgroundResource(R.color.light_grey);
        }
    }

    //退出登录操作
    public void logout(){
        String url = "http://wyyapi.itaemobile.top/logout";
        sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body().string();
                int code = 0;
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    code = jsonObject.getInt("code");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Message message = new Message();
                if (code == 200){
                    message.what = LOGOUT_SUCCEED;
                }else {
                    message.what = NETWORK_ERROR;
                }
                handler.sendMessage(message);
            }
        });
    }

    public void sendOkHttpRequest(String target_url , Callback callback){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(target_url)
                            .build();
                    client.newCall(request).enqueue(callback);
                }catch (Exception e){
                    Log.e("sendOkHttpRequest" , e.getMessage());
                }
            }
        }).start();
    }

    public void autoLogin(){
        String url = "http://wyyapi.itaemobile.top/user/binding";
        LoginActivity.sendLoginOkHttpRequest(this, url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = NETWORK_ERROR;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                parseInfoJSON(response.body().string());
            }
        });
    }

    public void parseInfoJSON(String responseData){
        Message message = new Message();
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            int code = jsonObject.getInt("code");
            if (code == 200){
                String data = jsonObject.toString();
                Gson gson = new Gson();
                Constant.profileBean = gson.fromJson(data , ProfileBean.class);
                Constant.isLogin = true;
                Constant.activeAccount = Constant.profileBean.getProfile().getNickname();
            }else {
                message.what = LOGIN_FAILED;
            }
            handler.sendMessage(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //切换显示时调用,通过接收广播传来的mode值判断切换的界面
    class ChangeModeReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = intent.getStringExtra("mode");
            switch (mode){
                case "local":
                    replaceFragment(localFragment);
                    break;
                case "me":
                    replaceFragment(meFragment);
                    break;
                case "playlist":
                    replaceFragment(recommendPlaylistFragment);
                    break;
                case "profile":
                    replaceFragment(profileFragment);
                    break;
            }
        }
    }
}