package com.imdacode.bharatfreetv;

import java.util.Locale;

public final class CategoryNormalizer {
    public static final String ALL = "All";
    public static final String NEWS = "News";
    public static final String MOVIES = "Movies";
    public static final String ENTERTAINMENT = "Entertainment";
    public static final String SPORTS = "Sports";
    public static final String MUSIC = "Music";
    public static final String REGIONAL = "Regional";
    public static final String FAVORITES = "Favorites";
    public static final String OTHER = "Other";

    private CategoryNormalizer() {
    }

    public static String normalize(String rawCategory) {
        String value = rawCategory == null ? "" : rawCategory.trim().toLowerCase(Locale.ROOT);
        if (containsAny(value, "news", "samachar")) {
            return NEWS;
        }
        if (containsAny(value, "movie", "cinema", "film")) {
            return MOVIES;
        }
        if (containsAny(value, "sport", "cricket", "football")) {
            return SPORTS;
        }
        if (containsAny(value, "music", "songs", "radio")) {
            return MUSIC;
        }
        if (containsAny(value, "regional", "hindi", "tamil", "telugu", "malayalam",
                "kannada", "bengali", "marathi", "punjabi", "gujarati", "odia")) {
            return REGIONAL;
        }
        if (containsAny(value, "entertainment", "general", "kids", "lifestyle", "comedy")) {
            return ENTERTAINMENT;
        }
        return OTHER;
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
