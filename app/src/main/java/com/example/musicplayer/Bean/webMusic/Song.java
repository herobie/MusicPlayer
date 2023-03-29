package com.example.musicplayer.Bean.webMusic;

import java.util.ArrayList;
import java.util.List;

public class Song {
    public String name;
    public int id;
    public List<Artist> ar;
    public Album al;
    public List<Artist> getAr() {
        return ar;
    }

    private boolean isSelected = false;
    private int position;

    public Song(String name , String artist , int id , String picUrl){
        this.name = name;
        this.id = id;
        this.al = new Album();
        al.picUrl = picUrl;
        Artist artist1 = new Artist();
        artist1.name = artist;
        ar = new ArrayList<>();
        ar.add(artist1);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
