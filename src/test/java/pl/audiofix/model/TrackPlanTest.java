package pl.audiofix.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackPlanTest {

    @Test
    void dtsTrackDefaultsToPcm() {
        var dts = stream(StreamType.AUDIO, "dts", "DTS-HD MA", true);

        var plan = TrackPlan.defaultsFor(dts);

        assertEquals(AudioCodec.PCM_S24LE, plan.getTargetCodec());
    }

    @Test
    void nonDtsAudioDefaultsToCopy() {
        var ac3 = stream(StreamType.AUDIO, "ac3", null, false);

        var plan = TrackPlan.defaultsFor(ac3);

        assertEquals(AudioCodec.COPY, plan.getTargetCodec());
    }

    @Test
    void trueHdTrackDefaultsToPcm() {
        var trueHd = stream(StreamType.AUDIO, "truehd", "Dolby TrueHD + Dolby Atmos", true);

        assertEquals(AudioCodec.PCM_S24LE, TrackPlan.defaultsFor(trueHd).getTargetCodec());
    }

    @Test
    void eac3TrackDefaultsToCopy() {
        var eac3 = stream(StreamType.AUDIO, "eac3", null, false);

        assertEquals(AudioCodec.COPY, TrackPlan.defaultsFor(eac3).getTargetCodec());
    }

    @Test
    void nonAudioTrackHasNoTargetCodec() {
        var subtitle = stream(StreamType.SUBTITLE, "subrip", null, false);
        var video = stream(StreamType.VIDEO, "hevc", "Main 10", true);

        assertNull(TrackPlan.defaultsFor(subtitle).getTargetCodec());
        assertNull(TrackPlan.defaultsFor(video).getTargetCodec());
    }

    @Test
    void everyTrackIsKeptByDefault() {
        assertTrue(TrackPlan.defaultsFor(stream(StreamType.AUDIO, "dts", null, false)).isKeep());
        assertTrue(TrackPlan.defaultsFor(stream(StreamType.SUBTITLE, "subrip", null, false)).isKeep());
    }

    @Test
    void makeDefaultFollowsSourceDisposition() {
        assertTrue(TrackPlan.defaultsFor(stream(StreamType.AUDIO, "dts", null, true)).isMakeDefault());
        assertFalse(TrackPlan.defaultsFor(stream(StreamType.AUDIO, "ac3", null, false)).isMakeDefault());
    }

    @Test
    void settersUpdateProperties() {
        var plan = TrackPlan.defaultsFor(stream(StreamType.AUDIO, "dts", null, true));

        plan.setKeep(false);
        plan.setTargetCodec(AudioCodec.AC3);
        plan.setMakeDefault(false);

        assertFalse(plan.keepProperty().get());
        assertEquals(AudioCodec.AC3, plan.targetCodecProperty().get());
        assertFalse(plan.isMakeDefault());
    }

    private static StreamInfo stream(StreamType type, String codec, String profile, boolean isDefault) {
        int channels = type == StreamType.AUDIO ? 6 : 0;
        return new StreamInfo(1, type, codec, profile, channels, null, "eng", null, isDefault);
    }
}
