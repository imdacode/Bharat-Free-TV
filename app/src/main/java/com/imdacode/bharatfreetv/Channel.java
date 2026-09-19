package com.imdacode.bharatfreetv;

import java.util.Objects;

public final class Channel {
    private final int number;
    private final String name;
    private final String category;
    private final String logoUrl;
    private final String streamUrl;
    private final String quality;

    public Channel(int number, String name, String category, String logoUrl, String streamUrl,
            String quality) {
        this.number = number;
        this.name = Objects.requireNonNull(name, "name");
        this.category = Objects.requireNonNull(category, "category");
        this.logoUrl = Objects.requireNonNull(logoUrl, "logoUrl");
        this.streamUrl = Objects.requireNonNull(streamUrl, "streamUrl");
        this.quality = Objects.requireNonNull(quality, "quality");
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getQuality() {
        return quality;
    }
}
