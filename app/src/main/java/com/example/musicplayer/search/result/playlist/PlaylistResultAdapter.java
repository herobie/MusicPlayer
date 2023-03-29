package com.example.musicplayer.search.result.playlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.me.SavedPlaylistActionDialog;
import com.example.musicplayer.playlist.PlaylistActivity;
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

public class PlaylistResultAdapter extends RecyclerView.Adapter<PlaylistResultAdapter.ViewHolder>{
    private Context context;
    public PlaylistResultAdapter(Context context){
        this.context = context;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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
        holder.result_playlist_name.setText(Constant.searchPlaylist.get(position).getName());
        holder.result_playlist_creator.setText(Constant.searchPlaylist.get(position).getCreator().getNickname());
        Glide.with(context)
                .load(Constant.searchPlaylist.get(position).getCoverImgUrl())
                .placeholder(R.drawable.ic_baseline_photo_24)
                .error(R.drawable.ic_baseline_photo_24)
                .into(holder.result_cover);
        holder.result_playlist_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long id = Constant.searchPlaylist.get(position).getId();
                Constant.playlistId = id;
                String url = "http://wyyapi.itaemobile.top/playlist/track/all?id=" + String.valueOf(id);
                sendOkHttpRequest(url, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        //清空避免显示错误
                        if (Constant.musicList != null){
                            Constant.musicList.clear();
                        }
                        parseJSON(response.body().string());
                        Intent in = new Intent(context , PlaylistActivity.class);
                        in.putExtra("name" , Constant.searchPlaylist.get(position).getName());
                        in.putExtra("coverImgUrl" , Constant.searchPlaylist.get(position).getCoverImgUrl());
                        in.putExtra("playlistType" , "web");
                        context.startActivity(in);
                    }
                });
            }
        });

        holder.result_playlist_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = Constant.searchPlaylist.get(position).getName();
                String coverImgUrl = Constant.searchPlaylist.get(position).getCoverImgUrl();
                long id = Constant.searchPlaylist.get(position).getId();
                Constant.playlistId = id;
                SavedPlaylistActionDialog savedPlaylistActionDialog = new SavedPlaylistActionDialog(context , name , coverImgUrl , id , "search");
                savedPlaylistActionDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return Constant.searchPlaylist.size();
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

    //解析返回的数据
    public void parseJSON(String responseData){
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONArray jsonArray = jsonObject.getJSONArray("songs");
            for (int i = 0 ; i < jsonArray.length() ; i++){
                String data = jsonArray.getString(i);
                Gson gson = new Gson();
                Song song = gson.fromJson(data , Song.class);
                Constant.musicList.add(song);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
