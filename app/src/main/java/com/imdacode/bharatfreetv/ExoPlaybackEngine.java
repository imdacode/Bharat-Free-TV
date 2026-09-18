package com.imdacode.bharatfreetv;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

@OptIn(markerClass = UnstableApi.class)
public final class ExoPlaybackEngine implements PlaybackEngine {
    private final PlayerView playerView;
    private final ExoPlayer player;
    private final Callback callback;

    public ExoPlaybackEngine(Context context, PlayerView playerView, String aspectMode,
            Callback callback) {
        this.playerView = playerView;
        this.callback = callback;
        player = new ExoPlayer.Builder(context).build();
        playerView.setPlayer(player);
        applyAspectMode(aspectMode);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    callback.onBuffering();
                } else if (playbackState == Player.STATE_READY) {
                    callback.onPlaying();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                callback.onError();
            }
        });
    }

    @Override
    public void play(Channel channel, long positionMillis) {
        player.setMediaItem(MediaItem.fromUri(channel.getStreamUrl()));
        player.prepare();
        if (positionMillis > 0) {
            player.seekTo(positionMillis);
        }
        player.play();
    }

    @Override
    public long getCurrentPosition() {
        return Math.max(0L, player.getCurrentPosition());
    }

    @Override
    public void release() {
        playerView.setPlayer(null);
        player.release();
    }

    private void applyAspectMode(String aspectMode) {
        if (AppPreferences.ASPECT_STRETCH.equals(aspectMode)
                || AppPreferences.ASPECT_16_9.equals(aspectMode)
                || AppPreferences.ASPECT_4_3.equals(aspectMode)) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        } else {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        }
    }
}
