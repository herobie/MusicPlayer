package com.example.musicplayer.me.saved;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.me.SavedPlaylistActionDialog;
import com.example.musicplayer.playlist.PlaylistActivity;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SavedPlaylistAdapter extends RecyclerView.Adapter<SavedPlaylistAdapter.ViewHolder> {
    private Context context;
    private final int NETWORK_ERROR = 1;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case NETWORK_ERROR:
                    Toast.makeText(context, "获取歌单数据失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public SavedPlaylistAdapter(Context context){
        this.context = context;
    }
    static class ViewHolder extends RecyclerView.ViewHolder{
        private ImageView result_cover;
        private TextView result_playlist_name , result_playlist_creator;
        private CardView result_playlist_view;
        private ImageButton result_playlist_action;
        public ViewHolder(@NonNull View view) {
            super(view);
            result_playlist_creator = view.findViewById(R.id.result_playlist_creator);
            result_playlist_name = view.findViewById(R.id.result_playlist_name);
            result_cover = view.findViewById(R.id.result_cover);
            result_playlist_view = view.findViewById(R.id.result_playlist_view);
            result_playlist_action = view.findViewById(R.id.result_playlist_action);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result_playlist, parent , false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.result_playlist_name.setText(Constant.myPlaylist.get(position).getName());
        holder.result_playlist_creator.setText(Constant.myPlaylist.get(position).getCreator().getNickname());
        Glide.with(context).load(Constant.myPlaylist.get(position).getCoverImgUrl())
                .placeholder(R.drawable.ic_baseline_photo_24)
                .error(R.drawable.ic_baseline_photo_24)
                .into(holder.result_cover);
        holder.result_playlist_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long musicId = new CRUD("playlist" , context).retrieveWebMusicInPlaylist(Constant.myPlaylist.get(position).getId());
                Log.d("SavedPlaylistFragment" , "目标歌单id" + String.valueOf(musicId));
                String url = "http://wyyapi.itaemobile.top/playlist/track/all?id=" + String.valueOf(musicId);
                Constant.playlistId = Constant.myPlaylist.get(position).getId();
                sendOkHttpRequest(url, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what = NETWORK_ERROR;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Log.d("SavedPlaylistFragment" , "收到数据");
                        Constant.musicList.clear();
                        parseJSON(response.body().string());
                        Intent in = new Intent(context , PlaylistActivity.class);
                        in.putExtra("name" , Constant.myPlaylist.get(position).getName());
                        in.putExtra("coverImgUrl" , Constant.myPlaylist.get(position).getCoverImgUrl());
                        in.putExtra("playlistType" , "save");
                        context.startActivity(in);
                    }
                });
            }
        });

        holder.result_playlist_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Constant.playlistId = Constant.myPlaylist.get(position).getId();
                SavedPlaylistActionDialog savedPlaylistActionDialog = new SavedPlaylistActionDialog(context ,Constant.playlistId , "search");
                savedPlaylistActionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Intent in = new Intent("refreshAdapter");
                        context.sendBroadcast(in);
                    }
                });
                savedPlaylistActionDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return Constant.myPlaylist.size();
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
                    Log.d("SavedPlaylistFragment" , "请求已发送");
                }catch (Exception e){
                    Log.e("SavedPlaylistFragment" , e.getMessage());
                }
            }
        }).start();
    }

    public void parseJSON(String responseData){
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONArray jsonArray = jsonObject.getJSONArray("songs");
            CountDownLatch latch = new CountDownLatch(dynamicSetThreadNumber(jsonArray.length()));
            for (int i = 0 ; i < jsonArray.length() ; i++){
                String data = jsonArray.getString(i);
                Gson gson = new Gson();
                Song song = gson.fromJson(data , Song.class);
                Constant.musicList.add(song);
            }
            Log.d("SavedPlaylistFragment" , "解析完成");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //根据数据个数决定开启子线程的多少
    public int dynamicSetThreadNumber(int dataNumbers){
        int threadNumber = 1;
        if (dataNumbers > 80 && dataNumbers <= 160){
            threadNumber = 2;
        }else if (dataNumbers > 160 && dataNumbers <= 250){
            threadNumber = 3;
        }else if (dataNumbers > 250 && dataNumbers <= 340){
            threadNumber = 4;
        }else if (dataNumbers > 340){
            threadNumber = 5;
        }
        return threadNumber;
    }
}
