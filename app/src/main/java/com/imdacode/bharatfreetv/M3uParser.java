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
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "([A-Za-z0-9_-]+)\\s*=\\s*(?:\\\"([^\\\"]*)\\\"|'([^']*)'|([^\\s,]+))");

    private M3uParser() {
    }

    public static List<Channel> parse(Reader reader) throws IOException {
        BufferedReader input = new BufferedReader(reader);
        List<Channel> channels = new ArrayList<>();
        Metadata pending = null;
        String line;

        while ((line = input.readLine()) != null) {
            String value = stripBom(line.trim());
            if (value.isEmpty() || value.equalsIgnoreCase("#EXTM3U")) {
                continue;
            }
            if (value.regionMatches(true, 0, "#EXTINF:", 0, 8)) {
                pending = parseExtInf(value);
                continue;
            }
            if (value.regionMatches(true, 0, "#EXTGRP:", 0, 8) && pending != null) {
                pending = pending.withGroup(value.substring(8).trim());
                continue;
            }
            if (value.startsWith("#")) {
                continue;
            }
            if (pending == null) {
                continue;
            }

            int channelNumber = pending.number > 0 ? pending.number : channels.size() + 1;
            channels.add(new Channel(
                    channelNumber,
                    pending.name,
                    CategoryNormalizer.normalize(pending.group),
                    pending.logoUrl,
                    value,
                    inferQuality(pending.name, pending.quality)));
            pending = null;
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

    private static Metadata parseExtInf(String line) {
        int separator = findNameSeparator(line);
        String attributes = separator >= 0 ? line.substring(0, separator) : line;
        String displayName = separator >= 0 ? line.substring(separator + 1).trim() : "";
        String group = "";
        String logoUrl = "";
        String quality = "";
        String tvgName = "";
        int number = 0;

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = firstNonNull(matcher.group(2), matcher.group(3), matcher.group(4)).trim();
            if ("group-title".equalsIgnoreCase(key) || "category".equalsIgnoreCase(key)) {
                group = value;
            } else if ("tvg-logo".equalsIgnoreCase(key) || "logo".equalsIgnoreCase(key)) {
                logoUrl = value;
            } else if ("tvg-name".equalsIgnoreCase(key)) {
                tvgName = value;
            } else if ("tvg-chno".equalsIgnoreCase(key)
                    || "channel-number".equalsIgnoreCase(key)) {
                number = parsePositiveInt(value);
            } else if ("quality".equalsIgnoreCase(key)
                    || "tvg-quality".equalsIgnoreCase(key)) {
                quality = value;
            }
        }

        String name = displayName.isEmpty() ? tvgName : displayName;
        if (name.isEmpty()) {
            name = "Unknown channel";
        }
        return new Metadata(number, name, group, logoUrl, quality);
    }

    private static int findNameSeparator(String line) {
        char quote = 0;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if ((character == '\"' || character == '\'') && (quote == 0 || quote == character)) {
                quote = quote == 0 ? character : 0;
            } else if (character == ',' && quote == 0) {
                return index;
            }
        }
        return -1;
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private static int parsePositiveInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private static final class Metadata {
        private final int number;
        private final String name;
        private final String group;
        private final String logoUrl;
        private final String quality;

        private Metadata(int number, String name, String group, String logoUrl, String quality) {
            this.number = number;
            this.name = name;
            this.group = group;
            this.logoUrl = logoUrl;
            this.quality = quality;
        }

        private Metadata withGroup(String replacementGroup) {
            if (!group.isEmpty() || replacementGroup.isEmpty()) {
                return this;
            }
            return new Metadata(number, name, replacementGroup, logoUrl, quality);
        }
    }
}
