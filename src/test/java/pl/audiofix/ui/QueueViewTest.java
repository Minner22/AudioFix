package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/**
 * The queue in the main window without ffmpeg: files are added with MainController.addMedia
 * (what [Dodaj pliki…] does after ffprobe), and the table, output field and log follow the selected file.
 */
class QueueViewTest {

    @TempDir
    Path dir;

    private MainController controller;
    private ListView<ConversionJob> queueList;
    private TableView<TrackPlan> table;
    private TextField outputField;
    private TextArea logArea;
    private Button changeOutputButton;
    private Button removeButton;
    private Button startButton;
    private Button applyToAllButton;
    private Button outputFolderButton;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void loadView() throws Exception {
        onFxThread(() -> {
            FXMLLoader loader = FxTestSupport.loadMainView();
            Parent root = loader.getRoot();
            controller = loader.getController();
            queueList = (ListView<ConversionJob>) root.lookup("#queueList");
            table = (TableView<TrackPlan>) root.lookup("#trackTable");
            outputField = (TextField) root.lookup("#outputField");
            logArea = (TextArea) root.lookup("#logArea");
            changeOutputButton = (Button) root.lookup("#changeOutputButton");
            removeButton = (Button) root.lookup("#removeFromQueueButton");
            startButton = (Button) root.lookup("#startButton");
            applyToAllButton = (Button) root.lookup("#applyToAllButton");
            outputFolderButton = (Button) root.lookup("#outputFolderButton");
        });
    }

    @AfterEach
    void closeDialogs() throws Exception {
        onFxThread(() -> new ArrayList<>(Window.getWindows()).forEach(window -> {
            if (window instanceof Stage stage) {
                stage.close();
            }
        }));
    }

    // ---------------------------------------------------------------- adding

    @Test
    void emptyQueueHasNothingToEditOrRemove() {
        assertTrue(queueList.getItems().isEmpty());
        assertTrue(table.getItems().isEmpty());
        assertTrue(changeOutputButton.isDisable());
        assertTrue(removeButton.isDisable());
        assertFalse(startButton.isDisable(), "Start explains with a warning that there is nothing to convert");
    }

    @Test
    void addedFilesAreListedInOrderAndLastOneIsShown() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob second = add("Odcinek 2.mkv");

