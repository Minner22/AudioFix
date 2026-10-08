package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/**
 * The "O programie" window: loads from about-view.fxml with AboutController and shows the given AboutInfo.
 */
class AboutViewTest {

    private static final AboutInfo INFO = new AboutInfo("0.2.0", Path.of("C:\\ffmpeg\\bin\\ffmpeg.exe"),
            "ffmpeg version 7.0.1-essentials_build", "25.0.2", "25.0.4", "Windows 11 10.0");

    private Parent root;
    private AboutController controller;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void loadView() throws Exception {
        onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(AboutController.class.getResource("about-view.fxml"));
            root = loader.load();   // a typo in fx:id, onAction or fx:controller fails here
            controller = loader.getController();
            new Scene(root);
            FxTestSupport.layout(root);
        });
    }

    @Test
    void showsAppVersion() throws Exception {
        onFxThread(() -> controller.show(INFO));

        assertEquals("AudioFix 0.2.0", lookup("appVersionLabel", Label.class).getText());
    }

    @Test
    void showsFfmpegVersionAndPath() throws Exception {
        onFxThread(() -> controller.show(INFO));

        String text = lookup("ffmpegLabel", Label.class).getText();
        assertTrue(text.contains("ffmpeg version 7.0.1-essentials_build"), text);
        assertTrue(text.contains("C:\\ffmpeg\\bin\\ffmpeg.exe"), text);
    }

    @Test
    void ffmpegVersionNotYetReadIsShownAsPending() throws Exception {
        // the version is read in the background - the window opens before it is known
        AboutInfo withoutVersion = new AboutInfo("0.2.0", INFO.ffmpegPath(), null, "25", "25", "Windows");

        onFxThread(() -> controller.show(withoutVersion));

        String text = lookup("ffmpegLabel", Label.class).getText();
        assertTrue(text.contains("sprawdzanie"), text);
        assertFalse(text.contains("null"), text);
    }

    @Test
    void missingFfmpegPathIsShownAsUnknown() throws Exception {
        AboutInfo withoutPath = new AboutInfo("0.2.0", null, "ffmpeg version 7.0.1", "25", "25", "Windows");

        onFxThread(() -> controller.show(withoutPath));

        String text = lookup("ffmpegLabel", Label.class).getText();
        assertTrue(text.contains("nieznana"), text);
        assertFalse(text.contains("null"), text);
    }

    @Test
    void showsEnvironment() throws Exception {
        onFxThread(() -> controller.show(INFO));

        String text = lookup("environmentLabel", Label.class).getText();
        assertTrue(text.contains("25.0.2"), text);
        assertTrue(text.contains("25.0.4"), text);
        assertTrue(text.contains("Windows 11"), text);
    }

    @Test
    void showsAuthor() {
        assertEquals("Autor: Minner22", lookup("authorLabel", Label.class).getText());
    }

    @Test
    void repositoryLinkPointsToGitHub() {
        assertEquals("https://github.com/Minner22/AudioFix", lookup("repositoryLink", Hyperlink.class).getText());
        assertNotNull(lookup("repositoryLink", Hyperlink.class).getOnAction());
    }

    @Test
    void licensesListAllComponents() {
        String licenses = lookup("licensesArea", TextArea.class).getText();

        for (String name : List.of("AudioFix", "MIT", "OpenJFX", "Jackson", "SLF4J", "Logback", "FFmpeg")) {
            assertTrue(licenses.contains(name), "licenses do not mention " + name);
        }
        assertFalse(lookup("licensesArea", TextArea.class).isEditable());
    }

    @Test
    void closeButtonReactsToEnterAndEscape() {
        Button close = lookup("closeButton", Button.class);

        assertTrue(close.isCancelButton(), "Esc should close the window");
        assertTrue(close.isDefaultButton(), "Enter should close the window");
    }

    @Test
    void copyButtonPutsVersionsOnClipboard() throws Exception {
        // uses the real system clipboard - previous content is restored afterwards
        AtomicReference<String> copied = new AtomicReference<>();
        onFxThread(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            String previous = clipboard.getString();
            try {
                controller.show(INFO);
                lookup("copyButton", Button.class).fire();
                copied.set(clipboard.getString());
            } finally {
                ClipboardContent restore = new ClipboardContent();
                restore.putString(previous == null ? "" : previous);
                clipboard.setContent(restore);
            }
        });

        assertEquals(INFO.toClipboardText(), copied.get());
    }

    private <T extends Node> T lookup(String id, Class<T> type) {
        Node node = root.lookup("#" + id);
        assertNotNull(node, "no node with fx:id=" + id);
        return assertInstanceOf(type, node);
    }
}
