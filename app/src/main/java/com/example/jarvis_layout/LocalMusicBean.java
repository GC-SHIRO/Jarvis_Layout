package com.example.jarvis_layout;

public class LocalMusicBean {
    private String id;
    private String song;
    private String singer;
    private String duration;
    private String path;

    public LocalMusicBean() {
    }

    public LocalMusicBean(String id, String song, String singer, String duration, String path) {
        this.id = id;
        this.song = song;
        this.singer = singer;
        this.duration = duration;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
