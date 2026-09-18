package com.imdacode.bharatfreetv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
            line = stripBom(line.trim());
            if (line.isEmpty() || "#EXTM3U".equalsIgnoreCase(line)) {
                continue;
            }
            if (line.regionMatches(true, 0, "#EXTINF:", 0, 8)) {
                pendingMetadata = parseMetadata(line);
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            if (pendingMetadata != null) {
                int number = pendingMetadata.number > 0
                        ? pendingMetadata.number : channels.size() + 1;
                channels.add(new Channel(
                        number,
                        pendingMetadata.name,
                        CategoryNormalizer.normalize(pendingMetadata.group),
                        pendingMetadata.logoUrl,
                        line,
                        inferQuality(pendingMetadata.name, pendingMetadata.quality)));
                pendingMetadata = null;
            }
        }

        return Collections.unmodifiableList(channels);
    }

    static String inferQuality(String name, String explicitQuality) {
        String explicit = explicitQuality == null ? "" : explicitQuality.trim();
        if (!explicit.isEmpty()) {
            return explicit.toUpperCase(Locale.ROOT);
        }
        String normalizedName = name == null ? "" : name.toUpperCase(Locale.ROOT);
        if (normalizedName.contains("4K") || normalizedName.contains("UHD")) {
            return "UHD";
        }
        if (normalizedName.contains("FHD") || normalizedName.contains("1080")) {
            return "FHD";
        }
        if (normalizedName.contains("HD") || normalizedName.contains("720")) {
            return "HD";
        }
        return "SD";
    }

    private static String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private static ChannelMetadata parseMetadata(String line) {
        int commaIndex = findNameSeparator(line);
        String attributes = commaIndex >= 0 ? line.substring(0, commaIndex) : line;
        String name = commaIndex >= 0 ? line.substring(commaIndex + 1).trim() : "Unknown channel";
        String group = "";
        String logoUrl = "";
        String quality = "";
        int number = 0;

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();
            if ("group-title".equalsIgnoreCase(key) || "category".equalsIgnoreCase(key)) {
                group = value;
            } else if ("tvg-logo".equalsIgnoreCase(key) || "logo".equalsIgnoreCase(key)) {
                logoUrl = value;
            } else if ("tvg-name".equalsIgnoreCase(key) && name.isEmpty()) {
                name = value;
            } else if ("tvg-chno".equalsIgnoreCase(key) || "channel-number".equalsIgnoreCase(key)) {
                number = parsePositiveInt(value);
            } else if ("quality".equalsIgnoreCase(key) || "tvg-quality".equalsIgnoreCase(key)) {
                quality = value;
            }
        }

        if (name.isEmpty()) {
            name = "Unknown channel";
        }
        return new ChannelMetadata(number, name, group, logoUrl, quality);
    }

    private static int findNameSeparator(String value) {
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == ',' && !quoted) {
                return index;
            }
        }
        return -1;
    }

    private static int parsePositiveInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static final class ChannelMetadata {
        private final int number;
        private final String name;
        private final String group;
        private final String logoUrl;
        private final String quality;

        private ChannelMetadata(int number, String name, String group, String logoUrl,
                String quality) {
            this.number = number;
            this.name = name;
            this.group = group;
            this.logoUrl = logoUrl;
            this.quality = quality;
        }
    }
}
