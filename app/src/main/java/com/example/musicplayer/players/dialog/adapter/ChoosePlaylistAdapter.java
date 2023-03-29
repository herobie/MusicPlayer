package com.example.musicplayer.players.dialog.adapter;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

import java.util.List;

public class ChoosePlaylistAdapter extends RecyclerView.Adapter<ChoosePlaylistAdapter.ViewHolder> {
    private Context context;
    private int itemPosition;
    private String launchFrom;
    public ChoosePlaylistAdapter(Context context , int itemPosition , String launchFrom){
        this.context = context;
        this.itemPosition = itemPosition;
        this.launchFrom = launchFrom;
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

    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.result_playlist_action.setVisibility(View.GONE);
        holder.result_playlist_name.setText(Constant.myPlaylist.get(position).getName());
        holder.result_playlist_creator.setText(Constant.myPlaylist.get(position).getCreator().getNickname());
        Glide.with(context).load(Constant.myPlaylist.get(position).getCoverImgUrl())
                .placeholder(R.drawable.ic_baseline_photo_24)
                .error(R.drawable.ic_baseline_photo_24)
                .into(holder.result_cover);
        holder.result_playlist_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取选中的歌单id
                long id = Constant.myPlaylist.get(position).getId();
                addIntoPlaylist(id);

            }
        });
    }

    public void addIntoPlaylist(long id){
        CRUD crud = new CRUD("music" , context);
        ContentValues values = new ContentValues();
        switch (launchFrom){
            case "local":
                if (!crud.retrieveIfAlreadyInPlaylist(id , Constant.localSongList.get(itemPosition).musicUrl ,Constant.localSongList.get(itemPosition).getId() )){
                    values.put("title" , Constant.localSongList.get(itemPosition).getTitle());
                    values.put("artist" , Constant.localSongList.get(itemPosition).artist);
                    values.put("id" , Constant.localSongList.get(itemPosition).getId());
                    values.put("picUrl" , Constant.localSongList.get(itemPosition).picUrl);
                    values.put("musicUrl" , Constant.localSongList.get(itemPosition).musicUrl);
                    values.put("playlist" , id);
                    values.put("creator" , Constant.activeAccount);
                    Toast.makeText(context, "添加成功!", Toast.LENGTH_SHORT).show();
                    crud.add(values);
                }else {
                    Toast.makeText(context, "歌曲已存在", Toast.LENGTH_SHORT).show();
                }
                break;
            case "search":
                putValues(Constant.searchSingleList , values , id , crud);
                break;
            case "searchPlaylist":
                putValues(Constant.searchMusicInPlaylist , values , id , crud);
                break;
            case "playlist":
                putValues(Constant.musicList , values , id , crud);
                break;
            case "player":
                if (!crud.retrieveIfAlreadyInPlaylist(id , Constant.currentPlayList.get(Constant.currentPosition).musicUrl , Constant.currentPlayList.get(Constant.currentPosition).getId())){
                    values.put("title" , Constant.currentPlayList.get(Constant.currentPosition).title);
                    values.put("artist" , Constant.currentPlayList.get(Constant.currentPosition).artist);
                    values.put("id" , Constant.currentPlayList.get(Constant.currentPosition).getId());
                    values.put("picUrl" , Constant.currentPlayList.get(Constant.currentPosition).picUrl);
                    values.put("playlist" , id);
                    values.put("creator" , Constant.activeAccount);
                    crud.add(values);
                    Toast.makeText(context, "添加成功！", Toast.LENGTH_SHORT).show();
                    //发一个广播，如果添加到的歌单是最喜爱的歌单则刷新一下图标
                    Intent in = new Intent("changeSong");
                    in.putExtra("activityAction" , "refreshIcon");
                    context.sendBroadcast(in);
                }else {
                    Toast.makeText(context, "歌曲已存在", Toast.LENGTH_SHORT).show();
                }
        }
        values.clear();
    }

    //组装values
    public void putValues(List<Song> songList , ContentValues values , long id , CRUD crud){
        if (!crud.retrieveIfAlreadyInPlaylist(id , "null" , songList.get(itemPosition).id)){
            values.put("title" , songList.get(itemPosition).name);
            values.put("artist" ,displayMultipleArtist(itemPosition , songList));
            values.put("id" , songList.get(itemPosition).id);
            values.put("picUrl" , songList.get(itemPosition).al.picUrl);
            values.put("playlist" , id);
            values.put("creator" , Constant.activeAccount);
            Toast.makeText(context, "添加成功!", Toast.LENGTH_SHORT).show();
            crud.add(values);
        }else {
            Toast.makeText(context, "歌曲已存在", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return Constant.myPlaylist.size();
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
