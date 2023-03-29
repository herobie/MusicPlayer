package com.example.musicplayer.playlist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;
import com.example.musicplayer.players.dialog.ItemsActionsDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder>{
    private PlayMusicService playMusicService;
    private Activity activity;
    private List<Song> songList;
    private String launchFrom;
    private long playlistId;
    private final int SEND_ERROR_INFO = 1;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case SEND_ERROR_INFO:
                    Toast.makeText(activity, "找不到歌曲url!", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public PlaylistAdapter(Activity activity , List<Song> songList , String launchFrom){
        this.activity = activity;
        this.songList = songList;
        this.launchFrom = launchFrom;
    }

    //这里直接调了一下本地音乐的item
    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView local_number , local_title , local_artist;
        private ImageButton local_actions;
        private ConstraintLayout local_item_view;
        public ViewHolder(@NonNull View view) {
            super(view);
            local_number = view.findViewById(R.id.local_number);
            local_title = view.findViewById(R.id.local_title);
            local_artist = view.findViewById(R.id.local_artist);
            local_actions = view.findViewById(R.id.local_actions);
            local_item_view = view.findViewById(R.id.local_item_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_item, parent , false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.local_number.setText(String.valueOf(position + 1));
        holder.local_title.setText(songList.get(position).name);
        holder.local_artist.setText(displayMultipleArtist(position));
        holder.local_actions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemsActionsDialog itemsActionsDialog = new ItemsActionsDialog(v.getContext() , launchFrom , position );
                itemsActionsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        notifyDataSetChanged();
                    }
                });
                itemsActionsDialog.show();
            }
        });
        holder.local_item_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acquireMusicUrl(formingUrl(position), new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String responseData = response.body().string();
                        parseJson(responseData , holder , position);
                        //收到数据后开启服务
                        Intent startPlayingIntent = new Intent( activity , PlayMusicService.class);
                        startPlayingIntent.putExtra("action" , PlayMusicService.NEW_SONG);
                        activity.startService(startPlayingIntent);
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public String formingUrl(int position){
        String musicID = "?id=" + String.valueOf(songList.get(position).id);
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
            if (!url.equals("null")){
                String picUrl = Constant.musicList.get(position).al.picUrl;
                String title = holder.local_title.getText().toString();
                String artist = holder.local_artist.getText().toString();
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
            }else {
                Message message = new Message();
                message.what = SEND_ERROR_INFO;
                handler.sendMessage(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

}
