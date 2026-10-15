package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;

/** The windows loaded with the English bundle, as with -Duser.language=en (#28). */
class EnglishViewTest {

    private static Parent main;
    private static Parent about;

    @BeforeAll
    static void loadInEnglish() throws Exception {
        FxTestSupport.startJavaFx();
        Messages.use(Locale.ENGLISH);
        main = callOnFxThread(() -> FxTestSupport.loadMainView().getRoot());
        about = callOnFxThread(() -> new FXMLLoader(AboutController.class.getResource("about-view.fxml"), Messages.bundle()).load());
    }

    @AfterAll
    static void backToDefault() {
        Messages.use(Locale.getDefault());
    }

    @Test
    void buttonsAreInEnglish() {
        assertEquals("Add files…", button(main, "addFilesButton").getText());
        assertEquals("ffmpeg settings", button(main, "ffmpegSettingsButton").getText());
        assertEquals("About", button(main, "aboutButton").getText());
        assertEquals("Remove from queue", button(main, "removeFromQueueButton").getText());
        assertEquals("Apply to all", button(main, "applyToAllButton").getText());
        assertEquals("Change…", button(main, "changeOutputButton").getText());
        assertEquals("Quick conversion", button(main, "quickConvertButton").getText());
        assertEquals("Cancel", button(main, "cancelButton").getText());
    }

    @Test
    void tooltipsAreInEnglish() {
        assertTrue(button(main, "quickConvertButton").getTooltip().getText().startsWith("Video + one audio track"));
        assertTrue(button(main, "outputFolderButton").getTooltip().getText().startsWith("Saves all waiting files"));
    }

    @Test
    void themeMenuIsInEnglish() {
        MenuButton theme = (MenuButton) main.lookup("#themeButton");

        assertEquals("Theme", theme.getText());
        assertEquals(List.of("System", "Light", "Dark"), theme.getItems().stream().map(MenuItem::getText).toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void columnsAreInEnglish() {
        TableView<?> table = (TableView<?>) main.lookup("#trackTable");

        assertEquals(List.of("Keep", "#", "Type", "Codec", "Channels", "Language", "Title", "Action", "Default"),
                table.getColumns().stream().map(TableColumn::getText).toList());
    }

    @Test
    void aboutWindowIsInEnglish() {
        assertEquals("Author: Minner22", ((Label) about.lookup("#authorLabel")).getText());
        assertEquals("Log folder", button(about, "logsButton").getText());
        assertEquals("Copy information", button(about, "copyButton").getText());
        assertEquals("Close", button(about, "closeButton").getText());
    }

    private static Button button(Parent root, String id) {
        return (Button) root.lookup("#" + id);
    }
}
