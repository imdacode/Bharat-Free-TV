package com.imdacode.bharatfreetv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ChannelFilter {
    private ChannelFilter() {
    }

    public static List<Channel> apply(List<Channel> channels, String category, String query,
            Set<String> favoriteUrls) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Channel> filtered = new ArrayList<>();
        for (Channel channel : channels) {
            boolean categoryMatch = CategoryNormalizer.ALL.equals(category)
                    || (CategoryNormalizer.FAVORITES.equals(category)
                    && favoriteUrls.contains(channel.getStreamUrl()))
                    || channel.getCategory().equals(category);
            boolean searchMatch = normalizedQuery.isEmpty()
                    || channel.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || channel.getCategory().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || String.valueOf(channel.getNumber()).contains(normalizedQuery);
            if (categoryMatch && searchMatch) {
                filtered.add(channel);
            }
        }
        return Collections.unmodifiableList(filtered);
    }
}
