package com.example.musicplayer.Bean;


//本地音乐
public class SongBean {
    public String artist , title, musicUrl, duration , type , picUrl;
    public int time;
    //这里的id是音乐的id，不是url!!!
    private int id;
    private boolean isSelected = false;

    public SongBean(String title, String artist , String musicUrl, String type){
        this.title = title;
        this.artist = artist;
        this.musicUrl = musicUrl;
        this.duration = duration;
        this.type = type;
    }

    public SongBean(String title, String artist , String musicUrl, String picUrl , int time , int id , String type){
        this.title = title;
        this.artist = artist;
        this.musicUrl = musicUrl;
        this.type = type;
        this.picUrl = picUrl;
        this.time = time;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getDuration() {
        return duration;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
