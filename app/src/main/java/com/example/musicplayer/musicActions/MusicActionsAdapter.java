package com.example.musicplayer.musicActions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MusicActionsAdapter extends RecyclerView.Adapter<MusicActionsAdapter.ViewHolder> {
    private String musicActionMode;
    private List<Song> songList;
    private Context context;
    private final int ADD_NEXT = 1;
    //记录选中的item的位置
    private List<Integer> positionList = new ArrayList<>();
    private android.os.Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case ADD_NEXT:

            }
        }
    };
    static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView actions_item_view;
        private CheckBox actions_check;
        private TextView actions_music_title , actions_artist;
        private ImageButton actions_slide;
        public ViewHolder(@NonNull View view) {
            super(view);
            actions_item_view = view.findViewById(R.id.actions_item_view);
            actions_check = view.findViewById(R.id.actions_check);
            actions_music_title = view.findViewById(R.id.actions_music_title);
            actions_artist = view.findViewById(R.id.actions_artist);
            actions_slide = view.findViewById(R.id.actions_slide);
        }
    }
    //获取是从哪里启动的
    public MusicActionsAdapter(String musicActionMode , Context context){
        this.musicActionMode = musicActionMode;
        this.context = context;
    }
    //用于接收从搜索栏或者收藏列表启动的
    public MusicActionsAdapter(String musicActionMode , List<Song> songList , Context context){
        this.musicActionMode = musicActionMode;
        this.songList = songList;
        this.context = context;
    }

    @NonNull
    @Override
    public MusicActionsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_actions, parent , false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MusicActionsAdapter.ViewHolder holder, int position) {
        initReceiver();
        if(musicActionMode.equals("local")){
            initLocal(holder , position);
            checkBoxAction(holder , position);
        }else if (musicActionMode.equals("web") || musicActionMode.equals("collections")){
            initWeb(holder , position , songList);
            checkBoxAction(holder , position , songList);
        }
        if (!musicActionMode.equals("local")){
            holder.actions_slide.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        int size = 0;
        if (musicActionMode.equals("local")){
            size = Constant.localSongList.size();
        }else if (musicActionMode.equals("web")){
            size = Constant.musicList.size();
        }
        return size;
    }

    public void initReceiver(){
        MusicActionReceiver musicActionReceiver = new MusicActionReceiver();
        IntentFilter musicActionFilter = new IntentFilter();
        musicActionFilter.addAction("musicAction");
        context.registerReceiver(musicActionReceiver , musicActionFilter);
    }

    public void initLocal(MusicActionsAdapter.ViewHolder holder, int position){
        holder.actions_music_title.setText(Constant.localSongList.get(position).getTitle());
        holder.actions_artist.setText(Constant.localSongList.get(position).getArtist());
    }

    public void initWeb(MusicActionsAdapter.ViewHolder holder, int position , List<Song> songList){
        holder.actions_music_title.setText(songList.get(position).name);
        holder.actions_artist.setText(displayMultipleArtist(position));
    }

    public void checkBoxAction(MusicActionsAdapter.ViewHolder holder, int position , List<Song> songList){
        holder.actions_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    songList.get(position).setSelected(true);
                }else {
                    songList.get(position).setSelected(false);
                }
            }
        });
    }

    public void checkBoxAction(MusicActionsAdapter.ViewHolder holder, int position ){
        holder.actions_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Constant.localSongList.get(position).setSelected(true);
                }else {
                    Constant.localSongList.get(position).setSelected(false);
                }
            }
        });
    }

    public String displayMultipleArtist(int position){
        String artists = "";
        //如果有一名以上的歌手，则循环调出歌手名
        for (int i = 0 ; i < songList.get(position).getAr().size() ; i++){
            artists += songList.get(position).getAr().get(i).getName();
            artists += "   ";
        }
        return artists;
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

    public void parseJson(String responseData , ViewHolder holder , int position){
        try {
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
            String artist = displayMultipleArtist(position);
            int time = jsonObject1.getInt("time");
            int id = jsonObject1.getInt("id");
            //判断当前播放列表中是否已经存在该音乐
            boolean isSame = false;
            //遍历当前播放列表，将该音乐的url与已存在的一一比较
            for (int i = 0 ; i < Constant.currentPlayList.size() ; i++){
                if (url.equals(Constant.currentPlayList.get(i).musicUrl)){
                    isSame = true;
                    Constant.currentPosition = i;
                    break;
                }
            }
            //找不到相同的就加入列表
            if (!isSame){
                SongBean songBean = new SongBean(title , artist , url , picUrl , time , id ,"web");
                Constant.currentPlayList.add(Constant.currentPosition , songBean);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addPlaylist(){
        if (musicActionMode.equals("local")){
            for (int i = 0 ; i < Constant.localSongList.size() ; i++){
                if (Constant.localSongList.get(i).isSelected()){
                    Constant.currentPlayList.add(Constant.localSongList.get(i));
                }
            }
        }else{

        }
    }

    public void quit(){
        for (int i = 0 ; i < Constant.localSongList.size() ; i++){
            Constant.localSongList.get(i).setSelected(false);
        }
    }


    class MusicActionReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            switch (action){
                case "quit":
                    quit();
                    break;
                case "addPlaylist":
                    addPlaylist();
                    break;
            }
        }
    }
}
