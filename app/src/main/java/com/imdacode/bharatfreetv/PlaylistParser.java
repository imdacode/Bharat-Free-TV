package com.imdacode.bharatfreetv;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlaylistParser {
    private PlaylistParser() {
    }

    public static List<Channel> parse(Reader reader) throws IOException {
        String content = readContent(reader).trim();
        if (content.startsWith("{") || content.startsWith("[")) {
            return parseJson(content);
        }
        return M3uParser.parse(new StringReader(content));
    }

    private static List<Channel> parseJson(String content) throws IOException {
        try {
            Object root = new JSONTokener(content).nextValue();
            JSONArray items;
            if (root instanceof JSONArray) {
                items = (JSONArray) root;
            } else if (root instanceof JSONObject) {
                items = ((JSONObject) root).optJSONArray("channels");
                if (items == null) {
                    throw new IOException("JSON playlist must contain a channels array");
                }
            } else {
                throw new IOException("Unsupported JSON playlist structure");
            }

            List<Channel> channels = new ArrayList<>();
            for (int index = 0; index < items.length(); index++) {
                JSONObject item = items.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                String url = firstNonBlank(item, "url", "streamUrl", "stream_url", "link");
                if (url.isEmpty()) {
                    continue;
                }
                String name = firstNonBlank(item, "name", "title", "channelName");
                if (name.isEmpty()) {
                    name = "Channel " + (channels.size() + 1);
                }
                String group = firstNonBlank(item, "group", "category", "group-title");
                String logo = firstNonBlank(item, "logo", "logoUrl", "tvg-logo");
                String quality = firstNonBlank(item, "quality", "resolution");
                int fallbackNumber = channels.size() + 1;
                int number = item.optInt("number", item.optInt("channelNumber", fallbackNumber));
                channels.add(new Channel(
                        Math.max(1, number),
                        name,
                        CategoryNormalizer.normalize(group),
                        logo,
                        url,
                        M3uParser.inferQuality(name, quality)));
            }
            return Collections.unmodifiableList(channels);
        } catch (JSONException exception) {
            throw new IOException("Invalid JSON playlist", exception);
        }
    }

    private static String readContent(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            content.append(buffer, 0, read);
        }
        return content.toString();
    }

    private static String firstNonBlank(JSONObject item, String... keys) {
        for (String key : keys) {
            String value = item.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }
}
