package com.raenn.subredditimages.pojo;

public class Image {
    private String url;
    private String id;
    private String datetime; //todo: store properly

    public Image(String id, String url, String datetime) {
        this.id = id;
        this.url = url;
        this.datetime = datetime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }
}
