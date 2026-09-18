package com.imdacode.bharatfreetv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PlaylistRepository {
    public interface Callback {
        void onLoaded(List<Channel> channels, boolean fromCache);

        void onError(String message);
    }

    private static final String BUNDLED_PLAYLIST = "playlist.m3u";
    private static final String CACHE_FILE = "playlist-cache.data";
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private static final int MAX_PLAYLIST_BYTES = 10 * 1024 * 1024;

    private final Context context;
    private final AppPreferences preferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean closed;

    public PlaylistRepository(Context context, AppPreferences preferences) {
        this.context = context.getApplicationContext();
        this.preferences = preferences;
    }

    public void load(Callback callback) {
        executor.execute(() -> loadInBackground(callback));
    }

    public boolean clearCache() {
        File cacheFile = getCacheFile();
        return !cacheFile.exists() || cacheFile.delete();
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }

    public static boolean isSupportedRemoteUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void loadInBackground(Callback callback) {
        String remoteUrl = preferences.getPlaylistUrl();
        if (!remoteUrl.isEmpty()) {
            try {
                String content = download(remoteUrl);
                List<Channel> channels = parse(content);
                if (channels.isEmpty()) {
                    throw new IOException("The remote playlist contains no playable channels");
                }
                writeCache(content);
                postLoaded(callback, channels, false);
                return;
            } catch (IOException remoteFailure) {
                try {
                    List<Channel> cachedChannels = parse(readCache());
                    if (!cachedChannels.isEmpty()) {
                        postLoaded(callback, cachedChannels, true);
                        return;
                    }
                } catch (IOException ignored) {
                    // Report the remote error below when no usable cache exists.
                }
                postError(callback, remoteFailure.getMessage());
                return;
            }
        }

        try (InputStreamReader reader = new InputStreamReader(
                context.getAssets().open(BUNDLED_PLAYLIST), StandardCharsets.UTF_8)) {
            List<Channel> channels = PlaylistParser.parse(reader);
            if (channels.isEmpty()) {
                postError(callback, context.getString(R.string.configure_playlist_message));
            } else {
                postLoaded(callback, channels, false);
            }
        } catch (IOException exception) {
            postError(callback, context.getString(R.string.playlist_error));
        }
    }

    private String download(String remoteUrl) throws IOException {
        if (!isSupportedRemoteUrl(remoteUrl)) {
            throw new IOException(context.getString(R.string.invalid_playlist_url));
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(remoteUrl).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/x-mpegURL, application/json, text/plain, */*");
        connection.setRequestProperty("User-Agent", "BharatFreeTV/2.0");
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Playlist server returned HTTP " + responseCode);
            }
            int contentLength = connection.getContentLength();
            if (contentLength > MAX_PLAYLIST_BYTES) {
                throw new IOException("Playlist exceeds the 10 MB size limit");
            }
            try (InputStream input = connection.getInputStream()) {
                return new String(readLimited(input), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_PLAYLIST_BYTES) {
                throw new IOException("Playlist exceeds the 10 MB size limit");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private List<Channel> parse(String content) throws IOException {
        return PlaylistParser.parse(new StringReader(content));
    }

    private void writeCache(String content) throws IOException {
        try (OutputStream output = new FileOutputStream(getCacheFile())) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readCache() throws IOException {
        File cacheFile = getCacheFile();
        if (!cacheFile.isFile()) {
            throw new IOException("No cached playlist");
        }
        try (InputStream input = new FileInputStream(cacheFile)) {
            return new String(readLimited(input), StandardCharsets.UTF_8);
        }
    }

    private File getCacheFile() {
        return new File(context.getCacheDir(), CACHE_FILE);
    }

    private void postLoaded(Callback callback, List<Channel> channels, boolean fromCache) {
        if (!closed) {
            mainHandler.post(() -> {
                if (!closed) {
                    callback.onLoaded(channels, fromCache);
                }
            });
        }
    }

    private void postError(Callback callback, String message) {
        String safeMessage = message == null || message.trim().isEmpty()
                ? context.getString(R.string.playlist_error) : message;
        if (!closed) {
            mainHandler.post(() -> {
                if (!closed) {
                    callback.onError(safeMessage);
                }
            });
        }
    }
}
