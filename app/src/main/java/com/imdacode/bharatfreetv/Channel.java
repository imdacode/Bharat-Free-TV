package com.imdacode.bharatfreetv;

import java.util.Objects;

public final class Channel {
    private final String name;
    private final String group;
    private final String logoUrl;
    private final String streamUrl;

    public Channel(String name, String group, String logoUrl, String streamUrl) {
        this.name = Objects.requireNonNull(name, "name");
        this.group = Objects.requireNonNull(group, "group");
        this.logoUrl = Objects.requireNonNull(logoUrl, "logoUrl");
        this.streamUrl = Objects.requireNonNull(streamUrl, "streamUrl");
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }
}
