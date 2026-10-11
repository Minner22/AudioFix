package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.audiofix.AudioFixApp;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.ui.theme.Theme;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/** Panel headers and the window look with the light theme (#44): texts, icons, the smallest window. */
class MainLayoutTest {

    /** Scene of a 960x600 window: the Windows frame and title bar take about 16x39 px. */
    private static final double MIN_SCENE_WIDTH = 944;
    private static final double MIN_SCENE_HEIGHT = 561;

    private static final String LONG_NAME = "Drive.2011.UHD.BluRay.2160p.DTS-HD.MA.7.1.HEVC.REMUX-FraMeSToR.mkv";

    private Parent root;
    private MainController controller;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    // ---------------------------------------------------------------- queue header

    @Test
    void queueHeaderCountsFilesAndRemainingOnes() throws Exception {
        load(1100, 700);
        assertEquals("Kolejka plików (0)", text("queueTitleLabel"));
        assertEquals("Pozostało: 0", text("queueRemainingLabel"));

        ConversionJob first = add("Odcinek 1.mkv");
        add("Odcinek 2.mkv");
        assertEquals("Kolejka plików (2)", text("queueTitleLabel"));
        assertEquals("Pozostało: 2", text("queueRemainingLabel"));

        onFxThread(first::markRunning);
        assertEquals("Pozostało: 2", text("queueRemainingLabel"), "the file being converted is still remaining");

        onFxThread(first::markDone);
        assertEquals("Kolejka plików (2)", text("queueTitleLabel"));
        assertEquals("Pozostało: 1", text("queueRemainingLabel"), "follows status changes, not only adding");
    }

    // ---------------------------------------------------------------- tracks header

    @Test
    void tracksHeaderIsPlainWithoutFile() throws Exception {
        load(1100, 700);

        assertEquals("Ścieżki", text("tracksTitleLabel"));
        assertEquals("", text("tracksFileLabel"));
        assertEquals("", text("tracksInfoLabel"));
        assertEquals("", text("trackCountLabel"));
    }

    @Test
    void tracksHeaderDescribesShownFile() throws Exception {
        load(1100, 700);
        ConversionJob job = add("Drive.mkv");

        assertEquals("Ścieżki:", text("tracksTitleLabel"));
        assertEquals("Drive.mkv", text("tracksFileLabel"));
        assertEquals("(32,4 GB, Matroska)", text("tracksInfoLabel"));
        assertEquals("5 ścieżek", text("trackCountLabel"), "rows of the table: video, 3 audio, subtitles");
        assertEquals(job.getInput().toString(), label("tracksFileLabel").getTooltip().getText());
    }

    @Test
    void tracksHeaderFollowsSelectionAndEmptyQueue() throws Exception {
        load(1100, 700);
        ConversionJob first = add("Odcinek 1.mkv");
        add("Odcinek 2.mkv");

        onFxThread(() -> queueList().getSelectionModel().select(first));
        assertEquals("Odcinek 1.mkv", text("tracksFileLabel"));

        onFxThread(() -> queueList().getSelectionModel().clearSelection());
        assertEquals("Ścieżki", text("tracksTitleLabel"));
        assertEquals("", text("tracksFileLabel"));
        assertNull(label("tracksFileLabel").getTooltip());
    }

    // ---------------------------------------------------------------- look

    @Test
    void iconsTakeColorOfTheirButton() throws Exception {
        load(1100, 700);

        assertEquals(Color.web("#0284c7"), iconFill("addFilesButton"), "add: accent");
        assertEquals(Color.web("#64748b"), iconFill("ffmpegSettingsButton"), "plain button: muted");
        assertEquals(Color.WHITE, iconFill("startButton"), "on the accent button");
        assertEquals(Color.web("#ea580c"), iconFill("quickConvertButton"), "warning: orange");
        assertEquals(Color.web("#b91c1c"), iconFill("removeFromQueueButton"), "danger: red");
    }

    @Test
    void iconsAreSmall() throws Exception {
        load(1100, 700);

        Bounds bounds = callOnFxThread(() -> lookup("addFilesButton", Button.class).getGraphic().getLayoutBounds());
        assertTrue(bounds.getWidth() <= Icon.SIZE + 0.5 && bounds.getHeight() <= Icon.SIZE + 0.5, bounds.toString());
    }

    @Test
    void outputPathIsMonospacedAndJoinedWithChangeButton() throws Exception {
        load(1100, 700);
        TextField field = lookup("outputField", TextField.class);
        Button change = lookup("changeOutputButton", Button.class);

        assertEquals("Consolas", field.getFont().getFamily());
        BackgroundFill fieldBorder = field.getBackground().getFills().getFirst();
        assertEquals(0, fieldBorder.getRadii().getTopRightHorizontalRadius(), "field: square right edge");
        BackgroundFill buttonFace = change.getBackground().getFills().get(1);
        assertEquals(0, buttonFace.getInsets().getLeft(), "button: no own left border, the field's one is shared");
        assertEquals(0, change.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());

        double gap = callOnFxThread(() -> change.localToScene(0, 0).getX()
                - field.localToScene(field.getWidth(), 0).getX());
        assertEquals(0, gap, 0.5, "button sits right at the field");
    }

