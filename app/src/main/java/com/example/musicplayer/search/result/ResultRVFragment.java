package com.example.musicplayer.search.result;

import android.annotation.SuppressLint;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;
import com.example.musicplayer.search.result.playlist.PlaylistResultAdapter;
import com.example.musicplayer.search.result.single.ResultAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ResultRVFragment extends Fragment implements View.OnClickListener{
    private LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
    private TextView local_play_warning;
    private RecyclerView rv_local_music;
    private ImageButton local_choose_music , local_play;
    private String launchType;
    private ResultRVReceiver resultRVReceiver;
    private IntentFilter resultRVFilter;
    private ResultAdapter resultAdapter;
    private PlaylistResultAdapter playlistResultAdapter;
    private boolean isWarningToasted = false;
    private final int DISPLAY_SINGLE_RESULT = 1;
    private final int DISPLAY_PLAYLIST_RESULT = 2;
    private final int REFRESH_ADAPTER = 3;
    private final int NETWORK_ERROR = 4;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case DISPLAY_SINGLE_RESULT:
                    displayItemsType("single");
                    launchType = "single";
                    break;
                case DISPLAY_PLAYLIST_RESULT:
                    displayItemsType("playlist");
                    launchType = "playlist";
                    break;
                case REFRESH_ADAPTER:
                    if (launchType.equals("playlist")){
                        playlistResultAdapter.notifyDataSetChanged();
                        local_play_warning.setText("搜索结果" + "(" + Constant.searchPlaylist.size() + ")");
                    }else {
                        resultAdapter.notifyDataSetChanged();
                        local_play_warning.setText("播放全部" + "(" + Constant.searchSingleList.size() + ")");
                    }
                    break;
                case NETWORK_ERROR:
                    //判断网络报错提示过没有，如果没有提示过才能发送toast
                    if (!isWarningToasted){
                        isWarningToasted = true;
                        Toast.makeText(getContext(), "获取音乐失败，请检查网络", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
    public ResultRVFragment(String launchType){
        this.launchType = launchType;
    }

    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.local_music_fragment, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView(view);
        initReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(resultRVReceiver);
    }

    public void initView(View view){
        local_play_warning = view.findViewById(R.id.local_play_warning);
        local_choose_music = view.findViewById(R.id.local_choose_music);
        local_choose_music.setVisibility(View.INVISIBLE);
        local_play = view.findViewById(R.id.local_play);
        local_play.setOnClickListener(this);
        rv_local_music = view.findViewById(R.id.rv_local_music);
        rv_local_music.setLayoutManager(layoutManager);
        resultAdapter = new ResultAdapter(getActivity());
        playlistResultAdapter = new PlaylistResultAdapter(getContext());
        displayItemsType(launchType);
    }

    public void initReceiver(){
        resultRVReceiver = new ResultRVReceiver();
        resultRVFilter = new IntentFilter();
        resultRVFilter.addAction("adapterActions");
        getContext().registerReceiver(resultRVReceiver , resultRVFilter);
    }

    private void displayItemsType(String type){
        if (type.equals("single")){
            rv_local_music.setAdapter(resultAdapter);
            local_play.setVisibility(View.VISIBLE);
            local_choose_music.setVisibility(View.VISIBLE);
            local_play_warning.setText("播放全部" + "(" + Constant.searchSingleList.size() + ")");
        }else if (type.equals("playlist")){
            rv_local_music.setAdapter(playlistResultAdapter);
            local_play.setVisibility(View.GONE);
            local_choose_music.setVisibility(View.GONE);
            local_play_warning.setText("搜索结果" + "(" + Constant.searchPlaylist.size() + ")");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.local_play:
                //先把至少有一个音乐准备完成设定为false
                boolean isAtLeastOneReady = false;
                isWarningToasted = false;
                //清空当前播放列表
                if (!Constant.currentPlayList.isEmpty()){
                    Constant.currentPlayList.clear();
                }
                int musicNum = Constant.searchSingleList.size();
                int threadNum = 1;
                //如果处理数据量比较大，则多开一个线程
                if (musicNum > 200){
                    threadNum = 2;
                }
                //开线程
                CountDownLatch latch = new CountDownLatch(threadNum);
                ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
                Log.d("ResultRVFragment" , "解析开始");
                for (int i = 0 ; i < musicNum ; i++){
                    //丢进内部类的
                    final int tmpPosition = i;
                    String url = formingUrl(i);
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url(url)
                                    .build();
                            Callback callback = new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                    Message message = new Message();
                                    message.what = NETWORK_ERROR;
                                    handler.sendMessage(message);
                                }

                                @Override
                                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                    parseSingleJson(response.body().string() , tmpPosition);
                                    Log.d("ResultRVFragment" , "线程完成");
                                }
                            };
                            client.newCall(request).enqueue(callback);
                        }
                    });
                }
                try {
                    //只要至少有一个线程解析出结果了就可以继续下面的步骤
                    latch.await(1, TimeUnit.SECONDS);
                    Log.d("ResultRVFragment" , "解析完成");
                    //判断有没有线程已经进行完成，如果还没有，则开始播放第一首音乐，如果有，则在播放列表中等待
                    if (!isAtLeastOneReady){
                        Intent startPlayingIntent = new Intent( getActivity() , PlayMusicService.class);
                        startPlayingIntent.putExtra("action" , PlayMusicService.NEW_SONG);
                        getActivity().startService(startPlayingIntent);
                        Log.d("ResultRVFragment" , "开启服务");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }


    public String formingUrl(int position){
        String musicID = "?id=" + String.valueOf(Constant.searchSingleList.get(position).id);
        String url = "http://wyyapi.itaemobile.top/song/url/v1" + musicID + Constant.defaultBrLevel;
        return url;
    }

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

    public void parseSingleJson(String responseData , int position){
        //获取数据
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(responseData);
            //最外层data数组
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            //数组中的数据
            JSONObject jsonObject1 = jsonArray.getJSONObject(0);
            //获取url , 标题 ， 歌手等数据
            String url = jsonObject1.getString("url");
            String picUrl = Constant.searchSingleList.get(position).al.picUrl;
            String title = Constant.searchSingleList.get(position).name;
            String artist = displayMultipleArtist(position);
            int time = jsonObject1.getInt("time");
            int id = jsonObject1.getInt("id");
            SongBean songBean = new SongBean(title , artist , url , picUrl , time , id ,"web");
            Constant.currentPlayList.add(songBean);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String displayMultipleArtist(int position){
        String artists = "";
        //如果有一名以上的歌手，则循环调出歌手名
        for (int i = 0 ; i < Constant.searchSingleList.get(position).getAr().size() ; i++){
            artists += Constant.searchSingleList.get(position).getAr().get(i).getName();
            artists += "   ";
        }
        return artists;
    }

    class ResultRVReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("adapterAction");
            Message message = new Message();
            switch (action){
                case "single":
                    message.what = DISPLAY_SINGLE_RESULT;
                    break;
                case "playlist":
                    message.what = DISPLAY_PLAYLIST_RESULT;
                    break;
                case "refresh":
                    message.what = REFRESH_ADAPTER;
            }
            handler.sendMessage(message);
        }
    }
}
