package com.example.musicplayer.Sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Bean.playlist.Playlist;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.Constant;

public class CRUD {
    private SQLiteDatabase db;
    private String name;
    private Context context;

    public CRUD(String name , Context context){
        this.name = name;
        this.context = context;
        SQLiteOpenHelper sqLiteOpenHelper = new UserDataBase(context , name , null , Constant.version);
        db = sqLiteOpenHelper.getWritableDatabase();
    }

    /**
     * 往表里添加数据
     */
    public void add(ContentValues values){
        db.insert(name,null,values);
    }

    /**
     * 清空“上次播放列表”，以便下一次进行读取
     */
    public void deleteLastPlaylist(){
        db.delete("lastPlay" , null , null);
    }

    /**
     * 获取上次退出时的播放列表
     */
    public void retrieveLastPlaylist(){
        Cursor cursor = db.query("lastPlay" , null , null , null , null , null , null);
        if (cursor.moveToFirst()){
            do{
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                String musicUrl = cursor.getString(cursor.getColumnIndexOrThrow("musicUrl"));
                String picUrl = cursor.getString(cursor.getColumnIndexOrThrow("picUrl"));
                int time = cursor.getInt(cursor.getColumnIndexOrThrow("time"));
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String isPlaying = cursor.getString(cursor.getColumnIndexOrThrow("isPlaying"));
                int position = cursor.getInt(cursor.getColumnIndexOrThrow("position"));
                SongBean songBean = new SongBean(title , artist , musicUrl , picUrl , time , id , "web");
                Constant.currentPlayList.add(songBean);
                if (isPlaying.equals("true")){
                    Constant.currentPosition = position;
                }
            }while (cursor.moveToNext());
        }
        cursor.close();
        Log.d("initLastPlaylist" , "上次播放列表加载完毕");
    }

    /**
     * 保存退出时的播放列表
     */
    public void saveCurrentPlaylist(){
        //先执行清空表，为接下来的保存列表腾出位置
        deleteLastPlaylist();
        for (int i = 0 ; i < Constant.currentPlayList.size() ; i++){
            ContentValues values = new ContentValues();
            values.put("title" , Constant.currentPlayList.get(i).title);
            values.put("artist" , Constant.currentPlayList.get(i).artist);
            values.put("musicUrl" , Constant.currentPlayList.get(i).musicUrl);
            values.put("picUrl" , Constant.currentPlayList.get(i).picUrl);
            values.put("time" , Constant.currentPlayList.get(i).time);
            values.put("id" , Constant.currentPlayList.get(i).getId());
            values.put("position" , i);
            if (i == Constant.currentPosition){
                values.put("isPlaying" , "true");
            }else {
                values.put("isPlaying" , "false");
            }
            Log.d("executeSaveProgress" , String.valueOf(i));
            add(values);
            values.clear();
        }
    }

    /**
     * 查询搜索记录
     */
    public void retrieveSearchHistory(){
        Constant.searchHistory.clear();
        //先从后往前检索最新加入数据库的几个数据
        Cursor cursor = db.query("searchHistory" , new String[]{"content"} , "creator = ?" , new String[]{Constant.activeAccount} , null , null , "id DESC");
        if (cursor.moveToFirst()){
            int i = 0;
            //调出16个搜索结果
            while (i < 16){
                try{
                    String historyItem = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                    Constant.searchHistory.add(historyItem);
                    cursor.moveToNext();
                    i++;
                }catch (Exception e){
                    //如果不到16个上面的getString方法会报错，于是就在这里直接停止循环
                    break;
                }
            }
        }
        cursor.close();
    }

    /**
     * 清空搜索记录
     */
    public void clearSearchHistory(){
        Constant.searchHistory.clear();
        db.delete("searchHistory" , "creator = ?" , new String[]{Constant.activeAccount});
    }


    /**
     * 删除搜索记录中的相同元素
     * @param potentialSameElement 可能相同的元素（即搜索栏中输入的）
     */
    public void deleteSameElementsInHistory(String potentialSameElement){
        Constant.searchHistory.clear();
        db.delete("searchHistory" , "content = ?" , new String[]{potentialSameElement});
    }

