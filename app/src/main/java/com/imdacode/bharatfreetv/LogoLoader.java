package com.imdacode.bharatfreetv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LogoLoader {
    private static final int MAX_LOGO_BYTES = 2 * 1024 * 1024;

    private final LruCache<String, Bitmap> cache = new LruCache<>(40);
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void load(String url, ImageView imageView) {
        imageView.setTag(url);
        imageView.setImageResource(R.drawable.ic_channel_placeholder);
        if (!PlaylistRepository.isSupportedRemoteUrl(url)) {
            return;
        }
        Bitmap cached = cache.get(url);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }
        executor.execute(() -> {
            Bitmap downloaded = download(url);
            if (downloaded != null) {
                cache.put(url, downloaded);
                mainHandler.post(() -> {
                    if (url.equals(imageView.getTag())) {
                        imageView.setImageBitmap(downloaded);
                    }
                });
            }
        });
    }

    public void close() {
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        cache.evictAll();
    }

    private Bitmap download(String value) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(value).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int total = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_LOGO_BYTES) {
                        return null;
                    }
                    output.write(buffer, 0, read);
                }
                return BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size());
            }
        } catch (IOException ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
