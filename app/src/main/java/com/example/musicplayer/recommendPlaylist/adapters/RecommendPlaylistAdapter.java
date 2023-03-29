package com.example.musicplayer.recommendPlaylist.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
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

public class RecommendPlaylistAdapter extends RecyclerView.Adapter<RecommendPlaylistAdapter.ViewHolder> {
    private Context context;
    public RecommendPlaylistAdapter(Context context){
        this.context = context;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        private ImageView playlist_cover;
        private TextView playlist_name;
        private LinearLayout playlist_view;
        public ViewHolder(@NonNull View view) {
            super(view);
            playlist_view = view.findViewById(R.id.playlist_view);
            playlist_name = view.findViewById(R.id.playlist_name);
            playlist_cover = view.findViewById(R.id.playlist_cover);
        }
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent , false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Glide.with(context)
                .load(Constant.tagsPlaylistBeanList.get(position).getCoverImgUrl())
                .placeholder(R.drawable.ic_baseline_photo_24)
                .error(R.drawable.ic_baseline_photo_24)
                .into(holder.playlist_cover);
        holder.playlist_name.setText(Constant.tagsPlaylistBeanList.get(position).getName());
        holder.playlist_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long id = Constant.tagsPlaylistBeanList.get(position).getId();
                Constant.playlistId = id;
                String url = "http://wyyapi.itaemobile.top/playlist/track/all?id=" + String.valueOf(id);
                sendOkHttpRequest(url, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Constant.musicList.clear();
                        parseJSON(response.body().string());
                        Intent in = new Intent(context , PlaylistActivity.class);
                        in.putExtra("name" , Constant.tagsPlaylistBeanList.get(position).getName());
                        in.putExtra("coverImgUrl" , Constant.tagsPlaylistBeanList.get(position).getCoverImgUrl());
                        in.putExtra("playlistType" , "web");
                        context.startActivity(in);
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return Constant.tagsPlaylistBeanList.size();
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
