package com.imdacode.bharatfreetv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

public final class M3uParserTest {
    @Test
    public void parsesMetadataCategoryNumberAndQuality() throws Exception {
        String playlist = "#EXTM3U\n"
                + "#EXTINF:-1 tvg-chno=\"104\" tvg-logo=\"logo.png\" "
                + "group-title=\"Hindi News\" tvg-quality=\"hd\",Bharat 24\n"
                + "https://example.com/live.m3u8\n";

        List<Channel> channels = M3uParser.parse(new StringReader(playlist));

        assertEquals(1, channels.size());
        Channel channel = channels.get(0);
        assertEquals(104, channel.getNumber());
        assertEquals("Bharat 24", channel.getName());
        assertEquals(CategoryNormalizer.NEWS, channel.getCategory());
        assertEquals("logo.png", channel.getLogoUrl());
        assertEquals("HD", channel.getQuality());
        assertEquals("https://example.com/live.m3u8", channel.getStreamUrl());
    }

    @Test
    public void supportsCommasInsideQuotedAttributes() throws Exception {
        String playlist = "#EXTM3U\n"
                + "#EXTINF:-1 group-title=\"Movies, Hindi\",Cinema HD\n"
                + "https://example.com/movie.m3u8\n";

        Channel channel = M3uParser.parse(new StringReader(playlist)).get(0);

        assertEquals("Cinema HD", channel.getName());
        assertEquals(CategoryNormalizer.MOVIES, channel.getCategory());
        assertEquals("HD", channel.getQuality());
    }

    @Test
    public void ignoresCommentsAndIncompleteEntries() throws Exception {
        String playlist = "\uFEFF#EXTM3U\n"
                + "# a comment\n"
                + "#EXTINF:-1,Missing URL\n"
                + "#EXTINF:-1,Working Channel\n"
                + "https://example.com/working.m3u8\n";

        List<Channel> channels = M3uParser.parse(new StringReader(playlist));

        assertEquals(1, channels.size());
        assertEquals("Working Channel", channels.get(0).getName());
        assertEquals(CategoryNormalizer.OTHER, channels.get(0).getCategory());
        assertEquals(1, channels.get(0).getNumber());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void returnedChannelListIsImmutable() throws Exception {
        List<Channel> channels = M3uParser.parse(new StringReader("#EXTM3U\n"));
        assertTrue(channels.isEmpty());
        channels.add(new Channel(1, "Name", "Other", "", "https://example.com", "SD"));
    }
}
