package com.example.musicplayer.me;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;

public class SavedPlaylistActionDialog extends Dialog implements View.OnClickListener{
    private Button saved_rename , saved_delete , saved_save;
    private String launchFrom;
    private long id;
    private String name , coverImgUrl , nickname;
    public SavedPlaylistActionDialog(@NonNull Context context ,String name , String coverImgUrl , long id , String launchFrom) {
        super(context , R.style.DialogBaseStyle);
        this.launchFrom = launchFrom;
        this.id = id;
        this.coverImgUrl = coverImgUrl;
        this.name = name;
    }

    public SavedPlaylistActionDialog(@NonNull Context context ,String name , String nickname ,String coverImgUrl , long id , String launchFrom) {
        super(context , R.style.DialogBaseStyle);
        this.launchFrom = launchFrom;
        this.id = id;
        this.coverImgUrl = coverImgUrl;
        this.name = name;
    }

    public SavedPlaylistActionDialog(@NonNull Context context , long id , String launchFrom){
        super(context);
        this.id = id;
        this.launchFrom = launchFrom;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_saved_playlist_action);
        initView();
    }

    public void initView(){
        saved_delete = findViewById(R.id.saved_delete);
        saved_delete.setOnClickListener(this);
        saved_rename = findViewById(R.id.saved_rename);
        saved_rename.setOnClickListener(this);
        saved_save = findViewById(R.id.saved_save);
        saved_save.setOnClickListener(this);
        if (launchFrom.equals("search")){
            saved_delete.setVisibility(View.GONE);
            saved_rename.setVisibility(View.GONE);
            //根据收藏状态来决定该按钮的文字
            if (new CRUD("playlist" , getContext()).isPlaylistIdExist(id)){
                saved_save.setText("取消收藏");
            }else {
                saved_save.setText("收藏");
            }
        }else if (launchFrom.equals("save")){
            saved_save.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.saved_save:
                //收藏歌单
                CRUD crud = new CRUD("playlist" , getContext());
                //如果收藏的歌单中没有相同的id则进行收藏操作
                if (!crud.isPlaylistIdExist(id)){
                    ContentValues values = new ContentValues();
                    values.put("name" , name);
                    values.put("nickname" , Constant.activeAccount);
                    values.put("coverImgUrl" , coverImgUrl);
                    values.put("type" , "save");
                    values.put("id" , id);
                    crud.add(values);
                    Toast.makeText(getContext(), "收藏成功", Toast.LENGTH_SHORT).show();
                    saved_save.setText("取消收藏");
                    dismiss();
                }else {
                    crud.deletePlaylist(id);
                    saved_save.setText("收藏");
                    Toast.makeText(getContext(), "取消收藏", Toast.LENGTH_SHORT).show();
                    Intent in = new Intent("refreshAdapter");
                    getContext().sendBroadcast(in);
                    dismiss();
                }
                break;
            case R.id.saved_rename:
                NewPlaylistDialog newPlaylistDialog = new NewPlaylistDialog(getContext() , "rename");
                newPlaylistDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //刷新布局
                        Intent in = new Intent("refreshAdapter");
                        getContext().sendBroadcast(in);
                    }
                });
                newPlaylistDialog.show();
                break;
            case R.id.saved_delete:
                new CRUD("playlist" , getContext()).deletePlaylist(id);
                Toast.makeText(getContext(), "操作成功", Toast.LENGTH_SHORT).show();
                dismiss();
                Intent in = new Intent("refreshAdapter");
                getContext().sendBroadcast(in);
                break;
        }
    }
}
