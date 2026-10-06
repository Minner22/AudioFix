package pl.audiofix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import pl.audiofix.AudioFixApp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Runs JavaFX code in tests without showing any window.
 */
final class FxTestSupport {

    private FxTestSupport() {
    }

    static void startJavaFx() {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> started.complete(null));
        } catch (IllegalStateException alreadyStarted) {
            started.complete(null);
        }
        Platform.setImplicitExit(false);
        started.orTimeout(10, TimeUnit.SECONDS).join();
    }

    /** Code that may throw, to be run on the JavaFX Application Thread. */
    interface FxAction {
        void run() throws Exception;
    }

    /** Runs the action on the JavaFX Application Thread and waits until it is done. */
    static void onFxThread(FxAction action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        try {
            done.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    /** Loads main-view.fxml; must be called on the JavaFX Application Thread. */
    static FXMLLoader loadMainView() throws Exception {
        FXMLLoader loader = new FXMLLoader(AudioFixApp.class.getResource("ui/main-view.fxml"));
        Parent root = loader.load();
        new Scene(root, 1100, 700);
        layout(root);
        return loader;
    }

    /** Nodes created by skins (cells, toolbar items) exist only after CSS and layout pass. */
    static void layout(Parent root) {
        root.applyCss();
        root.layout();
    }
}
