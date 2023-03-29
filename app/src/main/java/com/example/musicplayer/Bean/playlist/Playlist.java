package com.example.musicplayer.Bean.playlist;

import java.util.List;

public class Playlist {
    private String name;
    private String coverImgUrl;
    private Creator creator;
    private long id;
    private String orderBy;

    public Playlist(String name , String nickname , String coverImgUrl ,long id ,  String orderBy){
        this.name = name;
        this.creator = new Creator(nickname);
        this.coverImgUrl = coverImgUrl;
        this.orderBy = orderBy;
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public String getName() {
        return name;
    }

    public String getCoverImgUrl() {
        return coverImgUrl;
    }

    public void setCoverImgUrl(String coverImgUrl) {
        this.coverImgUrl = coverImgUrl;
    }

    public Creator getCreator() {
        return creator;
    }

    public long getId() {
        return id;
    }

}
