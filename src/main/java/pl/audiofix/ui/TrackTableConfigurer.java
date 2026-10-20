package pl.audiofix.ui;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;
import pl.audiofix.model.TrackPlans;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class TrackTableConfigurer {

    static final PseudoClass NEEDS_CONVERSION = PseudoClass.getPseudoClass("needs-conversion");
    static final PseudoClass REMOVED = PseudoClass.getPseudoClass("removed");
    static final PseudoClass MISSING = PseudoClass.getPseudoClass("missing");

    private static final List<String> TYPE_CLASSES = List.of("type-video", "type-audio", "type-subtitle");
    private static final List<String> LANGUAGE_CLASSES = List.of("language-local", "language-unknown", "language-other");

    private TrackTableConfigurer() {}

    public static void configureTable(TableView<TrackPlan> table) {

        table.setEditable(true);
        table.setRowFactory(track -> new TrackRow());
    }

    public static void configureKeep(TableColumn<TrackPlan, Boolean> column) {

        column.setCellValueFactory(cell -> cell.getValue().keepProperty());
        column.setCellFactory(c -> {
            CheckBoxTableCell<TrackPlan, Boolean> cell = new CheckBoxTableCell<>() {

                @Override
                public void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    TrackPlan plan = getTableRow() == null
                            ? null
                            : getTableRow().getItem();
                    setEditable(plan != null && isRemovable(plan.getStream().type()));
                }
            };
            cell.getStyleClass().add("keep-cell");   // stays bright in a removed row, it is the way back
            return cell;
        });
    }

    public static void configureIndex(TableColumn<TrackPlan, Integer> column) {

        configureReadOnly(column, StreamInfo::index);
        column.setCellFactory(c -> new TextCell<>("track-index"));
    }

    public static void configureType(TableColumn<TrackPlan, StreamType> column) {

        configureReadOnly(column, StreamInfo::type);
        column.setCellFactory(c -> new TypeCell());
    }

    public static void configureCodec(TableColumn<TrackPlan, StreamInfo> column) {

        configureReadOnly(column, stream -> stream);
        column.setCellFactory(c -> new CodecCell());
    }

    public static void configureChannels(TableColumn<TrackPlan, String> column) {

        configureReadOnly(column, TrackTexts::channels);
        column.setCellFactory(c -> new TextCell<>("track-channels"));
    }

    public static void configureLanguage(TableColumn<TrackPlan, String> column) {

        configureReadOnly(column, StreamInfo::language);
        column.setCellFactory(c -> new LanguageCell());
    }

    public static void configureTitle(TableColumn<TrackPlan, String> column) {

        configureReadOnly(column, StreamInfo::title);
        column.setCellFactory(c -> new TitleCell());
    }

    public static void configureAction(TableColumn<TrackPlan, AudioCodec> column) {

        column.setCellValueFactory(cell -> cell.getValue().targetCodecProperty());
        column.setCellFactory(c -> new ActionCell());
    }

    public static void configureDefault(TableColumn<TrackPlan, Boolean> column) {

        column.setCellValueFactory(cell -> cell.getValue().makeDefaultProperty());
        column.setCellFactory(c -> new DefaultCell());
    }

    private static <T> void configureReadOnly(TableColumn<TrackPlan, T> column, Function<StreamInfo, T> getter) {

        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue().getStream())));
        column.setEditable(false);
    }

    static String typeLabel(StreamType type) {

        return Messages.get("track.type." + type.name());
    }

    /** "type-video", "type-audio", "type-subtitle": the color of the "Typ" text. */
    static String typeStyleClass(StreamType type) {

        return "type-" + type.name().toLowerCase(Locale.ROOT);
    }

    static boolean isRemovable(StreamType type) {

        return type == StreamType.AUDIO || type == StreamType.SUBTITLE;
    }

    static boolean isShown(StreamType type) {

        return type == StreamType.VIDEO
                || type == StreamType.AUDIO
                || type == StreamType.SUBTITLE;
    }

    private static TrackPlan planOf(TableCell<TrackPlan, ?> cell) {

        return cell.getTableRow() == null
                ? null
                : cell.getTableRow().getItem();
    }

    /** Row look: :needs-conversion follows the chosen action, :removed the "Zostaw" checkbox. */
    private static final class TrackRow extends TableRow<TrackPlan> {

        private final InvalidationListener planListener = observable -> updateState();
        private TrackPlan observed;

        @Override
        protected void updateItem(TrackPlan plan, boolean empty) {
            super.updateItem(plan, empty);

            if (observed != null) {
                observed.keepProperty().removeListener(planListener);
                observed.targetCodecProperty().removeListener(planListener);
            }

            observed = empty ? null : plan;

            if (observed != null) {
                observed.keepProperty().addListener(planListener);
                observed.targetCodecProperty().addListener(planListener);
            }

            updateState();
        }

        private void updateState() {

            pseudoClassStateChanged(NEEDS_CONVERSION, observed != null && observed.isConverted());
            pseudoClassStateChanged(REMOVED, observed != null && !observed.isKeep());
        }
    }

    /** Text with its own style class; a missing value is a muted "-". */
    private static class TextCell<T> extends TableCell<TrackPlan, T> {

        TextCell(String styleClass) {

            getStyleClass().add(styleClass);
        }

        @Override
        protected void updateItem(T item, boolean empty) {

            super.updateItem(item, empty);
            pseudoClassStateChanged(MISSING, !empty && item == null);
            setText(empty ? null : item == null ? "-" : item.toString());
        }
    }

    /** Full title in a tooltip, because the column cuts long ones with "…". */
    private static final class TitleCell extends TextCell<String> {

        TitleCell() {

            super("track-title");
        }

        @Override
        protected void updateItem(String title, boolean empty) {

            super.updateItem(title, empty);
            setTooltip(empty || title == null ? null : new Tooltip(title));
        }
    }

    private static final class TypeCell extends TableCell<TrackPlan, StreamType> {

        TypeCell() {

            getStyleClass().add("track-type");
        }

        @Override
        protected void updateItem(StreamType type, boolean empty) {

            super.updateItem(type, empty);
            getStyleClass().removeAll(TYPE_CLASSES);   // cells are reused for other rows
            if (empty || type == null) {
                setText(null);
                return;
            }

            setText(typeLabel(type));
            getStyleClass().add(typeStyleClass(type));
        }
    }

    /** Codec name and, for audio, a smaller description under it. */
    private static final class CodecCell extends TableCell<TrackPlan, StreamInfo> {

        private final Label name = new Label();
        private final Label description = new Label();
        private final VBox lines = new VBox(name, description);

        CodecCell() {

            name.getStyleClass().add("codec-name");
            description.getStyleClass().add("codec-description");
            description.managedProperty().bind(description.visibleProperty());   // one line takes one line's height
            lines.setAlignment(Pos.CENTER_LEFT);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(StreamInfo stream, boolean empty) {

            super.updateItem(stream, empty);
            if (empty || stream == null) {
                setGraphic(null);
                return;
            }

            name.setText(TrackTexts.codecName(stream));
            description.setText(TrackTexts.codecDescription(stream));
            description.setVisible(!description.getText().isEmpty());
            setGraphic(lines);
        }
    }

    private static final class LanguageCell extends TableCell<TrackPlan, String> {

        private final Label badge = new Label();

        LanguageCell() {

            badge.getStyleClass().add("language-badge");
            badge.setMinWidth(Region.USE_PREF_SIZE);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(String language, boolean empty) {

            super.updateItem(language, empty);
            badge.getStyleClass().removeAll(LANGUAGE_CLASSES);
            if (empty || language == null) {
                setGraphic(null);
                return;
            }

            badge.setText(language);
            badge.getStyleClass().add(TrackTexts.languageStyleClass(language));
            setGraphic(badge);
        }
    }

    /**
     * Always visible list of actions for audio, orange when the track is converted.
     * Video and subtitles show a locked "Kopiuj"; their plan has no target codec.
     */
    private static final class ActionCell extends TableCell<TrackPlan, AudioCodec> {

        private final ComboBox<AudioCodec> codecs = new ComboBox<>();
        private final ComboBox<String> fixed = new ComboBox<>();
        private TrackPlan plan;

        ActionCell() {

            codecs.getItems().setAll(AudioCodec.values());
            codecs.setMaxWidth(Double.MAX_VALUE);
            codecs.setOnAction(event -> {
                if (plan != null && codecs.getValue() != null) {
                    plan.setTargetCodec(codecs.getValue());
                }
            });
            fixed.setMaxWidth(Double.MAX_VALUE);
            fixed.setDisable(true);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(AudioCodec codec, boolean empty) {

            super.updateItem(codec, empty);
            plan = null;   // showing the value below must not write it into the previous row's plan

            TrackPlan row = empty ? null : planOf(this);
            if (row == null) {
                setGraphic(null);
                return;
            }

            if (!row.getStream().isAudio()) {
                fixed.getItems().setAll(Messages.get(row.getStream().type() == StreamType.VIDEO ? "track.action.videoCopy" : "track.action.subtitleCopy"));
                fixed.getSelectionModel().selectFirst();
                setGraphic(fixed);
                return;
            }

            codecs.setValue(codec);
            codecs.getStyleClass().remove("highlighted");
            if (codec != null && codec != AudioCodec.COPY) {
                codecs.getStyleClass().add("highlighted");
            }
            codecs.disableProperty().bind(row.keepProperty().not());
            plan = row;
            setGraphic(codecs);
        }
    }

    private static final class DefaultCell extends TableCell<TrackPlan, Boolean> {

        private final RadioButton radio = new RadioButton();

        DefaultCell() {

            radio.setOnAction(event -> choose());
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(Boolean isDefault, boolean empty) {

            super.updateItem(isDefault, empty);

            TrackPlan plan = planOf(this);

            if (empty || plan == null) {
                setGraphic(null);
                return;
            }

            if (plan.getStream().type() == StreamType.VIDEO) {
                // the video track is always the default one, nothing to choose
                radio.disableProperty().unbind();
                radio.setDisable(true);
                radio.setSelected(true);
            }
            else {
                radio.disableProperty().bind(plan.keepProperty().not());
                radio.setSelected(Boolean.TRUE.equals(isDefault));
            }
            setGraphic(radio);
        }

        private void choose() {

            TrackPlan plan = getTableRow().getItem();
            List<TrackPlan> plans = getTableView().getItems();

            if (plan.getStream().type() == StreamType.SUBTITLE && plan.isMakeDefault()) {
                TrackPlans.clearSubtitleDefault(plans);
            }
            else {
                TrackPlans.setDefault(plans, plan);
            }

            radio.setSelected(plan.isMakeDefault());
        }
    }
}
