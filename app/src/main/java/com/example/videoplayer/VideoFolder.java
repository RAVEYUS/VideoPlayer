package com.example.videoplayer;

public class VideoFolder {
    String id;
    String name;
    int videoCount;
    long totalSize;
    String firstVideoPath;

    public VideoFolder(String id, String name, int videoCount) {
        this.id = id;
        this.name = name;
        this.videoCount = videoCount;
    }

    public VideoFolder(String id, String name, int videoCount, long totalSize) {
        this.id = id;
        this.name = name;
        this.videoCount = videoCount;
        this.totalSize = totalSize;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getVideoCount() { return videoCount; }
    public long getTotalSize() { return totalSize; }
    public String getFirstVideoPath() { return firstVideoPath; }

    public void setVideoCount(int videoCount) { this.videoCount = videoCount; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    public void setFirstVideoPath(String firstVideoPath) { this.firstVideoPath = firstVideoPath; }
}
