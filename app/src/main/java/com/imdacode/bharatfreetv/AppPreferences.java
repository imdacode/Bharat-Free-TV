package com.imdacode.bharatfreetv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AppPreferences {
    private static final String PREFERENCES_NAME = "bharat_free_tv_preferences";
    private static final String KEY_PLAYLIST_URL = "playlist_url";
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
