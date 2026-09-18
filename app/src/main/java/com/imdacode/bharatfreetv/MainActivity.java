package com.imdacode.bharatfreetv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.videolan.libvlc.util.VLCVideoLayout;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends ComponentActivity implements ChannelAdapter.Listener {
    private static final int SETTINGS_REQUEST = 100;
    private static final long WELCOME_DURATION_MILLIS = 2300L;
    private static final long INFO_BAR_DURATION_MILLIS = 5000L;
    private static final List<String> CATEGORIES = Arrays.asList(
            CategoryNormalizer.ALL,
            CategoryNormalizer.NEWS,
            CategoryNormalizer.MOVIES,
            CategoryNormalizer.ENTERTAINMENT,
            CategoryNormalizer.SPORTS,
            CategoryNormalizer.MUSIC,
            CategoryNormalizer.REGIONAL,
            CategoryNormalizer.FAVORITES);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideInfoBar = this::hideInfoBarNow;
    private final Runnable updateClock = new Runnable() {
        @Override
        public void run() {
            headerClock.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
            handler.postDelayed(this, 30_000L);
        }
    };

    private AppPreferences preferences;
    private PlaylistRepository playlistRepository;
    private LogoLoader logoLoader;
    private PlaybackEngine playbackEngine;
    private ChannelAdapter channelAdapter;
    private List<Channel> allChannels = Collections.emptyList();
    private Channel currentChannel;
    private String activeCategory = CategoryNormalizer.ALL;
    private long playbackPosition;
    private boolean started;

    private VideoViewport videoViewport;
    private PlayerView exoPlayerView;
    private VLCVideoLayout vlcVideoView;
    private View playerInputLayer;
    private View welcomeOverlay;
    private View navigationOverlay;
    private View infoBar;
    private TextView statusView;
    private TextView headerClock;
    private EditText searchInput;
    private LinearLayout categoryContainer;
    private RecyclerView channelList;
    private TextView emptyState;
    private TextView infoNumber;
    private ImageView infoLogo;
    private TextView infoName;
    private TextView infoMeta;
    private TextView infoStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enterImmersiveMode();

        preferences = new AppPreferences(this);
        playlistRepository = new PlaylistRepository(this, preferences);
        logoLoader = new LogoLoader();
        bindViews();
        setupNavigation();
        setupBackNavigation();

        videoViewport.setAspectMode(preferences.getAspectRatio());
        welcomeOverlay.setVisibility(View.VISIBLE);
        handler.postDelayed(this::hideWelcome, WELCOME_DURATION_MILLIS);
        loadPlaylist();
    }

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
        initializePlaybackEngine();
        if (currentChannel != null) {
            playbackEngine.play(currentChannel, playbackPosition);
        }
        handler.post(updateClock);
    }

    @Override
    protected void onStop() {
        started = false;
        handler.removeCallbacks(updateClock);
        releasePlaybackEngine();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        playlistRepository.close();
        logoLoader.close();
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isMenuVisible() && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU)) {
            showMenu();
            return true;
        }
        if (!isMenuVisible() && (keyCode == KeyEvent.KEYCODE_CHANNEL_UP
                || keyCode == KeyEvent.KEYCODE_PAGE_UP || keyCode == KeyEvent.KEYCODE_DPAD_UP)) {
            changeChannel(-1);
            return true;
        }
        if (!isMenuVisible() && (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN
                || keyCode == KeyEvent.KEYCODE_PAGE_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)) {
            changeChannel(1);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onChannelSelected(Channel channel) {
        playChannel(channel, 0L);
        hideMenu();
        playerInputLayer.requestFocus();
    }

    @Override
    public void onFavoriteToggled(Channel channel) {
        preferences.toggleFavorite(channel);
        applyFilters();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST && resultCode == RESULT_OK) {
            videoViewport.setAspectMode(preferences.getAspectRatio());
            if (data != null && data.getBooleanExtra(SettingsActivity.EXTRA_RELOAD_PLAYLIST, false)) {
                loadPlaylist();
            }
        }
    }

    private void bindViews() {
        videoViewport = findViewById(R.id.video_viewport);
        exoPlayerView = findViewById(R.id.exo_player_view);
        vlcVideoView = findViewById(R.id.vlc_player_view);
        playerInputLayer = findViewById(R.id.player_input_layer);
        welcomeOverlay = findViewById(R.id.welcome_overlay);
        navigationOverlay = findViewById(R.id.navigation_overlay);
        infoBar = findViewById(R.id.info_bar);
        statusView = findViewById(R.id.status);
        headerClock = findViewById(R.id.header_clock);
        searchInput = findViewById(R.id.search_input);
        categoryContainer = findViewById(R.id.category_container);
        channelList = findViewById(R.id.channel_list);
        emptyState = findViewById(R.id.empty_state);
        infoNumber = findViewById(R.id.info_channel_number);
        infoLogo = findViewById(R.id.info_channel_logo);
        infoName = findViewById(R.id.info_channel_name);
        infoMeta = findViewById(R.id.info_channel_meta);
        infoStatus = findViewById(R.id.info_playing_status);
    }

    private void setupNavigation() {
        channelAdapter = new ChannelAdapter(this, logoLoader);
        channelList.setLayoutManager(new LinearLayoutManager(this));
        channelList.setAdapter(channelAdapter);

        for (String category : CATEGORIES) {
            Button button = (Button) getLayoutInflater().inflate(
                    R.layout.item_category, categoryContainer, false);
            button.setText(category);
            button.setSelected(CategoryNormalizer.ALL.equals(category));
            button.setOnClickListener(view -> selectCategory(category));
            categoryContainer.addView(button);
        }

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });

        playerInputLayer.setOnClickListener(view -> showMenu());
        findViewById(R.id.menu_button).setOnClickListener(view -> toggleMenu());
        findViewById(R.id.settings_button).setOnClickListener(view -> {
            hideMenu();
            startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_REQUEST);
        });
        playerInputLayer.requestFocus();
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isMenuVisible()) {
                    hideMenu();
                    playerInputLayer.requestFocus();
                } else {
                    finish();
                }
            }
        });
    }

    private void loadPlaylist() {
        showStatus(getString(R.string.loading_playlist));
        playlistRepository.load(new PlaylistRepository.Callback() {
            @Override
            public void onLoaded(List<Channel> channels, boolean fromCache) {
                allChannels = channels;
                hideStatus();
                applyFilters();
                if (fromCache) {
                    Toast.makeText(MainActivity.this, R.string.using_cached_playlist,
                            Toast.LENGTH_LONG).show();
                }
                if (!allChannels.isEmpty()) {
                    Channel reloadedChannel = findByStreamUrl(currentChannel);
                    playChannel(reloadedChannel == null ? allChannels.get(0) : reloadedChannel,
                            reloadedChannel == null ? 0L : playbackPosition);
                }
            }

            @Override
            public void onError(String message) {
                allChannels = Collections.emptyList();
                applyFilters();
                showStatus(message);
                showMenu();
            }
        });
    }

    private void initializePlaybackEngine() {
        if (playbackEngine != null) {
            return;
        }
        String aspectMode = preferences.getAspectRatio();
        PlaybackEngine.Callback callback = new PlaybackEngine.Callback() {
            @Override
            public void onBuffering() {
                if (currentChannel != null) {
                    infoStatus.setText(R.string.buffering);
                    showStatus(getString(R.string.loading_channel, currentChannel.getName()));
                }
            }

            @Override
            public void onPlaying() {
                infoStatus.setText(R.string.now_playing);
                hideStatus();
            }

            @Override
            public void onError() {
                infoStatus.setText(R.string.unavailable);
                showStatus(getString(R.string.playback_error));
                showMenu();
            }
        };

        if (AppPreferences.ENGINE_VLC.equals(preferences.getPlayerEngine())) {
            try {
                exoPlayerView.setVisibility(View.GONE);
                vlcVideoView.setVisibility(View.VISIBLE);
                playbackEngine = new VlcPlaybackEngine(this, vlcVideoView, aspectMode, callback);
            } catch (RuntimeException | LinkageError error) {
                preferences.setPlayerEngine(AppPreferences.ENGINE_EXOPLAYER);
                Toast.makeText(this, R.string.vlc_start_error, Toast.LENGTH_LONG).show();
            }
        }
        if (playbackEngine == null) {
            vlcVideoView.setVisibility(View.GONE);
            exoPlayerView.setVisibility(View.VISIBLE);
            playbackEngine = new ExoPlaybackEngine(this, exoPlayerView, aspectMode, callback);
        }
    }

    private void releasePlaybackEngine() {
        if (playbackEngine == null) {
            return;
        }
        playbackPosition = playbackEngine.getCurrentPosition();
        playbackEngine.release();
        playbackEngine = null;
    }

    private void playChannel(Channel channel, long positionMillis) {
        currentChannel = channel;
        playbackPosition = positionMillis;
        channelAdapter.setSelectedChannel(channel);
        showInfoBar(channel);
        showStatus(getString(R.string.loading_channel, channel.getName()));
        if (started) {
            initializePlaybackEngine();
            playbackEngine.play(channel, positionMillis);
        }
    }

    private void changeChannel(int direction) {
        if (allChannels.isEmpty()) {
            showMenu();
            return;
        }
        int currentIndex = currentChannel == null ? 0 : allChannels.indexOf(currentChannel);
        int nextIndex = (currentIndex + direction + allChannels.size()) % allChannels.size();
        playChannel(allChannels.get(nextIndex), 0L);
    }

    private Channel findByStreamUrl(Channel channel) {
        if (channel == null) {
            return null;
        }
        for (Channel candidate : allChannels) {
            if (candidate.getStreamUrl().equals(channel.getStreamUrl())) {
                return candidate;
            }
        }
        return null;
    }

    private void selectCategory(String category) {
        activeCategory = category;
        for (int index = 0; index < categoryContainer.getChildCount(); index++) {
            View button = categoryContainer.getChildAt(index);
            button.setSelected(category.contentEquals(((Button) button).getText()));
        }
        applyFilters();
    }

    private void applyFilters() {
        if (channelAdapter == null) {
            return;
        }
        List<Channel> filtered = ChannelFilter.apply(
                allChannels, activeCategory, searchInput.getText().toString(),
                preferences.getFavorites());
        channelAdapter.submit(filtered, preferences.getFavorites());
        channelAdapter.setSelectedChannel(currentChannel);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        channelList.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showInfoBar(Channel channel) {
        handler.removeCallbacks(hideInfoBar);
        infoNumber.setText(String.format(Locale.US, "%03d", channel.getNumber()));
        infoName.setText(channel.getName());
        infoMeta.setText(getString(R.string.info_meta, channel.getCategory(), channel.getQuality(),
                AppPreferences.ENGINE_VLC.equals(preferences.getPlayerEngine()) ? "VLC" : "ExoPlayer"));
        infoStatus.setText(R.string.buffering);
        logoLoader.load(channel.getLogoUrl(), infoLogo);
        infoBar.setAlpha(1f);
        infoBar.setVisibility(View.VISIBLE);
        handler.postDelayed(hideInfoBar, INFO_BAR_DURATION_MILLIS);
    }

    private void hideInfoBarNow() {
        infoBar.animate().alpha(0f).setDuration(220L)
                .withEndAction(() -> infoBar.setVisibility(View.GONE)).start();
    }

    private void showMenu() {
        navigationOverlay.setAlpha(0f);
        navigationOverlay.setVisibility(View.VISIBLE);
        navigationOverlay.animate().alpha(1f).setDuration(180L).start();
        if (channelAdapter.getItemCount() > 0) {
            channelList.requestFocus();
        } else {
            findViewById(R.id.settings_button).requestFocus();
        }
    }

    private void hideMenu() {
        navigationOverlay.animate().alpha(0f).setDuration(150L)
                .withEndAction(() -> navigationOverlay.setVisibility(View.GONE)).start();
    }

    private void toggleMenu() {
        if (isMenuVisible()) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    private boolean isMenuVisible() {
        return navigationOverlay.getVisibility() == View.VISIBLE;
    }

    private void hideWelcome() {
        welcomeOverlay.animate().alpha(0f).setDuration(350L)
                .withEndAction(() -> welcomeOverlay.setVisibility(View.GONE)).start();
    }

    private void showStatus(String message) {
        statusView.setText(message);
        statusView.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusView.setVisibility(View.GONE);
    }

    private void enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