    /**
     * 从歌单中删除歌曲
     * @param playlist 歌单id
     * @param id 歌曲id
     */
    public void deleteMusicFromPlaylist(long playlist , int id){
        db.delete("music" , "playlist = ? AND id = ? AND creator = ?" , new String[]{String.valueOf(playlist) , String.valueOf(id) , Constant.activeAccount});
    }

    /**
     * 判断当前playlist表中有无”喜爱的歌单”这一歌单
     * @return 目标歌单存在状况
     */
    public boolean isFavoritePlaylistExist(){
        boolean isExist = false;
        Cursor cursor = db.query("playlist" , new String[]{"name" , "nickname" , "id"} , "name = ? AND nickname = ? AND id = ?" , new String[]{"我喜欢的音乐" , Constant.activeAccount , "0"} , null , null ,null );
        if (cursor.moveToFirst()){
            isExist = true;
        }
        cursor.close();
        return isExist;
    }

    /**
     * 判断歌单里有没有至少一首歌（用于返回歌单封面）
     * @param id 歌单id
     * @return 是否有至少一首歌
     */
    public boolean isAtLeastOneMusicExistInPlaylist(long id){
        boolean isExist = false;
        Cursor cursor = db.query("music" , new String[]{"playlist" , "creator"} , "playlist = ? AND creator = ?" , new String[]{String.valueOf(id) , Constant.activeAccount} , null , null , null );
        if (cursor.moveToFirst()){
            isExist = true;
        }
        cursor.close();
        return isExist;
    }

    /**
     * 获取歌单中第一首歌的封面url
     * @param id 歌单id
     * @return 封面url
     */
    public String getTheFirstSongCover(long id){
        String picUrl = null;
        Cursor cursor = db.query("music" , new String[]{ "creator" , "picUrl"} , "playlist = ? AND creator = ?" , new String[]{String.valueOf(id) , Constant.activeAccount} , null , null , "musicid DESC");
        if (cursor.moveToFirst()){
            picUrl = cursor.getString(cursor.getColumnIndexOrThrow("picUrl"));
        }
        return picUrl;
    }

    /**
     * 更新歌单封面url
     * @param id 目标歌单id
     * @param values 元素
     */
    public void updatePicUrl(long id , ContentValues values){
        db.update("playlist" , values , "id = ? AND nickname = ?" , new String[]{String.valueOf(id) , Constant.activeAccount});
    }

