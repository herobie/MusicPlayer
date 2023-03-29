package com.example.musicplayer.Sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserDataBase extends SQLiteOpenHelper {

    public UserDataBase(Context context , String name , SQLiteDatabase.CursorFactory factory , int version){
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //上次播放列表,这里记得加一个creator区分播放者
        db.execSQL("CREATE TABLE IF NOT EXISTS lastPlay(title TEXT , artist TEXT , musicUrl TEXT , picUrl TEXT , time INTEGER , id INTEGER , isPlaying TEXT , position INTEGER)");
        //搜索历史记录
        db.execSQL("CREATE TABLE IF NOT EXISTS searchHistory (id INTEGER PRIMARY KEY AUTOINCREMENT , content TEXT , creator TEXT)");
        //本地收藏和创建的歌单,type区分收藏或创建，orderBy记录排序方式
        db.execSQL("CREATE TABLE IF NOT EXISTS playlist(playlistid INTEGER PRIMARY KEY AUTOINCREMENT , name TEXT , coverImgUrl TEXT , nickname TEXT , id INTEGER , type TEXT , orderBy TEXT)");
        //保存创建的歌单中的歌曲,id用于获取musicUrl,playlist是整型（即保存的是playlist的id而非名称）
        db.execSQL("CREATE TABLE IF NOT EXISTS music(musicid INTEGER PRIMARY KEY AUTOINCREMENT , position INTEGER , title TEXT , artist TEXT , id INTEGER , picUrl TEXT , musicUrl TEXT , playlist INTEGER , creator TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
