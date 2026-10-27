package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.Tooltip;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Map;


final class FxmlTexts {

    private FxmlTexts() {
    }

    static void translate(URL fxml, Map<String, Object> shown) {

        FXMLLoader fresh = new FXMLLoader(fxml, Messages.bundle());
        try {
            fresh.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        shown.forEach((id, element) -> copy(element, fresh.getNamespace().get(id)));
    }

    private static void copy(Object shown, Object fresh) {

        switch (shown) {
            case Labeled labeled when fresh instanceof Labeled source -> {
                labeled.setText(source.getText());
                copyTooltip(labeled.getTooltip(), source.getTooltip());
            }
            case MenuItem item when fresh instanceof MenuItem source -> item.setText(source.getText());
            case TableColumnBase<?, ?> column when fresh instanceof TableColumnBase<?, ?> source -> column.setText(source.getText());
            default -> { }
        }
    }

    private static void copyTooltip(Tooltip shown, Tooltip fresh) {

        if (shown != null && fresh != null) {
            shown.setText(fresh.getText());
        }
    }
}
