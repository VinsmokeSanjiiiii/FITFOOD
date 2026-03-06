package com.fitfood.app;

public class HealthTip {
    private String title_en;
    private String title_tl;
    private String content_en;
    private String content_tl;
    private String icon;

    public HealthTip() {
    }

    public HealthTip(String title_en, String title_tl, String content_en, String content_tl, String icon) {
        this.title_en = title_en;
        this.title_tl = title_tl;
        this.content_en = content_en;
        this.content_tl = content_tl;
        this.icon = icon;
    }

    public String getTitle_en() {
        return title_en;
    }

    public String getTitle_tl() {
        return title_tl;
    }

    public String getContent_en() {
        return content_en;
    }

    public String getContent_tl() {
        return content_tl;
    }

    public String getIcon() {
        return icon;
    }
}
