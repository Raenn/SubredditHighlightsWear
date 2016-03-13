package com.raenn.subredditimages.pojo;

public class Image {
    private String url;
    private String id;
    private String datetime; //todo: store properly
    private boolean isOver18;

    public Image(String id, String url, String datetime, boolean isOver18) {
        this.id = id;
        this.url = url;
        this.datetime = datetime;
        this.isOver18 = isOver18;
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

    public boolean isOver18() {
        return isOver18;
    }

    public void setOver18(boolean over18) {
        isOver18 = over18;
    }
}
