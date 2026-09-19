package com.imdacode.bharatfreetv;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

public final class SettingsActivity extends ComponentActivity {
    public static final String EXTRA_RELOAD_PLAYLIST = "reload_playlist";

    private AppPreferences preferences;
    private EditText playlistUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        preferences = new AppPreferences(this);
        playlistUrl = findViewById(R.id.playlist_url);

        playlistUrl.setText(preferences.getPlaylistUrl());

        findViewById(R.id.save_settings).setOnClickListener(view -> saveSettings());
        findViewById(R.id.clear_cache_reload).setOnClickListener(view -> clearCacheAndReload());
        findViewById(R.id.close_settings).setOnClickListener(view -> finish());
    }

    private void saveSettings() {
        String url = playlistUrl.getText().toString().trim();
        if (!url.isEmpty() && !PlaylistRepository.isSupportedRemoteUrl(url)) {
            playlistUrl.setError(getString(R.string.invalid_playlist_url));
            playlistUrl.requestFocus();
            return;
        }
        boolean reloadPlaylist = !url.equals(preferences.getPlaylistUrl());
        preferences.setPlaylistUrl(url);
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_RELOAD_PLAYLIST, reloadPlaylist));
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void clearCacheAndReload() {
        PlaylistRepository repository = new PlaylistRepository(this, preferences);
        boolean cleared = repository.clearCache();
        repository.close();
        if (!cleared) {
            Toast.makeText(this, R.string.cache_clear_error, Toast.LENGTH_LONG).show();
            return;
        }
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_RELOAD_PLAYLIST, true));
        Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
        finish();
    }

}
