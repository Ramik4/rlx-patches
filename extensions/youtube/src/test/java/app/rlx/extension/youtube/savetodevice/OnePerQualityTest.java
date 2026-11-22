/*
 * Copyright 2026 RLX Patches (https://github.com/Ramik4/rlx-patches).
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.rlx.extension.youtube.savetodevice;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class OnePerQualityTest {

    private static OfflineFormats.Stream video(int height, String codec) {
        return new OfflineFormats.Stream("", 0, "", 0, height, 30, codec, 0, true, false, false);
    }

    private static List<String> labels(List<OfflineFormats.Stream> streams) {
        List<String> labels = new ArrayList<>();
        for (OfflineFormats.Stream stream : streams) labels.add(stream.height() + " " + stream.codec());
        return labels;
    }

    @Test
    public void keepsTheFirstStreamOfEachQuality() {
        List<OfflineFormats.Stream> sorted = List.of(
                video(2160, OfflineFormats.CODEC_AV1), video(2160, OfflineFormats.CODEC_VP9),
                video(1080, OfflineFormats.CODEC_H264), video(1080, OfflineFormats.CODEC_AV1),
                video(1080, OfflineFormats.CODEC_VP9), video(480, OfflineFormats.CODEC_H264),
                video(480, OfflineFormats.CODEC_VP9), video(360, OfflineFormats.CODEC_H264),
                video(144, OfflineFormats.CODEC_AV1));
        assertEquals(List.of("2160 AV1", "1080 H.264", "480 H.264"),
                labels(OfflinePickerSheet.onePerQuality(sorted)));
    }

    @Test
    public void keepsTheLowQualitiesOfVideosWithout480p() {
        List<OfflineFormats.Stream> sorted = List.of(
                video(360, OfflineFormats.CODEC_H264), video(360, OfflineFormats.CODEC_VP9),
                video(240, OfflineFormats.CODEC_AV1), video(144, OfflineFormats.CODEC_H264));
        assertEquals(List.of("360 H.264", "240 AV1", "144 H.264"),
                labels(OfflinePickerSheet.onePerQuality(sorted)));
    }
}