    @Test
    void panelHeadersHaveSameHeight() throws Exception {
        load(1100, 700);

        List<Double> heights = callOnFxThread(() -> root.lookupAll(".panel-header").stream()
                .map(node -> node.getLayoutBounds().getHeight())
                .toList());
        assertEquals(2, heights.size());
        assertEquals(heights.get(0), heights.get(1), "with and without a button in the header");
    }

    // ---------------------------------------------------------------- smallest window

    @Test
    void smallestWindowCutsNoButtonOrHeader() throws Exception {
        load(MIN_SCENE_WIDTH, MIN_SCENE_HEIGHT);
        add(LONG_NAME);

        // the file name may be shortened, list items have their own test, table columns are #45
        List<String> cut = callOnFxThread(() -> java.util.stream.Stream.of(".button", ".label")
                .flatMap(selector -> root.lookupAll(selector).stream())
                .filter(Node::isVisible)
                .filter(node -> node != label("tracksFileLabel"))
                .filter(node -> !isInside(node, "queueList") && !isInside(node, "trackTable"))
                .map(Labeled.class::cast)
                .filter(labeled -> labeled.getText() != null && !labeled.getText().isEmpty())
                .filter(labeled -> !labeled.getText().equals(shownText(labeled)))
                .map(labeled -> labeled.getText() + " -> " + shownText(labeled))
                .toList());
        assertEquals(List.of(), cut);
    }

    @Test
    void smallestWindowShortensFileNameInTheMiddle() throws Exception {
        load(MIN_SCENE_WIDTH, MIN_SCENE_HEIGHT);
        add(LONG_NAME);

        String shown = callOnFxThread(() -> shownText(label("tracksFileLabel")));
        assertTrue(shown.contains("…"), shown);
        assertTrue(shown.startsWith("Drive") && shown.endsWith(".mkv"), shown);
    }

    @Test
    void smallestWindowKeepsButtonsInside() throws Exception {
        load(MIN_SCENE_WIDTH, MIN_SCENE_HEIGHT);
        add(LONG_NAME);

        List<String> outside = callOnFxThread(() -> root.lookupAll(".button").stream()
                .filter(node -> {
                    Bounds bounds = node.localToScene(node.getLayoutBounds());
                    return bounds.getMaxX() > MIN_SCENE_WIDTH + 0.5 || bounds.getMaxY() > MIN_SCENE_HEIGHT + 0.5;
                })
                .map(Node::getId)
                .toList());
        assertEquals(List.of(), outside);
    }

    // ---------------------------------------------------------------- helpers

    private void load(double width, double height) throws Exception {
        onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(AudioFixApp.class.getResource("ui/main-view.fxml"));
            root = loader.load();
            controller = loader.getController();
            Theme.apply(new Scene(root, width, height));
            FxTestSupport.layout(root);
        });
    }

    private ConversionJob add(String name) throws Exception {
        return callOnFxThread(() -> {
            ConversionJob job = controller.addMedia(media(Path.of("D:\\Filmy").resolve(name)));
            FxTestSupport.layout(root);
            return job;
        });
    }

    /** Video, three audio tracks and subtitles, 32,4 GB Matroska. */
    private static MediaInfo media(Path path) {
        return new MediaInfo(path, 6034, List.of(
                new StreamInfo(0, StreamType.VIDEO, "hevc", "Main 10", 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "truehd", null, 8, "7.1", "eng", null, true),
                new StreamInfo(2, StreamType.AUDIO, "dts", "DTS-HD MA", 6, "5.1(side)", "eng", null, false),
                new StreamInfo(3, StreamType.AUDIO, "ac3", null, 6, "5.1(side)", "pol", "Lektor", false),
                new StreamInfo(4, StreamType.SUBTITLE, "hdmv_pgs_subtitle", null, 0, null, "pol", null, false)),
                34_789_235_712L, "Matroska / WebM");
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.control.ListView<ConversionJob> queueList() {
        return lookup("queueList", javafx.scene.control.ListView.class);
    }

    private String text(String id) throws Exception {
        return callOnFxThread(() -> label(id).getText());
    }

    private Label label(String id) {
        return lookup(id, Label.class);
    }

    private Color iconFill(String buttonId) throws Exception {
        return callOnFxThread(() -> {
            Node graphic = lookup(buttonId, Button.class).getGraphic();
            assertTrue(graphic instanceof Icon, "no icon on " + buttonId);
            return (Color) ((SVGPath) graphic.lookup(".icon")).getFill();
        });
    }

    private <T extends Node> T lookup(String id, Class<T> type) {
        Node node = root.lookup("#" + id);
        assertNotNull(node, "no node with fx:id=" + id);
        assertTrue(type.isInstance(node), id + " is " + node.getClass().getSimpleName());
        return type.cast(node);
    }

    private boolean isInside(Node node, String parentId) {
        Node parent = lookup(parentId, Node.class);
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == parent) {
                return true;
            }
        }
        return false;
    }

    /** Text the control really shows, after shortening with the ellipsis. */
    private static String shownText(Labeled labeled) {
        Node text = labeled.lookup(".text");
        assertFalse(text == null, "no text node in " + labeled);
        return ((Text) text).getText();
    }
}
