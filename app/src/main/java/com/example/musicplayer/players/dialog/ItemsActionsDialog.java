package com.example.musicplayer.players.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ItemsActionsDialog extends Dialog implements View.OnClickListener{
    private String launchFrom;
    private Button items_play_next , items_add_playList , items_delete;
    private int position;
    private long playlistId;
    private int sameElementPosition = 0;
    private final int SEND_ERROR_INFO = 1;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case SEND_ERROR_INFO:
                    Toast.makeText(getContext(), "找不到歌曲url!", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    public ItemsActionsDialog(@NonNull Context context , String launchFrom) {
        super(context , R.style.DialogBaseStyle);
        this.launchFrom = launchFrom;
    }

    public ItemsActionsDialog(@NonNull Context context , String launchFrom , int position){
        super(context , R.style.DialogBaseStyle);
        this.launchFrom = launchFrom;
        this.position = position;
    }

    public ItemsActionsDialog(@NonNull Context context , String launchFrom , int position , long playlistId){
        super(context , R.style.DialogBaseStyle);
        this.launchFrom = launchFrom;
        this.position = position;
        this.playlistId = playlistId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_items_actions);
        initView();
    }

    public void initView(){
        items_play_next = findViewById(R.id.items_play_next);
        items_play_next.setOnClickListener(this);
        items_add_playList = findViewById(R.id.items_add_playList);
        items_add_playList.setOnClickListener(this);
        items_delete = findViewById(R.id.items_delete);
        items_delete.setOnClickListener(this);
        if (!launchFrom.equals("playlist") && !launchFrom.equals("searchPlaylist")){
            items_delete.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.items_play_next:
                if (launchFrom.equals("local")){
                    addNext(Constant.localSongList);
                }else if (launchFrom.equals("search")){
                    toSongBean(Constant.searchSingleList);
                }else if (launchFrom.equals("playlist")){
                    toSongBean(Constant.musicList);
                }else if (launchFrom.equals("searchPlaylist")){
                    toSongBean(Constant.searchMusicInPlaylist);
                }
                dismiss();
                break;
            case R.id.items_add_playList:
                ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog(getContext() , position , launchFrom);
                choosePlaylistDialog.show();
                break;
            case R.id.items_delete:
                new CRUD("music" , getContext()).deleteMusicFromPlaylist(Constant.playlistId , Constant.musicList.get(position).id);
                Toast.makeText(getContext(), "操作成功！", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    //下一首播放，传入参数为任一SongBean类的集合（歌曲来源,如searchList,musicList）
    public void addNext(List<SongBean> songBeanList){
        //判断当前播放数组长度，如果为空则直接添加
        if (Constant.currentPlayList.size() < 1){
            Constant.currentPlayList.add(songBeanList.get(position));
            Intent in = new Intent(getContext() , PlayMusicService.class);
            in.putExtra("action" , PlayMusicService.CHANGE_SONG);
            getContext().startService(in);
        }else {//如果不为空则判断该歌曲是否已经在播放列表中，如果不存在则添加，存在则交换位置
            if (!isAlreadyInList(songBeanList.get(position).musicUrl)){
                Constant.currentPlayList.add(nextSongPosition() , songBeanList.get(position));
            }else{
                Collections.swap(Constant.currentPlayList , nextSongPosition() , sameElementPosition);
            }
        }
    }

    public void addNext(SongBean songBean){
        //判断当前播放数组长度，如果为空则直接添加，如果不为空则加到下一首
        if (Constant.currentPlayList.size() < 1){
            Constant.currentPlayList.add(songBean);
            Intent in = new Intent(getContext() , PlayMusicService.class);
            in.putExtra("action" , PlayMusicService.CHANGE_SONG);
            getContext().startService(in);
        }else {
            if (!isAlreadyInList(songBean.musicUrl)){
                Constant.currentPlayList.add(nextSongPosition() , songBean);
            }else{
                Collections.swap(Constant.currentPlayList , nextSongPosition() , sameElementPosition);
            }
        }
    }

    public boolean isAlreadyInList(String url){
        boolean isExist = false;
        for (int i = 0 ; i < Constant.currentPlayList.size() ; i++){
            if (url.equals(Constant.currentPlayList.get(i).musicUrl)){
                isExist = true;
                sameElementPosition = i;
                break;
            }
        }
        return isExist;
    }

    //判断当前位置加一有无超出集合长度，如果超出，则改为把这首歌放到第一首
    public int nextSongPosition(){
        int tmpPosition = Constant.currentPosition + 1;
        if (Constant.currentPosition + 1 >= Constant.currentPlayList.size()){
            tmpPosition = 0;
        }
        return  tmpPosition;
    }

    //对于从搜索结果，歌单等地方加入的歌曲，必须先将其从Song转为SongBean类，传入参数songList为需要转换的集合（如搜索结果等）
    public void toSongBean(List<Song> songList){
        acquireMusicUrl(formingUrl(position , songList), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    SongBean songBean = parseJSON(response.body().string() , songList);
                    //判断音乐url是不是一个"null"，如果不是才能加入播放列表
                    if (!songBean.musicUrl.equals("null")){
                        //生成的SongBean类通过这个方法判断是直接加入下一首还是交换位置进入下一首
                        addNext(songBean);
                    }else{
                        //如果url是"null"则丢弃该类并提示错误
                        songBean = null;
                        Message message = new Message();
                        message.what = SEND_ERROR_INFO;
                        handler.sendMessage(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //生成请求
    public String formingUrl(int position , List<Song> songList){
        String musicID = "?id=" + String.valueOf(songList.get(position).id);
        String url = "http://wyyapi.itaemobile.top/song/url/v1" + musicID + Constant.defaultBrLevel;
        return url;
    }
    //发送请求
    public void acquireMusicUrl(String url , Callback callback){
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                client.newCall(request).enqueue(callback);
            }
        }).start();
    }

    //解析返回的数据，并返回一个生成的SongBean类
    public SongBean parseJSON(String responseData , List<Song> songList) throws JSONException {
        //获取数据
        JSONObject jsonObject = new JSONObject(responseData);
        //最外层data数组
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        //数组中的数据
        JSONObject jsonObject1 = jsonArray.getJSONObject(0);
        //获取url , 标题 ， 歌手等数据
        String url = jsonObject1.getString("url");
        String picUrl = songList.get(position).al.picUrl;
        String title = songList.get(position).name;
        String artist = displayMultipleArtist(position , songList);
        int time = jsonObject1.getInt("time");
        int id = jsonObject1.getInt("id");
        SongBean songBean = new SongBean(title , artist , url , picUrl , time , id ,"web");
        return songBean;
    }

    //组装多位作曲家的名字
    public String displayMultipleArtist(int position , List<Song> songList){
        String artists = "";
        //如果有一名以上的歌手，则循环调出歌手名
        for (int i = 0 ; i < songList.get(position).getAr().size() ; i++){
            artists += songList.get(position).getAr().get(i).getName();
            artists += "   ";
        }
        return artists;
    }

}
