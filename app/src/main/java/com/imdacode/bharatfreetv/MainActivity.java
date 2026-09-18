package com.imdacode.bharatfreetv;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public final class MainActivity extends ComponentActivity {
    private static final String PLAYLIST_FILE = "playlist.m3u";

    private PlayerView playerView;
    private TextView statusView;
    private ExoPlayer player;
    private List<Channel> channels = Collections.emptyList();
    private int selectedChannel;
    private long playbackPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        statusView = findViewById(R.id.status);
        RecyclerView channelList = findViewById(R.id.channel_list);
        channelList.setLayoutManager(new LinearLayoutManager(this));

        try {
            channels = loadChannels();
            ChannelAdapter adapter = new ChannelAdapter(channels, this::playChannel);
            channelList.setAdapter(adapter);
            if (channels.isEmpty()) {
                showStatus(getString(R.string.no_channels));
            }
        } catch (IOException exception) {
            showStatus(getString(R.string.playlist_error));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
    }

    @Override
    protected void onStop() {
        releasePlayer();
        super.onStop();
    }

    private List<Channel> loadChannels() throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                getAssets().open(PLAYLIST_FILE), StandardCharsets.UTF_8)) {
            return M3uParser.parse(reader);
        }
    }

    private void initializePlayer() {
        if (player != null || channels.isEmpty()) {
            return;
        }
        player = new ExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    showStatus(getString(R.string.loading_channel, channels.get(selectedChannel).getName()));
                } else if (playbackState == Player.STATE_READY) {
                    hideStatus();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                showStatus(getString(R.string.playback_error));
            }
        });
        playerView.setPlayer(player);
        prepareChannel(channels.get(selectedChannel), playbackPosition);
    }

    private void playChannel(int position, Channel channel) {
        selectedChannel = position;
        playbackPosition = 0L;
        if (player == null) {
            initializePlayer();
        } else {
            prepareChannel(channel, 0L);
        }
    }

    private void prepareChannel(Channel channel, long position) {
        if (player == null) {
            return;
        }
        showStatus(getString(R.string.loading_channel, channel.getName()));
        player.setMediaItem(MediaItem.fromUri(channel.getStreamUrl()));
        player.prepare();
        player.seekTo(position);
        player.play();
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

    private void showStatus(String message) {
        statusView.setText(message);
        statusView.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusView.setVisibility(View.GONE);
    }
}
