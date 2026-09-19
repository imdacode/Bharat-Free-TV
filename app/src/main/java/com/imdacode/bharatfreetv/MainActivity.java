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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@OptIn(markerClass = UnstableApi.class)
public final class MainActivity extends ComponentActivity implements ChannelAdapter.Listener {
    private static final long INFO_BAR_DURATION_MILLIS = 4_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideInfoBar = this::hideInfoBarNow;
    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null
                        && result.getData().getBooleanExtra(
                        SettingsActivity.EXTRA_RELOAD_PLAYLIST, false)) {
                    loadPlaylist();
                }
            });

    private AppPreferences preferences;
    private PlaylistRepository playlistRepository;
    private LogoLoader logoLoader;
    private ExoPlayer player;
    private ChannelAdapter channelAdapter;
    private List<Channel> allChannels = Collections.emptyList();
    private Channel currentChannel;
    private String activeCategory = CategoryNormalizer.ALL;
    private long playbackPosition;

    private PlayerView playerView;
    private View navigationOverlay;
    private View infoBar;
    private EditText searchInput;
    private LinearLayout categoryContainer;
    private RecyclerView channelList;
    private TextView emptyState;
    private TextView infoNumber;
    private ImageView infoLogo;
    private TextView infoName;
    private TextView infoCategory;
    private TextView infoQuality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enterImmersiveMode();

        preferences = new AppPreferences(this);
        playlistRepository = new PlaylistRepository(this, preferences);
        logoLoader = new LogoLoader();
        bindViews();
        configurePlayerSurface();
        configureGuide();
        configureBackNavigation();
        loadPlaylist();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
        if (currentChannel != null) {
            startPlayback(currentChannel, playbackPosition);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
    }

    @Override
    protected void onStop() {
        releasePlayer();
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
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
            showMenu();
            return true;
        }
        if (isMenuVisible() && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            hideMenu();
            playerView.requestFocus();
            return true;
        }
        if (!isMenuVisible() && (keyCode == KeyEvent.KEYCODE_CHANNEL_UP
                || keyCode == KeyEvent.KEYCODE_PAGE_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_UP)) {
            changeChannel(-1);
            return true;
        }
        if (!isMenuVisible() && (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN
                || keyCode == KeyEvent.KEYCODE_PAGE_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)) {
            changeChannel(1);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onChannelSelected(Channel channel) {
        playChannel(channel, 0L);
        hideMenu();
        playerView.requestFocus();
    }

    @Override
    public void onFavoriteToggled(Channel channel) {
        preferences.toggleFavorite(channel);
        applyFilters();
    }

    private void bindViews() {
        playerView = findViewById(R.id.exo_player_view);
        navigationOverlay = findViewById(R.id.navigation_overlay);
        infoBar = findViewById(R.id.info_bar);
        searchInput = findViewById(R.id.search_input);
        categoryContainer = findViewById(R.id.category_container);
        channelList = findViewById(R.id.channel_list);
        emptyState = findViewById(R.id.empty_state);
        infoNumber = findViewById(R.id.info_channel_number);
        infoLogo = findViewById(R.id.info_channel_logo);
        infoName = findViewById(R.id.info_channel_name);
        infoCategory = findViewById(R.id.info_channel_category);
        infoQuality = findViewById(R.id.info_quality_badge);
    }

    private void configurePlayerSurface() {
        playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        playerView.setOnClickListener(view -> toggleMenu());
        playerView.requestFocus();
    }

    private void configureGuide() {
        channelAdapter = new ChannelAdapter(this, logoLoader);
        channelList.setLayoutManager(new LinearLayoutManager(this));
        channelList.setAdapter(channelAdapter);

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

        findViewById(R.id.settings_button).setOnClickListener(view -> {
            hideMenu();
            settingsLauncher.launch(new Intent(this, SettingsActivity.class));
        });
    }

    private void configureBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isMenuVisible()) {
                    hideMenu();
                    playerView.requestFocus();
                } else {
                    finish();
                }
            }
        });
    }

    private void initializePlayer() {
        if (player != null) {
            return;
        }
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    scheduleInfoBarHide();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                handler.removeCallbacks(hideInfoBar);
                Toast.makeText(MainActivity.this, R.string.playback_error,
                        Toast.LENGTH_LONG).show();
                showMenu();
            }
        });
    }

    private void releasePlayer() {
        if (player == null) {
            return;
        }
        playbackPosition = player.getCurrentPosition();
        playerView.setPlayer(null);
        player.release();
        player = null;
    }

    private void loadPlaylist() {
        playlistRepository.load(new PlaylistRepository.Callback() {
            @Override
            public void onLoaded(List<Channel> channels, boolean fromCache) {
                allChannels = channels;
                rebuildCategoryTabs();
                applyFilters();
                if (fromCache) {
                    Toast.makeText(MainActivity.this, R.string.using_cached_playlist,
                            Toast.LENGTH_LONG).show();
                }
                if (!allChannels.isEmpty()) {
                    Channel reloaded = findByStreamUrl(currentChannel);
                    playChannel(reloaded == null ? allChannels.get(0) : reloaded,
                            reloaded == null ? 0L : playbackPosition);
                }
            }

            @Override
            public void onError(String message) {
                allChannels = Collections.emptyList();
                rebuildCategoryTabs();
                applyFilters();
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                showMenu();
            }
        });
    }

    private void playChannel(Channel channel, long positionMillis) {
        currentChannel = channel;
        playbackPosition = positionMillis;
        channelAdapter.setSelectedChannel(channel);
        showInfoBar(channel);
        if (player != null) {
            startPlayback(channel, positionMillis);
        }
    }

    private void startPlayback(Channel channel, long positionMillis) {
        player.setMediaItem(MediaItem.fromUri(channel.getStreamUrl()));
        player.prepare();
        if (positionMillis > 0L) {
            player.seekTo(positionMillis);
        }
        player.play();
    }

    private void changeChannel(int direction) {
        if (allChannels.isEmpty()) {
            showMenu();
            return;
        }
        int currentIndex = currentChannel == null ? -1 : allChannels.indexOf(currentChannel);
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

    private void rebuildCategoryTabs() {
        Set<String> categories = new LinkedHashSet<>();
        categories.add(CategoryNormalizer.ALL);
        for (Channel channel : allChannels) {
            categories.add(channel.getCategory());
        }
        categories.add(CategoryNormalizer.FAVORITES);
        if (!categories.contains(activeCategory)) {
            activeCategory = CategoryNormalizer.ALL;
        }

        categoryContainer.removeAllViews();
        for (String category : categories) {
            Button button = (Button) getLayoutInflater().inflate(
                    R.layout.item_category, categoryContainer, false);
            button.setText(category);
            button.setSelected(category.equals(activeCategory));
            button.setOnClickListener(view -> selectCategory(category));
            categoryContainer.addView(button);
        }
    }

    private void selectCategory(String category) {
        activeCategory = category;
        for (int index = 0; index < categoryContainer.getChildCount(); index++) {
            Button button = (Button) categoryContainer.getChildAt(index);
            button.setSelected(category.contentEquals(button.getText()));
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
        infoBar.animate().cancel();
        infoNumber.setText(String.format(Locale.US, "%03d", channel.getNumber()));
        infoName.setText(channel.getName());
        infoCategory.setText(getString(R.string.info_category, channel.getCategory()));
        infoQuality.setText(channel.getQuality());
        logoLoader.load(channel.getLogoUrl(), infoLogo);
        infoBar.setAlpha(1f);
        infoBar.setVisibility(View.VISIBLE);
    }

    private void scheduleInfoBarHide() {
        handler.removeCallbacks(hideInfoBar);
        handler.postDelayed(hideInfoBar, INFO_BAR_DURATION_MILLIS);
    }

    private void hideInfoBarNow() {
        infoBar.animate().alpha(0f).setDuration(220L)
                .withEndAction(() -> infoBar.setVisibility(View.GONE)).start();
    }

    private void showMenu() {
        navigationOverlay.animate().cancel();
        navigationOverlay.setAlpha(0f);
        navigationOverlay.setVisibility(View.VISIBLE);
        navigationOverlay.animate().alpha(1f).setDuration(180L).start();
        channelList.post(() -> {
            if (channelAdapter.getItemCount() > 0) {
                channelList.requestFocus();
            } else {
                findViewById(R.id.settings_button).requestFocus();
            }
        });
    }

    private void hideMenu() {
        navigationOverlay.animate().cancel();
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

    private void enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
