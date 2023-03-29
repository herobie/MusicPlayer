package com.example.musicplayer.recommendPlaylist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.recommendPlaylist.adapters.MoreTagsAdapter;
import com.example.musicplayer.recommendPlaylist.adapters.RecommendPlaylistAdapter;
import com.example.musicplayer.recommendPlaylist.bean.TagsBean;
import com.example.musicplayer.recommendPlaylist.bean.TagsPlaylistBean;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RecommendPlaylistFragment extends Fragment implements TabLayout.OnTabSelectedListener{
    private TabLayout playlist_tag;
    private RecommendPlaylistAdapter recommendPlaylistAdapter;
    private RecyclerView playlist_playlist_rv;
    private MoreTagsAdapter moreTagsAdapter;
    private RecommendPlaylistReceiver recommendPlaylistReceiver;
    private IntentFilter recommendPlaylistFilter;
    private String url = "http://wyyapi.itaemobile.top/playlist/highquality/tags";
    private final int UPDATE_TAG_UI = 1;
    private final int UPDATE_PLAYLIST = 2;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UPDATE_TAG_UI:
                    playlist_tag.addOnTabSelectedListener(RecommendPlaylistFragment.this);
                    //先给第一个加上默认选中的功能
                    playlist_tag.addTab(playlist_tag.newTab().setText(Constant.tagList.get(0).getName()) , true);
                    //再加载前五个推荐tag
                    for (int i = 1 ; i < 5 ; i++){
                        playlist_tag.addTab(playlist_tag.newTab().setText(Constant.tagList.get(i).getName()));
                    }
                    //从第六个开始通过“更多”开启tag列表
                    playlist_tag.addTab(playlist_tag.newTab().setText("更多"));
                    break;
                case UPDATE_PLAYLIST:
                    playlist_playlist_rv.setAdapter(recommendPlaylistAdapter);
                    recommendPlaylistAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };
    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_playlist, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView();
        initTabItems(view);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(recommendPlaylistReceiver);
    }

    public void initView(){
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3 , StaggeredGridLayoutManager.VERTICAL);
        playlist_playlist_rv = getView().findViewById(R.id.playlist_playlist_rv);
        playlist_playlist_rv.setLayoutManager(layoutManager);
        recommendPlaylistAdapter = new RecommendPlaylistAdapter(getContext());
        playlist_playlist_rv.setAdapter(recommendPlaylistAdapter);
        moreTagsAdapter = new MoreTagsAdapter(getContext());

        recommendPlaylistReceiver = new RecommendPlaylistReceiver();
        recommendPlaylistFilter = new IntentFilter();
        recommendPlaylistFilter.addAction("showMoreTags");
        getContext().registerReceiver(recommendPlaylistReceiver , recommendPlaylistFilter);
    }

    //初始化标签
    public void initTabItems(View view){
        playlist_tag = view.findViewById(R.id.playlist_tag);
        //获取歌单标签
        sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                parseTagJSON(response.body().string());
                Message message = new Message();
                message.what = UPDATE_TAG_UI;
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

    //解析获取的标签
    public void parseTagJSON(String responseData){
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONArray jsonArray = jsonObject.getJSONArray("tags");
            for (int i = 0 ; i < jsonArray.length() ; i++){
                String tagObject = jsonArray.getJSONObject(i).toString();
                Gson gson = new Gson();
                TagsBean tagsBean = gson.fromJson(tagObject , TagsBean.class);
                Constant.tagList.add(tagsBean);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        tabSelected(tab);
    }

    public void tabSelected(TabLayout.Tab tab){
        switch (tab.getPosition()){
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                String target_url = "http://wyyapi.itaemobile.top/top/playlist/highquality" + "?cat=" + Constant.tagList.get(tab.getPosition()).getName() + "&limit=30";
                sendOkHttpRequest(target_url, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Constant.tagsPlaylistBeanList.clear();
                        parsePlaylistJSON(response.body().string());
                        Message message = new Message();
                        message.what = UPDATE_PLAYLIST;
                        handler.sendMessage(message);
                    }
                });
                break;
            case 5:
                playlist_playlist_rv.setAdapter(moreTagsAdapter);
                break;
        }
    }

    //解析通过标签获取的歌单信息
    public void parsePlaylistJSON(String responseData){
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONArray jsonArray = jsonObject.getJSONArray("playlists");
            for (int i = 0 ; i < jsonArray.length() ; i++){
                String playlistsObject = jsonArray.getString(i);
                Gson gson = new Gson();
                TagsPlaylistBean tagsPlaylistBean = gson.fromJson(playlistsObject , TagsPlaylistBean.class);
                Constant.tagsPlaylistBeanList.add(tagsPlaylistBean);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        tabSelected(tab);
    }

    public void showMorePlaylist(String tagName){
        String url = "http://wyyapi.itaemobile.top/top/playlist/highquality" + "?cat=" + tagName + "&limit=30";
        sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                parsePlaylistJSON(response.body().string());
                Message message = new Message();
                message.what = UPDATE_PLAYLIST;
                handler.sendMessage(message);
            }
        });
    }

    class RecommendPlaylistReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String tagName = intent.getStringExtra("tagName");
            showMorePlaylist(tagName);
        }
    }
}
