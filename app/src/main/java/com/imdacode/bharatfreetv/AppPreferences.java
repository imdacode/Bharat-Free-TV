package com.imdacode.bharatfreetv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AppPreferences {
    public static final String ENGINE_EXOPLAYER = "exoplayer";
    public static final String ENGINE_VLC = "vlc";
    public static final String ASPECT_AUTO = "auto";
    public static final String ASPECT_16_9 = "16:9";
    public static final String ASPECT_4_3 = "4:3";
    public static final String ASPECT_STRETCH = "stretch";

    private static final String PREFERENCES_NAME = "bharat_free_tv_preferences";
    private static final String KEY_PLAYLIST_URL = "playlist_url";
    private static final String KEY_ENGINE = "player_engine";
    private static final String KEY_ASPECT_RATIO = "aspect_ratio";
    private static final String KEY_FAVORITES = "favorite_stream_urls";

    private final SharedPreferences preferences;

    public AppPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public String getPlaylistUrl() {
        return preferences.getString(KEY_PLAYLIST_URL, "").trim();
    }

    public void setPlaylistUrl(String url) {
        preferences.edit().putString(KEY_PLAYLIST_URL, url.trim()).apply();
    }

    public String getPlayerEngine() {
        return preferences.getString(KEY_ENGINE, ENGINE_EXOPLAYER);
    }

    public void setPlayerEngine(String engine) {
        preferences.edit().putString(KEY_ENGINE, engine).apply();
    }

    public String getAspectRatio() {
        return preferences.getString(KEY_ASPECT_RATIO, ASPECT_AUTO);
    }

    public void setAspectRatio(String aspectRatio) {
        preferences.edit().putString(KEY_ASPECT_RATIO, aspectRatio).apply();
    }

    public Set<String> getFavorites() {
        Set<String> stored = preferences.getStringSet(KEY_FAVORITES, Collections.emptySet());
        return new HashSet<>(stored == null ? Collections.emptySet() : stored);
    }

    public boolean isFavorite(Channel channel) {
        return getFavorites().contains(channel.getStreamUrl());
    }

    public void toggleFavorite(Channel channel) {
        Set<String> favorites = getFavorites();
        if (!favorites.add(channel.getStreamUrl())) {
            favorites.remove(channel.getStreamUrl());
        }
        preferences.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }
}
