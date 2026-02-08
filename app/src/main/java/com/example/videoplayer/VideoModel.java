package com.example.videoplayer;

import android.net.Uri;

public class VideoModel {
    private String title;
    private String duration;
    private long durationMs;
    private Uri videoUri;
    private String folderPath;
    private long size;

    public VideoModel(String title, String duration, Uri videoUri) {
        this.title = title;
        this.duration = duration;
        this.videoUri = videoUri;
    }

    public VideoModel(String title, String duration, Uri videoUri, String folderPath) {
        this.title = title;
        this.duration = duration;
        this.videoUri = videoUri;
        this.folderPath = folderPath;
    }

    public VideoModel(String title, String duration, Uri videoUri, String folderPath, long size) {
        this.title = title;
        this.duration = duration;
        this.videoUri = videoUri;
        this.folderPath = folderPath;
        this.size = size;
    }

    public VideoModel(String title, String duration, Uri videoUri, String folderPath, long size, long durationMs) {
        this.title = title;
        this.duration = duration;
        this.videoUri = videoUri;
        this.folderPath = folderPath;
        this.size = size;
        this.durationMs = durationMs;
    }

    public String getTitle() { return title; }
    public String getDuration() { return duration; }
    public long getDurationMs() { return durationMs; }
    public Uri getVideoUri() { return videoUri; }
    public String getFolderPath() { return folderPath; }
    public long getSize() { return size; }
}
