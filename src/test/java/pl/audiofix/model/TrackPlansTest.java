package pl.audiofix.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackPlansTest {

    // ---------------------------------------------------------------- setDefault

    @Test
    void setDefaultMakesChosenAudioTheOnlyDefault() {
        List<TrackPlan> plans = plans();   // 1 TrueHD is default

        TrackPlans.setDefault(plans, plans.get(3));   // DTS

        assertEquals(List.of(3), defaultIndexes(plans, StreamType.AUDIO));
    }

    @Test
    void setDefaultOnAudioDoesNotTouchSubtitles() {
        List<TrackPlan> plans = plans();
        plans.get(5).setMakeDefault(true);

        TrackPlans.setDefault(plans, plans.get(2));

        assertEquals(List.of(5), defaultIndexes(plans, StreamType.SUBTITLE));
    }

    @Test
    void setDefaultOnSubtitleMakesItTheOnlyDefaultSubtitle() {
        List<TrackPlan> plans = plans();
        plans.get(4).setMakeDefault(true);

        TrackPlans.setDefault(plans, plans.get(5));

        assertEquals(List.of(5), defaultIndexes(plans, StreamType.SUBTITLE));
        assertEquals(List.of(1), defaultIndexes(plans, StreamType.AUDIO));
    }

    @Test
    void choosingRemovedTrackAsDefaultKeepsItAgain() {
        List<TrackPlan> plans = plans();
        plans.get(3).setKeep(false);

        TrackPlans.setDefault(plans, plans.get(3));

        assertTrue(plans.get(3).isKeep());
        assertTrue(plans.get(3).isMakeDefault());
    }

    @Test
    void setDefaultIgnoresVideo() {
        List<TrackPlan> plans = plans();

        TrackPlans.setDefault(plans, plans.get(0));

        assertEquals(List.of(1), defaultIndexes(plans, StreamType.AUDIO));
    }

    // ---------------------------------------------------------------- clearSubtitleDefault

    @Test
    void subtitlesCanHaveNoDefault() {
        List<TrackPlan> plans = plans();
        plans.get(4).setMakeDefault(true);

        TrackPlans.clearSubtitleDefault(plans);

        assertEquals(List.of(), defaultIndexes(plans, StreamType.SUBTITLE));
        assertEquals(List.of(1), defaultIndexes(plans, StreamType.AUDIO));
    }

    // ---------------------------------------------------------------- normalizeDefaults

    @Test
    void removingDefaultAudioMovesDefaultToFirstKeptAudio() {
        List<TrackPlan> plans = plans();
        plans.get(1).setKeep(false);   // TrueHD (default) removed

        TrackPlans.normalizeDefaults(plans);

        assertEquals(List.of(2), defaultIndexes(plans, StreamType.AUDIO));
    }

    @Test
    void removingAllAudioLeavesNoDefaultAudio() {
        List<TrackPlan> plans = plans();
        plans.stream().filter(p -> p.getStream().isAudio()).forEach(p -> p.setKeep(false));

        TrackPlans.normalizeDefaults(plans);

        assertEquals(List.of(), defaultIndexes(plans, StreamType.AUDIO));
    }

    @Test
    void keepingAudioAgainAfterRemovingAllMakesItDefault() {
        List<TrackPlan> plans = plans();
        plans.stream().filter(p -> p.getStream().isAudio()).forEach(p -> p.setKeep(false));
        TrackPlans.normalizeDefaults(plans);

        plans.get(3).setKeep(true);
        TrackPlans.normalizeDefaults(plans);

        assertEquals(List.of(3), defaultIndexes(plans, StreamType.AUDIO));
    }

    @Test
    void removingDefaultSubtitleLeavesNoDefaultSubtitle() {
        List<TrackPlan> plans = plans();
        plans.get(4).setMakeDefault(true);
        plans.get(4).setKeep(false);

        TrackPlans.normalizeDefaults(plans);

        assertEquals(List.of(), defaultIndexes(plans, StreamType.SUBTITLE), "subtitles are optional - no automatic default");
    }

    @Test
    void severalDefaultAudioFromFileKeepOnlyTheFirst() {
        // many files mark several audio tracks as default
        List<TrackPlan> plans = plans();
        plans.get(2).setMakeDefault(true);
        plans.get(3).setMakeDefault(true);

        TrackPlans.normalizeDefaults(plans);

        assertEquals(List.of(1), defaultIndexes(plans, StreamType.AUDIO));
    }

    @Test
    void fileWithoutDefaultAudioGetsFirstAudioAsDefault() {
        List<TrackPlan> plans = plans();
        plans.get(1).setMakeDefault(false);

        TrackPlans.normalizeDefaults(plans);

        assertEquals(List.of(1), defaultIndexes(plans, StreamType.AUDIO));
    }

    @Test
    void severalDefaultSubtitlesKeepOnlyTheFirst() {
        List<TrackPlan> plans = plans();
        plans.get(4).setMakeDefault(true);
        plans.get(5).setMakeDefault(true);

        TrackPlans.normalizeDefaults(plans);

        assertEquals(List.of(4), defaultIndexes(plans, StreamType.SUBTITLE));
    }

    @Test
    void normalizeDoesNotChangeVideo() {
        List<TrackPlan> plans = plans();

        TrackPlans.normalizeDefaults(plans);

        assertTrue(plans.get(0).isMakeDefault());
    }

    // ---------------------------------------------------------------- validate

    @Test
    void validPlanHasNoErrors() {
        assertEquals(List.of(), TrackPlans.validate(plans()));
    }

    @Test
    void noAudioIsAnError() {
        List<TrackPlan> plans = plans();
        plans.stream().filter(p -> p.getStream().isAudio()).forEach(p -> p.setKeep(false));

        List<String> errors = TrackPlans.validate(plans);

        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().contains("audio"), errors.toString());
    }

    @Test
    void noDefaultAudioIsAnError() {
        List<TrackPlan> plans = plans();
        plans.get(1).setMakeDefault(false);

        assertEquals(1, TrackPlans.validate(plans).size());
    }

    @Test
    void twoDefaultAudioIsAnError() {
        List<TrackPlan> plans = plans();
        plans.get(2).setMakeDefault(true);

        assertEquals(1, TrackPlans.validate(plans).size());
    }

    @Test
    void defaultOnRemovedAudioDoesNotCount() {
        List<TrackPlan> plans = plans();
        plans.get(1).setKeep(false);   // still marked default, but removed
        plans.get(2).setMakeDefault(true);

        assertEquals(List.of(), TrackPlans.validate(plans));
    }

    @Test
    void twoDefaultSubtitlesIsAnError() {
        List<TrackPlan> plans = plans();
        plans.get(4).setMakeDefault(true);
        plans.get(5).setMakeDefault(true);

        assertEquals(1, TrackPlans.validate(plans).size());
    }

    @Test
    void noDefaultSubtitleIsFine() {
        List<TrackPlan> plans = plans();

        assertFalse(plans.stream().anyMatch(p -> p.getStream().type() == StreamType.SUBTITLE && p.isMakeDefault()));
        assertEquals(List.of(), TrackPlans.validate(plans));
    }

    // ---------------------------------------------------------------- layoutDifference (#59)

    @Test
    void sameLayoutHasNoDifference() {
        assertEquals(Optional.empty(), TrackPlans.layoutDifference(plans(), plans()));
    }

    @Test
    void titlesMayDiffer() {
        // episodes often have different track titles but the same tracks
        List<TrackPlan> other = new ArrayList<>(plans());
        other.set(1, retitled(other.get(1), "Inny tytuł"));

        assertEquals(Optional.empty(), TrackPlans.layoutDifference(plans(), other));
    }

    @Test
    void differentTrackCountIsReported() {
        List<TrackPlan> shorter = plans().subList(0, 4);

        assertEquals(Optional.of("inna liczba ścieżek: 4 zamiast 6"), TrackPlans.layoutDifference(plans(), shorter));
    }

    @Test
    void differentCodecIsReportedWithTrackNumber() {
        List<TrackPlan> other = new ArrayList<>(plans());
        other.set(2, plan(2, StreamType.AUDIO, "eac3", false));

        assertEquals(Optional.of("ścieżka #2: kodek eac3 zamiast ac3"), TrackPlans.layoutDifference(plans(), other));
    }

    @Test
    void differentChannelsAreReported() {
        List<TrackPlan> other = new ArrayList<>(plans());
        other.set(3, TrackPlan.defaultsFor(new StreamInfo(3, StreamType.AUDIO, "dts", null, 2, null, "eng", null, false)));

        assertEquals(Optional.of("ścieżka #3: kanały 2 zamiast 6"), TrackPlans.layoutDifference(plans(), other));
    }

    @Test
    void differentLanguageIsReported() {
        List<TrackPlan> other = new ArrayList<>(plans());
        other.set(4, TrackPlan.defaultsFor(new StreamInfo(4, StreamType.SUBTITLE, "subrip", null, 0, null, "pol", null, false)));

        assertEquals(Optional.of("ścieżka #4: język pol zamiast eng"), TrackPlans.layoutDifference(plans(), other));
    }

    @Test
    void differentTypeIsReported() {
        List<TrackPlan> other = new ArrayList<>(plans());
        other.set(5, plan(5, StreamType.AUDIO, "subrip", false));

        assertEquals(Optional.of("ścieżka #5: inny typ ścieżki"), TrackPlans.layoutDifference(plans(), other));
    }

    // ---------------------------------------------------------------- copySettings (#59)

    @Test
    void copySettingsCopiesKeepCodecAndDefaults() {
        List<TrackPlan> from = plans();
        from.get(2).setKeep(false);                          // AC3 removed
        from.get(3).setTargetCodec(AudioCodec.EAC3);         // DTS -> E-AC3
        TrackPlans.setDefault(from, from.get(3));            // DTS default audio
        TrackPlans.setDefault(from, from.get(4));            // first subtitles default
        List<TrackPlan> to = withNormalizingListeners(plans());

        TrackPlans.copySettings(from, to);

        assertFalse(to.get(2).isKeep());
        assertEquals(AudioCodec.EAC3, to.get(3).getTargetCodec());
        assertEquals(List.of(3), defaultIndexes(to, StreamType.AUDIO));
        assertEquals(List.of(4), defaultIndexes(to, StreamType.SUBTITLE));
    }

    @Test
    void removingTrackWhileCopyingDoesNotAddSecondDefault() {
        // copying track by track (keep + default together) breaks here: after TrueHD's default is cleared,
        // removing AC3 re-normalizes defaults and makes TrueHD default again, next to the copied DTS
        List<TrackPlan> from = plans();
        TrackPlans.setDefault(from, from.get(3));   // DTS
        from.get(2).setKeep(false);                 // AC3 removed
        List<TrackPlan> to = withNormalizingListeners(plans());

        TrackPlans.copySettings(from, to);

        assertEquals(List.of(3), defaultIndexes(to, StreamType.AUDIO));
        assertEquals(List.of(), TrackPlans.validate(to));
    }

    @Test
    void copySettingsKeepsRemovedTracksAgainWhenSourceKeepsThem() {
        List<TrackPlan> to = withNormalizingListeners(plans());
        to.get(2).setKeep(false);

        TrackPlans.copySettings(plans(), to);

        assertTrue(to.get(2).isKeep());
    }

    @Test
    void copySettingsRefusesDifferentLayout() {
        assertThrows(IllegalArgumentException.class, () -> TrackPlans.copySettings(plans(), plans().subList(0, 3)));
    }

    // ---------------------------------------------------------------- helpers

    /** Like MainController.addMedia: changing keep re-normalizes defaults. */
    private static List<TrackPlan> withNormalizingListeners(List<TrackPlan> plans) {
        plans.forEach(plan -> plan.keepProperty().addListener(observable -> TrackPlans.normalizeDefaults(plans)));
        return plans;
    }

    private static TrackPlan retitled(TrackPlan plan, String title) {
        StreamInfo s = plan.getStream();
        return TrackPlan.defaultsFor(new StreamInfo(s.index(), s.type(), s.codec(), s.profile(), s.channels(),
                s.channelLayout(), s.language(), title, s.isDefault()));
    }

    /** 0 video, 1 TrueHD (default), 2 AC3, 3 DTS, 4 subtitles pol, 5 subtitles eng. */
    private static List<TrackPlan> plans() {
        return List.of(
                plan(0, StreamType.VIDEO, "hevc", true),
                plan(1, StreamType.AUDIO, "truehd", true),
                plan(2, StreamType.AUDIO, "ac3", false),
                plan(3, StreamType.AUDIO, "dts", false),
                plan(4, StreamType.SUBTITLE, "subrip", false),
                plan(5, StreamType.SUBTITLE, "subrip", false));
    }

    private static TrackPlan plan(int index, StreamType type, String codec, boolean isDefault) {
        int channels = type == StreamType.AUDIO ? 6 : 0;
        return TrackPlan.defaultsFor(new StreamInfo(index, type, codec, null, channels, null, "eng", null, isDefault));
    }

    private static List<Integer> defaultIndexes(List<TrackPlan> plans, StreamType type) {
        return plans.stream()
                .filter(p -> p.getStream().type() == type && p.isMakeDefault())
                .map(p -> p.getStream().index())
                .toList();
    }
}
