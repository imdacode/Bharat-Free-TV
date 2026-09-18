package com.imdacode.bharatfreetv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3uParser {
    private static final Pattern ATTRIBUTE_PATTERN =
            Pattern.compile("([A-Za-z0-9_-]+)=\\\"([^\\\"]*)\\\"");

    private M3uParser() {
    }

    public static List<Channel> parse(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<Channel> channels = new ArrayList<>();
        ChannelMetadata pendingMetadata = null;
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            if (line.isEmpty() || "#EXTM3U".equalsIgnoreCase(line)) {
                continue;
            }
            if (line.startsWith("#EXTINF:")) {
                pendingMetadata = parseMetadata(line);
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            if (pendingMetadata != null) {
                channels.add(new Channel(
                        pendingMetadata.name,
                        pendingMetadata.group,
                        pendingMetadata.logoUrl,
                        line));
                pendingMetadata = null;
            }
        }

        return Collections.unmodifiableList(channels);
    }

    private static ChannelMetadata parseMetadata(String line) {
        int commaIndex = line.indexOf(',');
        String attributes = commaIndex >= 0 ? line.substring(0, commaIndex) : line;
        String name = commaIndex >= 0 ? line.substring(commaIndex + 1).trim() : "Unknown channel";
        String group = "Other";
        String logoUrl = "";

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            if ("group-title".equalsIgnoreCase(key)) {
                group = value;
            } else if ("tvg-logo".equalsIgnoreCase(key)) {
                logoUrl = value;
            } else if ("tvg-name".equalsIgnoreCase(key) && name.isEmpty()) {
                name = value;
            }
        }

        if (name.isEmpty()) {
            name = "Unknown channel";
        }
        return new ChannelMetadata(name, group, logoUrl);
    }

    private static final class ChannelMetadata {
        private final String name;
        private final String group;
        private final String logoUrl;

        private ChannelMetadata(String name, String group, String logoUrl) {
            this.name = name;
            this.group = group;
            this.logoUrl = logoUrl;
        }
    }
}
