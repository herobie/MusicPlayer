package com.example.musicplayer.profile;

public class ProfileBean {

    public Profile profile;
    public Account account;

    public Profile getProfile() {
        return profile;
    }

    public Account getAccount() {
        return account;
    }

    public class Profile{
        private String avatarUrl;
        //关注
        private int follows;
        //粉丝
        private int followeds;
        private Account account;
        private String signature;
        private int province;
        private int city;
        private String backgroundUrl;
        private String nickname;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public int getFollows() {
            return follows;
        }

        public void setFollows(int follows) {
            this.follows = follows;
        }

        public int getFolloweds() {
            return followeds;
        }

        public void setFolloweds(int followeds) {
            this.followeds = followeds;
        }

        public Account getAccount() {
            return account;
        }

        public void setAccount(Account account) {
            this.account = account;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public int getProvince() {
            return province;
        }

        public void setProvince(int province) {
            this.province = province;
        }

        public int getCity() {
            return city;
        }

        public void setCity(int city) {
            this.city = city;
        }

        public String getBackgroundUrl() {
            return backgroundUrl;
        }

        public void setBackgroundUrl(String backgroundUrl) {
            this.backgroundUrl = backgroundUrl;
        }
    }

    public class Account{
        private int id;

        public int getId() {
            return id;
        }
    }


}
