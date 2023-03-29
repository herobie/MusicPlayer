package com.example.musicplayer.search;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.musicplayer.Bean.playlist.Playlist;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.players.OnPlayingFragment;
import com.example.musicplayer.search.result.ResultFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener{
    private TextInputEditText search_input;
    private Button search_search;
    private ImageButton  search_back;
    private OnPlayingFragment onPlayingFragment;
    private ResultFragment resultFragment;
    private SearchFragment searchFragment;
    private SearchBarInsertReceiver searchBarInsertReceiver;
    private IntentFilter searchBarInsertFilter;
    private String default_url = "http://wyyapi.itaemobile.top/cloudsearch";
    private StringBuilder urlBuilder = new StringBuilder();
    private boolean isParseSingleFinish = false;
    private boolean isParsePlaylistFinish = false;
    private final int WAITING_FOR_RESPONSE = 1;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case WAITING_FOR_RESPONSE:
                    //等待两个都解析完毕后才打开页面，否则每隔200毫秒重复此操作
                    if (isParsePlaylistFinish && isParseSingleFinish){
                        replaceFragment(resultFragment , true);
                        //发送广播刷新结果
                        Intent searchChangeIntent = new Intent("adapterActions");
                        searchChangeIntent.putExtra("adapterAction" , "refresh");
                        sendBroadcast(searchChangeIntent);
                    }else{
                        sendEmptyMessageDelayed(WAITING_FOR_RESPONSE , 200);
                    }
                    break;
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initView();
        initReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(searchBarInsertReceiver);
    }

    public void initView(){
        search_input = findViewById(R.id.search_input);
        search_back = findViewById(R.id.search_back);
        search_back.setOnClickListener(this);
        search_search = findViewById(R.id.search_search);
        search_search.setOnClickListener(this);
        resultFragment = new ResultFragment();
        searchFragment = new SearchFragment();
        replaceFragment(searchFragment , false);
        replaceOnPlayingFragment(new OnPlayingFragment());
    }

    public void initReceiver(){
        searchBarInsertReceiver = new SearchBarInsertReceiver();
        searchBarInsertFilter = new IntentFilter();
        searchBarInsertFilter.addAction("searchBarInsert");
        this.registerReceiver(searchBarInsertReceiver , searchBarInsertFilter);
    }

    public void initSQL(){

    }

    public void replaceFragment(Fragment fragment , boolean isAddToBackStack){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.search_frag_container , fragment);
        if (isAddToBackStack){
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void replaceOnPlayingFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.search_frag_bottom_container , fragment);
        transaction.commit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.search_search:
                search();
                break;
            case R.id.search_back:
                finish();
                break;
        }
    }

    public void search(){
        isParseSingleFinish = false;
        isParsePlaylistFinish = false;
        CRUD crud = new CRUD("searchHistory" , this);
        crud.deleteSameElementsInHistory(search_input.getText().toString());
        ContentValues values = new ContentValues();
        values.put("content" , search_input.getText().toString());
        values.put("creator" , Constant.activeAccount);
        crud.add(values);
        Message message = new Message();
        sendOkHttpRequest(formingUrl("&type=1"), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SearchFailed" , e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //清空集合，避免搜索结果重复显示
                if (!Constant.searchSingleList.isEmpty()){
                    Constant.searchSingleList.clear();
                }
                String responseData = response.body().string();
                parseSingleJSON(responseData);
                isParseSingleFinish = true;
                if (!isParsePlaylistFinish){
                    message.what = WAITING_FOR_RESPONSE;
                    handler.sendMessage(message);
                }
            }
        });
        sendOkHttpRequest(formingUrl("&type=1000"), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!Constant.searchPlaylist.isEmpty()){
                    Constant.searchPlaylist.clear();
                }
                String responseData = response.body().string();
                parsePlaylistJSON(responseData);
                isParsePlaylistFinish = true;
                if (!isParseSingleFinish){
                    message.what = WAITING_FOR_RESPONSE;
                    handler.sendMessage(message);
                }
            }
        });
    }


    //组装url，返回一个组装好的url
    public String formingUrl(String type){
        urlBuilder.append(default_url);
        urlBuilder.append("?keywords=" + Objects.requireNonNull(search_input.getText()).toString() + type + Constant.searchLimit);
        String target_url = urlBuilder.toString();
        //清空StringBuilder，避免重复输入
        urlBuilder.delete(0 , urlBuilder.length());
        return target_url;
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
//                    Response response = client.newCall(request).execute();
//                    responseData = response.body().string();
                }catch (Exception e){
                    Log.e("sendOkHttpRequest" , e.getMessage());
                }
            }
        }).start();
    }

    public void parseSingleJSON(String responseData){
        try {
            //解析最外层数据
            JSONObject jsonObject = new JSONObject(responseData);
            //获得result中内容
            JSONObject result = jsonObject.getJSONObject("result");
            int songCount = result.optInt("songCount");
            //判断有无搜索结果
            if (songCount > 0){
                //获取songs数组中单个元素的内容
                JSONArray songs = result.getJSONArray("songs");
                //将songs集合中的内容逐个提取出来加入searchSingleList集合
                for (int i = 0 ; i < 8 ; i ++){
                    String content = songs.getJSONObject(i).toString();
                    Gson gson = new Gson();
                    Song song = gson.fromJson(content , Song.class);
                    Constant.searchSingleList.add(song);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void parsePlaylistJSON(String responseData){
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONObject result = jsonObject.getJSONObject("result");
            JSONArray playlists = result.optJSONArray("playlists");
            for (int i = 0 ; i < 8 ; i++){
                String data = playlists.getJSONObject(i).toString();
                Gson gson = new Gson();
                Playlist playlist = gson.fromJson(data , Playlist.class);
                Constant.searchPlaylist.add(playlist);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    class SearchBarInsertReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String itemName = intent.getStringExtra("itemName");
            search_input.setText(itemName);
            search();
        }
    }
}
