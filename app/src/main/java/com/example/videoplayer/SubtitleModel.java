package com.example.videoplayer;

public class SubtitleModel {
    private String id;
    private String fileName;
    private String language;
    private String downloadUrl;
    private int fileId;

    public SubtitleModel(String id, String fileName, String language, String downloadUrl, int fileId) {
        this.id = id;
        this.fileName = fileName;
        this.language = language;
        this.downloadUrl = downloadUrl;
        this.fileId = fileId;
    }

    public String getId() { return id; }
    public String getFileName() { return fileName; }
    public String getLanguage() { return language; }
    public String getDownloadUrl() { return downloadUrl; }
    public int getFileId() { return fileId; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}
