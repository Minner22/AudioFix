package pl.audiofix.ui;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;
import pl.audiofix.model.TrackPlans;

import java.util.List;
import java.util.function.Function;

public final class TrackTableConfigurer {

    static final PseudoClass NEEDS_CONVERSION = PseudoClass.getPseudoClass("needs-conversion");
    static final PseudoClass REMOVED = PseudoClass.getPseudoClass("removed");

    private TrackTableConfigurer() {}

    public static void configureTable(TableView<TrackPlan> table) {

        table.setEditable(true);
        table.setRowFactory(track -> new TrackRow());
    }

    public static void configureKeep(TableColumn<TrackPlan, Boolean> column) {

        column.setCellValueFactory(cell -> cell.getValue().keepProperty());
        column.setCellFactory(c -> new CheckBoxTableCell<>() {

            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                TrackPlan plan = getTableRow() == null
                        ? null
                        : getTableRow().getItem();
                setEditable(plan != null && isRemovable(plan.getStream().type()));
            }
        });

    }

    public static void configureAction(TableColumn<TrackPlan, AudioCodec> column) {

        column.setCellValueFactory(cell -> cell.getValue().targetCodecProperty());
        column.setCellFactory(c -> new ComboBoxTableCell<>(AudioCodec.values()) {
            @Override
            public void updateItem(AudioCodec item, boolean empty) {
                super.updateItem(item, empty);
                TrackPlan plan = getTableRow() == null
                        ? null
                        : getTableRow().getItem();
                setEditable(plan != null && plan.getStream().isAudio());
            }
        });
    }

    public static void configureDefault(TableColumn<TrackPlan, Boolean> column) {

        column.setCellValueFactory(cell -> cell.getValue().makeDefaultProperty());
        column.setCellFactory(c -> new DefaultCell());
    }

    public static <T> void configureReadOnly(TableColumn<TrackPlan, T> column, Function<StreamInfo, T> getter) {

        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue().getStream())));
        column.setEditable(false);
    }

    static String typeLabel(StreamType type) {

        return switch (type) {
            case VIDEO -> "Wideo";
            case AUDIO -> "Audio";
            case SUBTITLE -> "Napisy";
            case ATTACHMENT -> "Załącznik";
            case OTHER -> "Inne";
        };
    }

    static boolean isRemovable(StreamType type) {

        return type == StreamType.AUDIO || type == StreamType.SUBTITLE;
    }

    static boolean isShown(StreamType type) {

        return type == StreamType.VIDEO
                || type == StreamType.AUDIO
                || type == StreamType.SUBTITLE;
    }

    private static final class TrackRow extends TableRow<TrackPlan> {

        private final InvalidationListener keepListener = observable -> updateRemoved();
        private TrackPlan observed;

        @Override
        protected void updateItem(TrackPlan plan, boolean empty) {
            super.updateItem(plan, empty);

            if (observed != null) {
                observed.keepProperty().removeListener(keepListener);
            }

            observed = empty ? null : plan;

            if (observed != null) {
                observed.keepProperty().addListener(keepListener);
            }

            pseudoClassStateChanged(NEEDS_CONVERSION, observed != null && observed.getStream().needsConversion());
            updateRemoved();
        }

        private void updateRemoved() {

            pseudoClassStateChanged(REMOVED, observed != null && !observed.isKeep());
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

            TrackPlan plan = getTableRow() == null
                    ? null
                    : getTableRow().getItem();

            if (empty || plan == null || !isRemovable(plan.getStream().type())) {
                setGraphic(null);
                return;
            }

            radio.setSelected(Boolean.TRUE.equals(isDefault));
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