    /**
     * 检索保存的歌单
     * @param type 歌单类型（创建的/收藏的)
     */
    public void retrievePlaylist(String type){
        Constant.myPlaylist.clear();
        if (!type.equals("all")){//查询create或save
            Cursor cursor = db.query("playlist" , new String[]{"nickname" , "type" , "name" , "orderBy" , "id" , "coverImgUrl"} , "nickname = ? AND type = ?" , new String[]{Constant.activeAccount , type} , null ,null ,"playlistid ASC" );
            if (cursor.moveToFirst()){
                do{
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String coverImgUrl = cursor.getString(cursor.getColumnIndexOrThrow("coverImgUrl"));
                    String nickname = cursor.getString(cursor.getColumnIndexOrThrow("nickname"));
                    String orderBy = cursor.getString(cursor.getColumnIndexOrThrow("orderBy"));
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    Playlist playlist = new Playlist(name , nickname , coverImgUrl , id , orderBy);
                    Constant.myPlaylist.add(playlist);
                }while (cursor.moveToNext());
            }
            cursor.close();
        }else {//查询全部
            Cursor cursor = db.query("playlist", new String[]{"nickname", "type", "name", "orderBy", "id", "coverImgUrl"}, "nickname = ?", new String[]{Constant.activeAccount}, null, null, "type ASC");
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String coverImgUrl = cursor.getString(cursor.getColumnIndexOrThrow("coverImgUrl"));
                    String nickname = cursor.getString(cursor.getColumnIndexOrThrow("nickname"));
                    String orderBy = cursor.getString(cursor.getColumnIndexOrThrow("orderBy"));
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    Playlist playlist = new Playlist(name, nickname, coverImgUrl, id, orderBy);
                    Constant.myPlaylist.add(playlist);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    /**
     * 检索歌单中的歌曲
     * @param id 歌单id
     * @param orderBy 排列方式
     */
    public void retrieveMusicInPlaylist(long id , String orderBy){
        Constant.musicList.clear();
        Cursor cursor = null;
        if (orderBy == null){
            orderBy = "";
        }
        //判断有无指定排序方式
        if (!orderBy.contains("DESC") || !orderBy.contains("ASC") || !orderBy.contains("customize")){
            cursor = db.query("music" , new String[]{"creator" , "playlist" , "title" , "artist" , "id" , "picUrl"} , "creator = ? AND playlist = ?" , new String[]{Constant.activeAccount , String.valueOf(id)} , null , null , null);
        }else {
            cursor = db.query("music" , new String[]{"creator" , "playlist" , "title" , "artist" , "id" , "picUrl"} , "creator = ? AND playlist = ?" , new String[]{Constant.activeAccount , String.valueOf(id)} , null , null , orderBy);
        }
        if (cursor.moveToFirst()){
            do{
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                //这个是歌曲的id
                int id1 = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String picUrl = cursor.getString(cursor.getColumnIndexOrThrow("picUrl"));
                Song song = new Song(title , artist , id1 , picUrl);
                Constant.musicList.add(song);
            }while (cursor.moveToNext());
        }
        cursor.close();
    }

    /**
     * 根据歌单id来获取歌单内歌曲
     * @param id 歌单id
     * @return 歌曲id
     */
    public long retrieveWebMusicInPlaylist(long id){
        long musicId = 0;
        Cursor cursor = db.query("playlist" , new String[]{"name" , "coverImgUrl" , "id" , "nickname" , "type"} , "nickname = ? AND id = ? AND type = ?" , new String[]{Constant.activeAccount , String.valueOf(id) , "save"} , null , null , null);
        if (cursor.moveToFirst()){
            do{
                musicId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            }while (cursor.moveToNext());
        }
        return musicId;
    }

    /**
     * 检查该音乐是否已经存在于某个歌单中
     * @param playlistId 歌单id
     * @param musicUrl 音乐url
     * @return 存在状态
     */
    public boolean retrieveIfAlreadyInPlaylist(long playlistId , String musicUrl , int id){
        boolean isExist = false;
        //根据歌单id查找
        Cursor cursor = db.query("music" , new String[]{"creator" , "musicUrl" , "id"} , "creator = ? AND playlist = ?" , new String[]{Constant.activeAccount , String.valueOf(playlistId) } , null , null ,null);
        if (cursor.moveToFirst()){
            do{
                //这里会比较音乐的url或者id，因为本地音乐是没有id的，只能比url，网络音乐url和id都有，但是存储的Song类没有musicUrl这个变量
                if (id == cursor.getInt(cursor.getColumnIndexOrThrow("id")) || musicUrl.equals(cursor.getString(cursor.getColumnIndexOrThrow("musicUrl")))){
                    isExist = true;
                    break;
                }
            }while (cursor.moveToNext());
        }
        cursor.close();
        return isExist;
    }

    /**
     * 检查歌单id是否和已经存在的id相同
     * @param id 歌单id
     * @return 存在状态
     */
    public boolean isPlaylistIdExist(long id){
        boolean isExist = false;
        Cursor cursor = db.query("playlist" , new String[]{"nickname" , "id"} , "nickname = ? AND id = ?" , new String[]{Constant.activeAccount , String.valueOf(id)} , null , null , null);
        if (cursor.moveToFirst()){
            isExist = true;
        }
        cursor.close();
        return isExist;
    }

    /**
     * 更新歌单名
     * @param values 元素
     * @param playlistId 歌单id
     */
    public void updatePlaylistName(ContentValues values , long playlistId){
        db.update("playlist" , values , "nickname = ? AND id = ?" , new String[]{Constant.activeAccount , String.valueOf(playlistId)});
    }

    /**
     * 删除歌单
     * @param id 歌单id
     */
    public void deletePlaylist(long id){
        db.delete("playlist" , "nickname = ? AND id = ?" , new String[]{Constant.activeAccount , String.valueOf(id)});
    }
}
