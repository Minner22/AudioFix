package pl.audiofix.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class TrackPlan {

    private final StreamInfo stream;
    private final BooleanProperty keep = new SimpleBooleanProperty(true);
    private final ObjectProperty<AudioCodec> targetCodec = new SimpleObjectProperty<>();
    private final BooleanProperty makeDefault = new SimpleBooleanProperty();

    private TrackPlan(StreamInfo stream) {

        this.stream = stream;
    }

    public BooleanProperty keepProperty() {

        return keep;
    }

    public boolean isKeep() {

        return keep.get();
    }

    public void setKeep(boolean value) {

        keep.set(value);
    }

    public ObjectProperty<AudioCodec> targetCodecProperty() {

        return targetCodec;
    }

    public AudioCodec getTargetCodec() {

        return targetCodec.get();
    }

    public void setTargetCodec(AudioCodec value) {

        targetCodec.set(value);
    }

    public BooleanProperty makeDefaultProperty() {

        return makeDefault;
    }

    public boolean isMakeDefault() {

        return makeDefault.get();
    }

    public void setMakeDefault(boolean value) {

        makeDefault.set(value);
    }

    public StreamInfo getStream() {

        return stream;
    }

    public static TrackPlan defaultsFor(StreamInfo stream) {

        TrackPlan plan = new TrackPlan(stream);
        plan.setKeep(true);
        plan.setMakeDefault(stream.isDefault());
        if (stream.isAudio()) {
            plan.setTargetCodec(stream.isDts() ? AudioCodec.PCM_S24LE : AudioCodec.COPY);
        }
        else {
            plan.setTargetCodec(null);
        }

        return plan;
    }
}
