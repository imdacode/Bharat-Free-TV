package com.imdacode.bharatfreetv;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.Arrays;

public final class VlcPlaybackEngine implements PlaybackEngine {
    private final LibVLC libVlc;
    private final MediaPlayer player;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VlcPlaybackEngine(Context context, VLCVideoLayout videoLayout, String aspectMode,
            Callback callback) {
        libVlc = new LibVLC(context, Arrays.asList("--network-caching=1500", "--clock-jitter=0"));
        player = new MediaPlayer(libVlc);
        player.attachViews(videoLayout, null, false, false);
        applyAspectMode(aspectMode);
        player.setEventListener(event -> {
            if (event.type == MediaPlayer.Event.Buffering || event.type == MediaPlayer.Event.Opening) {
                mainHandler.post(callback::onBuffering);
            } else if (event.type == MediaPlayer.Event.Playing) {
                mainHandler.post(callback::onPlaying);
            } else if (event.type == MediaPlayer.Event.EncounteredError) {
                mainHandler.post(callback::onError);
            }
        });
    }

    @Override
    public void play(Channel channel, long positionMillis) {
        Media media = new Media(libVlc, Uri.parse(channel.getStreamUrl()));
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=1500");
        player.setMedia(media);
        media.release();
        player.play();
        if (positionMillis > 0) {
            player.setTime(positionMillis);
        }
    }

    @Override
    public long getCurrentPosition() {
        return Math.max(0L, player.getTime());
    }

    @Override
    public void release() {
        player.stop();
        player.detachViews();
        player.release();
        libVlc.release();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void applyAspectMode(String aspectMode) {
        if (AppPreferences.ASPECT_16_9.equals(aspectMode)) {
            player.setVideoScale(MediaPlayer.ScaleType.SURFACE_16_9);
        } else if (AppPreferences.ASPECT_4_3.equals(aspectMode)) {
            player.setVideoScale(MediaPlayer.ScaleType.SURFACE_4_3);
        } else if (AppPreferences.ASPECT_STRETCH.equals(aspectMode)) {
            player.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL);
        } else {
            player.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL);
        }
    }
}
