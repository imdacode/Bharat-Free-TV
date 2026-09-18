package com.imdacode.bharatfreetv;

public interface PlaybackEngine {
    interface Callback {
        void onBuffering();

        void onPlaying();

        void onError();
    }

    void play(Channel channel, long positionMillis);

    long getCurrentPosition();

    void release();
}
