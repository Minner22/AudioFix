package pl.audiofix.ffmpeg;

import pl.audiofix.model.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandBuilder {

    private static final List<String> COPY_ATTACHMENTS_AND_CHAPTERS = List.of("-map", "0:t?", "-map_chapters", "0");
    private static final List<String> COPY_ALL_STREAMS = List.of("-c", "copy");
    private static final List<String> PROGRESS_TO_STDOUT = List.of("-progress", "pipe:1", "-nostats");

    public List<String> build(Path ffmpeg, Path input, Path output, List<TrackPlan> plans) {

        List<TrackPlan> kept = plans.stream()
                .filter(TrackPlan::isKeep)
                .filter(plan -> isMappable(plan.getStream().type()))
                .toList();

        List<String> args = new ArrayList<>();
        args.addAll(inputArgs(ffmpeg, input));
        args.addAll(mapArgs(kept));
        args.addAll(COPY_ATTACHMENTS_AND_CHAPTERS);
        args.addAll(COPY_ALL_STREAMS);
        args.addAll(audioArgs(ofType(kept, StreamType.AUDIO)));
        args.addAll(subtitleArgs(ofType(kept, StreamType.SUBTITLE)));
        args.addAll(PROGRESS_TO_STDOUT);
        args.add(output.toString());

        return args;
    }

    /**
     * The command used before AudioFix existed: ffmpeg picks one video, one audio (most channels)
     * and one text subtitle stream itself; the audio is always converted to PCM 24-bit.
     */
    public List<String> buildQuick(Path ffmpeg, Path input, Path output) {

        List<String> args = new ArrayList<>(inputArgs(ffmpeg, input));
        args.addAll(List.of("-c:v", "copy", "-c:a", AudioCodec.PCM_S24LE.getFfmpegName()));
        args.addAll(PROGRESS_TO_STDOUT);
        args.add(output.toString());

        return args;
    }

    public static String toCommandLine(List<String> args) {

        return args.stream()
                .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                .collect(Collectors.joining(" "));
    }

    private static List<String> inputArgs(Path ffmpeg, Path input) {

        return List.of(ffmpeg.toString(), "-hide_banner", "-y", "-i", input.toString());
    }

    private static List<String> mapArgs(List<TrackPlan> kept) {

        List<String> args = new ArrayList<>();
        for (TrackPlan plan : kept) {
            args.add("-map");
            args.add("0:" + plan.getStream().index());
        }

        return args;
    }

    private static List<String> audioArgs(List<TrackPlan> audio) {

        List<String> args = new ArrayList<>();
        for (int k = 0; k < audio.size(); k++) {
            TrackPlan plan = audio.get(k);
            args.addAll(codecArgs(k, plan));
            args.addAll(dispositionArgs("a", k, plan));
        }

        return args;
    }

    private static List<String> codecArgs(int k, TrackPlan plan) {

        AudioCodec codec = plan.getTargetCodec();
        if (codec == null || codec == AudioCodec.COPY) {
            return List.of();
        }

        StreamInfo stream = plan.getStream();
        List<String> args = new ArrayList<>(List.of("-c:a:" + k, codec.getFfmpegName()));
        codec.getBitrate().ifPresent(bitrate -> args.addAll(List.of("-b:a:" + k, bitrate)));

        int channels = codec.outputChannels(stream.channels());
        if (channels < stream.channels()) {
            args.addAll(List.of("-ac:a:" + k, String.valueOf(channels)));
        }

        TrackTitles.forConversion(stream, codec)
                .ifPresent(title -> args.addAll(List.of("-metadata:s:a:" + k, "title=" + title)));

        return args;
    }

    private static List<String> subtitleArgs(List<TrackPlan> subtitles) {

        List<String> args = new ArrayList<>();
        for (int s = 0; s < subtitles.size(); s++) {
            args.addAll(dispositionArgs("s", s, subtitles.get(s)));
        }

        return args;
    }

    private static List<String> dispositionArgs(String streamType, int index, TrackPlan plan) {

        return List.of("-disposition:" + streamType + ":" + index, plan.isMakeDefault() ? "+default" : "-default");
    }

    private static List<TrackPlan> ofType(List<TrackPlan> plans, StreamType type) {

        return plans.stream()
                .filter(plan -> plan.getStream().type() == type)
                .toList();
    }

    private static boolean isMappable(StreamType type) {

        return switch (type) {
            case VIDEO, AUDIO, SUBTITLE -> true;
            case ATTACHMENT, OTHER -> false;
        };
    }

}