        assertEquals(List.of(first, second), queueList.getItems());
        assertSame(second, queueList.getSelectionModel().getSelectedItem());
        assertEquals(dir.resolve("Odcinek 2_fixed.mkv").toString(), outputField.getText());
    }

    @Test
    void filesWithSameNameGetDifferentOutputs() throws Exception {
        // Film.mkv and Film.mp4 would both default to Film_fixed.mkv - the second must not overwrite the first
        ConversionJob mkv = add("Film.mkv");
        ConversionJob mp4 = add("Film.mp4");

        assertEquals(dir.resolve("Film_fixed.mkv"), mkv.getOutput());
        assertEquals(dir.resolve("Film_fixed (1).mkv"), mp4.getOutput());
    }

    @Test
    void fileAlreadyWaitingIsNotAddedTwice() throws Exception {
        add("Film.mkv");

        onFxThread(() -> controller.addFiles(List.of(dir.resolve("Film.mkv"))));

        assertEquals(1, queueList.getItems().size());
        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("już są w kolejce")), alertTexts().toString());
    }

    // ---------------------------------------------------------------- selection

    @Test
    void selectingFileShowsItsTracksOutputAndLog() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        add("Odcinek 2.mkv");

        onFxThread(() -> queueList.getSelectionModel().select(first));

        assertEquals(first.getPlans().stream().filter(p -> p.getStream().type() != StreamType.ATTACHMENT).toList(),
                table.getItems());
        assertEquals(dir.resolve("Odcinek 1_fixed.mkv").toString(), outputField.getText());
        assertTrue(logArea.getText().startsWith("Wczytano Odcinek 1.mkv: 3 ścieżek"), logArea.getText());
        assertFalse(logArea.getText().contains("Odcinek 2"), "log of the other file leaked in:\n" + logArea.getText());
    }

    @Test
    void everyFileHasItsOwnSettings() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob second = add("Odcinek 2.mkv");

        onFxThread(() -> {
            queueList.getSelectionModel().select(first);
            table.getItems().get(1).setKeep(false);
        });

        assertFalse(first.getPlans().get(1).isKeep());
        assertTrue(second.getPlans().get(1).isKeep(), "change in one file must not affect another");
    }

    @Test
    void newLinesOfShownLogAppearAndHiddenLogDoesNot() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob second = add("Odcinek 2.mkv");

        onFxThread(() -> {
            second.appendLog("linia drugiego");
            first.appendLog("linia pierwszego");
        });

        assertTrue(logArea.getText().contains("linia drugiego"));
        assertFalse(logArea.getText().contains("linia pierwszego"), "only the selected file's log is shown");
    }

    // ---------------------------------------------------------------- editing lock

    @Test
    void fileBeingConvertedCannotBeEditedButOthersCan() throws Exception {
        ConversionJob running = add("Odcinek 1.mkv");
        ConversionJob waiting = add("Odcinek 2.mkv");

        onFxThread(() -> {
            queueList.getSelectionModel().select(running);
            running.markRunning();   // what JobQueue does when it takes the file
        });
        assertTrue(table.isDisable(), "table of a running file");
        assertTrue(changeOutputButton.isDisable());
        assertTrue(removeButton.isDisable(), "a running file cannot be removed");

        onFxThread(() -> queueList.getSelectionModel().select(waiting));
        assertFalse(table.isDisable(), "a waiting file stays editable while another one converts");
        assertFalse(changeOutputButton.isDisable());
        assertFalse(removeButton.isDisable());
    }

    @Test
    void finishedFileIsReadOnly() throws Exception {
        ConversionJob job = add("Film.mkv");

        onFxThread(() -> {
            job.markRunning();
            job.markDone();
        });

        assertTrue(table.isDisable());
        assertTrue(changeOutputButton.isDisable());
        assertFalse(removeButton.isDisable(), "a finished file can be removed from the list");
    }

    // ---------------------------------------------------------------- output

    @Test
    void changedOutputIsSavedInJobAndShown() throws Exception {
        ConversionJob job = add("Film.mkv");

        onFxThread(() -> controller.changeOutput(job, dir.resolve("Wynik")));

        assertEquals(dir.resolve("Wynik.mkv"), job.getOutput());
        assertEquals(dir.resolve("Wynik.mkv").toString(), outputField.getText());
    }

    @Test
    void outputCannotBeTheSourceFile() throws Exception {
        ConversionJob job = add("Film.mkv");

        onFxThread(() -> controller.changeOutput(job, dir.resolve("Film.mkv")));

        assertEquals(dir.resolve("Film_fixed.mkv"), job.getOutput());
    }

    @Test
    void outputCannotBeTakenFromAnotherQueuedFile() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob second = add("Odcinek 2.mkv");

        onFxThread(() -> controller.changeOutput(second, first.getOutput()));

        assertEquals(dir.resolve("Odcinek 2_fixed.mkv"), second.getOutput());
        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("inny film z kolejki")), alertTexts().toString());
    }

    // ---------------------------------------------------------------- remove

    @Test
    void removingSelectsNextFile() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob second = add("Odcinek 2.mkv");
        ConversionJob third = add("Odcinek 3.mkv");

        onFxThread(() -> {
            queueList.getSelectionModel().select(second);
            removeButton.fire();
        });

        assertEquals(List.of(first, third), queueList.getItems());
        assertSame(third, queueList.getSelectionModel().getSelectedItem());
    }

    @Test
    void removingLastFileClearsEditor() throws Exception {
        add("Film.mkv");

        onFxThread(removeButton::fire);

        assertTrue(queueList.getItems().isEmpty());
        assertTrue(table.getItems().isEmpty());
        assertEquals("", outputField.getText());
        assertEquals("", logArea.getText());
        assertTrue(changeOutputButton.isDisable());
    }

    // ---------------------------------------------------------------- start validation

    @Test
    void invalidFileStopsStartAndIsSelected() throws Exception {
        ConversionJob ok = add("Odcinek 1.mkv");
        ConversionJob invalid = add("Odcinek 2.mkv");
        onFxThread(() -> {
            invalid.getPlans().stream().filter(p -> p.getStream().isAudio()).forEach(p -> p.setKeep(false));
            queueList.getSelectionModel().select(ok);
        });

        onFxThread(startButton::fire);

        assertSame(invalid, queueList.getSelectionModel().getSelectedItem(), "the file to fix should be shown");
        assertEquals(JobStatus.PENDING, ok.getStatus(), "nothing starts while any file is invalid");
        assertFalse(startButton.isDisable());
        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("Odcinek 2.mkv")), alertTexts().toString());
    }

    @Test
    void startWithEmptyQueueWarns() throws Exception {
        onFxThread(startButton::fire);

        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("Dodaj pliki")), alertTexts().toString());
    }

    // ---------------------------------------------------------------- apply to all (#59)

    @Test
    void trackSettingsAreCopiedToFilesWithSameLayout() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob second = add("Odcinek 2.mkv");
        ConversionJob third = add("Odcinek 3.mkv");
        onFxThread(() -> {
            first.getPlans().get(2).setKeep(false);                      // AC3 removed
            first.getPlans().get(1).setTargetCodec(AudioCodec.EAC3);     // DTS -> E-AC3
        });

        onFxThread(() -> controller.applyToAll(first));

        for (ConversionJob job : List.of(second, third)) {
            assertFalse(job.getPlans().get(2).isKeep(), job.toString());
            assertEquals(AudioCodec.EAC3, job.getPlans().get(1).getTargetCodec(), job.toString());
        }
        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("plików: 2")), alertTexts().toString());
    }

    @Test
    void fileWithOtherLayoutIsSkippedWithReason() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob other = callOnFxThread(() -> controller.addMedia(new MediaInfo(dir.resolve("Film.mkv"), 10, List.of(
                new StreamInfo(0, StreamType.VIDEO, "h264", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "dts", null, 6, null, "eng", null, true),
                new StreamInfo(2, StreamType.AUDIO, "eac3", null, 6, null, "pol", null, false)))));
        onFxThread(() -> first.getPlans().get(2).setKeep(false));

        onFxThread(() -> controller.applyToAll(first));

        assertTrue(other.getPlans().get(2).isKeep(), "other layout must stay untouched");
        String alerts = String.join("\n", alertTexts());
        assertTrue(alerts.contains("Film.mkv: ścieżka #2: kodek eac3 zamiast ac3"), alerts);
    }

    @Test
    void applyToAllDoesNotTouchStartedFilesModeOrOutput() throws Exception {
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob done = add("Odcinek 2.mkv");
        ConversionJob quick = add("Odcinek 3.mkv");
        Path quickOutput = quick.getOutput();
        onFxThread(() -> {
            done.markRunning();
            done.markDone();
            quick.setMode(ConversionMode.QUICK);
            first.getPlans().get(2).setKeep(false);
        });

        onFxThread(() -> controller.applyToAll(first));

        assertTrue(done.getPlans().get(2).isKeep(), "finished file must not change");
        assertFalse(quick.getPlans().get(2).isKeep());
        assertEquals(ConversionMode.QUICK, quick.getMode());
        assertEquals(quickOutput, quick.getOutput());
    }

    @Test
    void applyToAllWithoutOtherFilesSaysSo() throws Exception {
        ConversionJob only = add("Film.mkv");

        onFxThread(() -> controller.applyToAll(only));

        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("Brak innych plików")), alertTexts().toString());
    }

    @Test
    void applyToAllIsOnlyForWaitingFile() throws Exception {
        ConversionJob job = add("Film.mkv");
        assertFalse(applyToAllButton.isDisable());

        onFxThread(job::markRunning);

        assertTrue(applyToAllButton.isDisable());
    }

    // ---------------------------------------------------------------- output folder for all (#59)

    @Test
    void outputsOfWaitingFilesMoveToFolderWithTheirNames() throws Exception {
        Path target = Files.createDirectory(dir.resolve("Seriale"));
        ConversionJob first = add("Odcinek 1.mkv");
        ConversionJob second = add("Odcinek 2.mkv");
        onFxThread(() -> controller.changeOutput(second, dir.resolve("Mój wynik.mkv")));

        onFxThread(() -> controller.moveOutputsTo(target));

        assertEquals(target.resolve("Odcinek 1_fixed.mkv"), first.getOutput());
        assertEquals(target.resolve("Mój wynik.mkv"), second.getOutput());
        assertEquals(target.resolve("Mój wynik.mkv").toString(), outputField.getText(), "shown file's field follows");
    }

    @Test
    void sameNamesFromDifferentFoldersDoNotCollide() throws Exception {
        Path target = Files.createDirectory(dir.resolve("Seriale"));
        Path otherDir = Files.createDirectory(dir.resolve("Inne"));
        ConversionJob first = add("Film.mkv");
        ConversionJob second = callOnFxThread(() -> controller.addMedia(media(otherDir.resolve("Film.mkv"))));

        onFxThread(() -> controller.moveOutputsTo(target));

        assertEquals(target.resolve("Film_fixed.mkv"), first.getOutput());
        assertEquals(target.resolve("Film_fixed (1).mkv"), second.getOutput());
        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("Film_fixed (1).mkv")), alertTexts().toString());
    }

    @Test
    void existingFileInFolderIsNotOverwritten() throws Exception {
        Path target = Files.createDirectory(dir.resolve("Seriale"));
        Files.createFile(target.resolve("Film_fixed.mkv"));
        ConversionJob job = add("Film.mkv");

        onFxThread(() -> controller.moveOutputsTo(target));

        assertEquals(target.resolve("Film_fixed (1).mkv"), job.getOutput());
    }

    @Test
    void startedFilesKeepTheirOutput() throws Exception {
        Path target = Files.createDirectory(dir.resolve("Seriale"));
        ConversionJob running = add("Odcinek 1.mkv");
        ConversionJob done = add("Odcinek 2.mkv");
        ConversionJob waiting = add("Odcinek 3.mkv");
        Path runningOutput = running.getOutput();
        Path doneOutput = done.getOutput();
        onFxThread(() -> {
            done.markRunning();
            done.markDone();
            running.markRunning();
        });

        onFxThread(() -> controller.moveOutputsTo(target));

        assertEquals(runningOutput, running.getOutput());
        assertEquals(doneOutput, done.getOutput());
        assertEquals(target.resolve("Odcinek 3_fixed.mkv"), waiting.getOutput());
    }

    @Test
    void folderForAllNeedsWaitingFiles() throws Exception {
        assertTrue(outputFolderButton.isDisable(), "empty queue");

        ConversionJob job = add("Film.mkv");
        assertFalse(outputFolderButton.isDisable());

        onFxThread(() -> {
            job.markRunning();
            job.markDone();
        });
        assertTrue(outputFolderButton.isDisable(), "nothing waiting");
    }

    // ---------------------------------------------------------------- drag and drop (#49)

    @Test
    void droppingOnlyOtherFilesAddsNothingAndExplains() throws Exception {
        Path notes = Files.writeString(dir.resolve("notatki.txt"), "x");

        boolean added = callOnFxThread(() -> controller.dropFiles(List.of(notes)));

        assertFalse(added, "drop must not be reported as completed");
        assertTrue(queueList.getItems().isEmpty());
        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("notatki.txt")), alertTexts().toString());
    }

    @Test
    void droppingFolderWithoutFilmsAddsNothing() throws Exception {
        Path photos = Files.createDirectory(dir.resolve("Zdjęcia"));

        assertFalse(callOnFxThread(() -> controller.dropFiles(List.of(photos))));
        assertTrue(queueList.getItems().isEmpty());
    }

    // ---------------------------------------------------------------- helpers

    private ConversionJob add(String fileName) throws Exception {
        return callOnFxThread(() -> controller.addMedia(media(dir.resolve(fileName))));
    }

    /** 0 video, 1 DTS (default), 2 AC3. */
    private static MediaInfo media(Path path) {
        return new MediaInfo(path, 10, List.of(
                new StreamInfo(0, StreamType.VIDEO, "h264", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "dts", null, 6, null, "eng", null, true),
                new StreamInfo(2, StreamType.AUDIO, "ac3", null, 6, null, "pol", null, false)));
    }

    /** Header and content of every open alert. */
    private static List<String> alertTexts() throws Exception {
        return callOnFxThread(() -> Window.getWindows().stream()
                .filter(window -> window.getScene() != null && window.getScene().getRoot() instanceof DialogPane)
                .map(window -> (DialogPane) window.getScene().getRoot())
                .map(pane -> pane.getHeaderText() + "\n" + pane.getContentText())
                .toList());
    }
}
