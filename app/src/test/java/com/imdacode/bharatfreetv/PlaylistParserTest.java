package com.imdacode.bharatfreetv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

public final class PlaylistParserTest {
    @Test
    public void parsesJsonArrayWithCommonFieldAliases() throws Exception {
        String json = "[{\"channelNumber\":7,\"title\":\"Stadium 4K\","
                + "\"stream_url\":\"https://example.com/sport.m3u8\","
                + "\"category\":\"Cricket\",\"logoUrl\":\"https://example.com/logo.png\"}]";

        List<Channel> channels = PlaylistParser.parse(new StringReader(json));

        assertEquals(1, channels.size());
        assertEquals(7, channels.get(0).getNumber());
        assertEquals("Stadium 4K", channels.get(0).getName());
        assertEquals(CategoryNormalizer.SPORTS, channels.get(0).getCategory());
        assertEquals("UHD", channels.get(0).getQuality());
    }

    @Test
    public void parsesObjectContainingChannelsAndSkipsMissingUrls() throws Exception {
        String json = "{\"channels\":[{\"name\":\"Broken\"},"
                + "{\"name\":\"Music One\",\"url\":\"https://example.com/music.m3u8\","
                + "\"group\":\"Songs\",\"quality\":\"FHD\"}]}";

        List<Channel> channels = PlaylistParser.parse(new StringReader(json));

        assertEquals(1, channels.size());
        assertEquals("Music One", channels.get(0).getName());
        assertEquals(CategoryNormalizer.MUSIC, channels.get(0).getCategory());
        assertEquals("FHD", channels.get(0).getQuality());
    }
}
