package com.griffin3.simplemusic;

public class SongInfo {
    private long id;
    private String artist;
    private String title;
    private long duration;
    private String data;
    private int volume;
    private int likes;

    public SongInfo(long id, String artist, String title, long duration, String data, int volume, int likes) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.duration = duration;
        this.data = data;
        this.volume = volume;
        this.likes = likes;
    }

    // Static factory method to parse from pipe-separated string (from queue)
    public static SongInfo fromQueueString(String queueString) {
        String[] parts = queueString.split("\\|", -1);
        if (parts.length >= 4) {
            String artist = parts[0];
            String title = parts[1];
            long duration = Long.parseLong(parts[2]);
            String data = parts[3];
            return new SongInfo(-1, artist, title, duration, data, 128, 0);
        }
        return null;
    }

    // Static factory method to parse from JSON string (from getCurrentSongInfo)
    public static SongInfo fromJsonString(String jsonString) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonString);
            long id = json.getLong("id");
            String artist = json.getString("artist");
            String title = json.getString("title");
            long duration = json.getLong("duration");
            String data = json.getString("data");
            int volume = json.getInt("volume");
            int likes = json.getInt("likes");
            return new SongInfo(id, artist, title, duration, data, volume, likes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Getters
    public long getId() { return id; }
    public String getArtist() { return artist; }
    public String getTitle() { return title; }
    public long getDuration() { return duration; }
    public String getData() { return data; }
    public int getVolume() { return volume; }
    public int getLikes() { return likes; }

    // Utility method to get filename from data path
    public String getFilename() {
        if (data != null && data.contains("/")) {
            return data.substring(data.lastIndexOf("/") + 1);
        }
        return data;
    }

    // Utility method to format duration as MM:SS
    public String getFormattedDuration() {
        long minutes = duration / 60000;
        long seconds = (duration % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
