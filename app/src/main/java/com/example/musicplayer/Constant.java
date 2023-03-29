package com.example.musicplayer;

import android.media.MediaPlayer;

import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Bean.playlist.Playlist;
import com.example.musicplayer.Bean.webMusic.Song;
import com.example.musicplayer.profile.ProfileBean;
import com.example.musicplayer.recommendPlaylist.bean.TagsBean;
import com.example.musicplayer.recommendPlaylist.bean.TagsPlaylistBean;

import java.util.ArrayList;
import java.util.List;

public class Constant {
    /**底部导航栏的选项的id
     * @0 发现（随机推荐歌曲等）
     * @1 歌单（发现歌单）
     * @2 我的（收藏的歌单）
     * @3 本地（本地歌曲）
     * @4 个人资料
     */
    public static int[] itemsDisplayOnTheBottom = {0 , 1 , 2 ,3 ,4};
    //当前播放的播放列表
    public static List<SongBean> currentPlayList = new ArrayList<>();
    //用于保存原来的播放顺序
    public static List<SongBean> tmpPlayList = new ArrayList<>();
    //用于存放本地歌单
    public static List<SongBean> localSongList = new ArrayList<>();
    //创建或收藏的歌单
    public static List<Playlist> myPlaylist = new ArrayList<>();
    //存放搜索历史记录
    public static List<String> searchHistory = new ArrayList<>();
    //显示单曲搜索结果
    public static List<Song> searchSingleList = new ArrayList<>();
    //显示歌单搜索结果
    public static List<Playlist> searchPlaylist = new ArrayList<>();
    //显示歌单内歌曲
    public static List<Song> musicList = new ArrayList<>();
    //显示歌单内歌曲搜索结果
    public static List<Song> searchMusicInPlaylist = new ArrayList<>();
    //存放歌单标签
    public static List<TagsBean> tagList = new ArrayList<>();
    //存放由tag找到的歌单
    public static List<TagsPlaylistBean> tagsPlaylistBeanList = new ArrayList<>();



    public static MediaPlayer mediaPlayer = new MediaPlayer();


    //设置搜索上限
    public static String searchLimit = "&limit=12";
    //设置默认音乐品质
    public static String defaultBrLevel = "&level=higher";

    //判断加载的上一次退出时播放的音乐是否进入准备状态
    public static boolean isInitLastPlayMusic = false;
    //当前播放位置
    public static int currentPosition = 0;
    //当前播放模式，0为列表循环，1为单曲循环，2为随机播放
    public static int currentPlayMode = 0;
    //当前显示的歌单id
    public static long playlistId = 0;
    //数据库版本
    public static int version = 2;
    //用户名
    public static String activeAccount = "Default";
    //是否登录
    public static boolean isLogin = false;

    public static ProfileBean profileBean;
}
