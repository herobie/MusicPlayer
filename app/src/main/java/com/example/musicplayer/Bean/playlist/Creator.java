package com.example.musicplayer.Bean.playlist;

public class Creator {
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public Creator (){

    }

    public Creator(String nickname){
        this.nickname = nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
