package com.imdacode.bharatfreetv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

public final class M3uParserTest {
    @Test
    public void parsesExtendedMetadataAndStreamUrl() throws Exception {
        String playlist = "#EXTM3U\n"
                + "#EXTINF:-1 tvg-logo=\"logo.png\" group-title=\"News\",Aaj Tak\n"
                + "https://example.com/live.m3u8\n";

        List<Channel> channels = M3uParser.parse(new StringReader(playlist));

        assertEquals(1, channels.size());
        assertEquals("Aaj Tak", channels.get(0).getName());
        assertEquals("News", channels.get(0).getGroup());
        assertEquals("logo.png", channels.get(0).getLogoUrl());
        assertEquals("https://example.com/live.m3u8", channels.get(0).getStreamUrl());
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
        assertEquals("Other", channels.get(0).getGroup());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void returnedChannelListIsImmutable() throws Exception {
        List<Channel> channels = M3uParser.parse(new StringReader("#EXTM3U\n"));
        assertTrue(channels.isEmpty());
        channels.add(new Channel("Name", "Group", "", "https://example.com"));
    }
}
