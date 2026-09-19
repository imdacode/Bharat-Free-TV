package com.imdacode.bharatfreetv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public final class ChannelFilterTest {
    private final Channel news = new Channel(
            101, "Bharat News", CategoryNormalizer.NEWS, "", "https://example.com/news", "HD");
    private final Channel movie = new Channel(
            202, "Cinema Plus", CategoryNormalizer.MOVIES, "", "https://example.com/movie", "SD");
    private final List<Channel> channels = Arrays.asList(news, movie);

    @Test
    public void combinesCategoryAndSearchFilters() {
        List<Channel> filtered = ChannelFilter.apply(
                channels, CategoryNormalizer.NEWS, "bharat", Collections.emptySet());

        assertEquals(Collections.singletonList(news), filtered);
    }

    @Test
    public void searchesByChannelNumber() {
        List<Channel> filtered = ChannelFilter.apply(
                channels, CategoryNormalizer.ALL, "202", Collections.emptySet());

        assertEquals(Collections.singletonList(movie), filtered);
    }

    @Test
    public void favoriteCategoryUsesPersistentStreamKeys() {
        List<Channel> filtered = ChannelFilter.apply(
                channels, CategoryNormalizer.FAVORITES, "",
                new HashSet<>(Collections.singletonList(movie.getStreamUrl())));

        assertEquals(Collections.singletonList(movie), filtered);
    }
}
