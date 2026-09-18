package com.imdacode.bharatfreetv;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

public final class SettingsActivity extends ComponentActivity {
    public static final String EXTRA_RELOAD_PLAYLIST = "reload_playlist";

    private AppPreferences preferences;
    private EditText playlistUrl;
    private RadioGroup engineGroup;
    private RadioGroup aspectGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        preferences = new AppPreferences(this);
        playlistUrl = findViewById(R.id.playlist_url);
        engineGroup = findViewById(R.id.engine_group);
        aspectGroup = findViewById(R.id.aspect_group);

        playlistUrl.setText(preferences.getPlaylistUrl());
        engineGroup.check(AppPreferences.ENGINE_VLC.equals(preferences.getPlayerEngine())
                ? R.id.engine_vlc : R.id.engine_exoplayer);
        checkAspectPreference();

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
        preferences.setPlayerEngine(engineGroup.getCheckedRadioButtonId() == R.id.engine_vlc
                ? AppPreferences.ENGINE_VLC : AppPreferences.ENGINE_EXOPLAYER);
        preferences.setAspectRatio(selectedAspectPreference());
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

    private void checkAspectPreference() {
        String aspect = preferences.getAspectRatio();
        if (AppPreferences.ASPECT_16_9.equals(aspect)) {
            aspectGroup.check(R.id.aspect_16_9);
        } else if (AppPreferences.ASPECT_4_3.equals(aspect)) {
            aspectGroup.check(R.id.aspect_4_3);
        } else if (AppPreferences.ASPECT_STRETCH.equals(aspect)) {
            aspectGroup.check(R.id.aspect_stretch);
        } else {
            aspectGroup.check(R.id.aspect_auto);
        }
    }

    private String selectedAspectPreference() {
        int checkedId = aspectGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.aspect_16_9) {
            return AppPreferences.ASPECT_16_9;
        }
        if (checkedId == R.id.aspect_4_3) {
            return AppPreferences.ASPECT_4_3;
        }
        if (checkedId == R.id.aspect_stretch) {
            return AppPreferences.ASPECT_STRETCH;
        }
        return AppPreferences.ASPECT_AUTO;
    }
}
