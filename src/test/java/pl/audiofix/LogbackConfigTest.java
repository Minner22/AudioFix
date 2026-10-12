package pl.audiofix;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The app's logback.xml (not logback-test.xml) loaded into its own logger context,
 * with the log folder pointed at a temporary directory.
 */
class LogbackConfigTest {

    @TempDir
    Path logs;

    private LoggerContext context;

    @BeforeEach
    void configure() throws Exception {
        context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());   // SLF4J sets it for the real context
        context.putProperty(LogFiles.PROPERTY, logs.toString());   // what Launcher sets as a system property
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(LogbackConfigTest.class.getResource("/logback.xml"));
    }

    @AfterEach
    void stop() {
        context.stop();
    }

    @Test
    void appLogGoesToFileInLogFolder() throws Exception {
        context.getLogger("pl.audiofix.ffmpeg.FfmpegLocator").info("Using saved ffmpeg: C:\\ffmpeg\\bin\\ffmpeg.exe");
        context.stop();   // flush and close

        String log = Files.readString(logs.resolve("audiofix.log"), StandardCharsets.UTF_8);
        assertTrue(log.contains("INFO"), log);
        assertTrue(log.contains("Using saved ffmpeg: C:\\ffmpeg\\bin\\ffmpeg.exe"), log);
    }

    @Test
    void polishLettersSurvive() throws Exception {
        context.getLogger("pl.audiofix.ui.MainController").warn("Błąd: nie można odczytać ścieżek");
        context.stop();

        assertTrue(Files.readString(logs.resolve("audiofix.log"), StandardCharsets.UTF_8).contains("Błąd: nie można odczytać ścieżek"));
    }

    @Test
    void lineHasDateNotOnlyTime() throws Exception {
        // a file holds a whole day and old files are kept: the time alone would be ambiguous
        context.getLogger("pl.audiofix.AudioFixApp").info("start");
        context.stop();

        String line = Files.readAllLines(logs.resolve("audiofix.log"), StandardCharsets.UTF_8).getFirst();
        assertTrue(line.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} .*"), line);
    }

    @Test
    void filesRollDailyAndOldOnesAreRemoved() {
        RollingFileAppender<ILoggingEvent> file = fileAppender();
        TimeBasedRollingPolicy<?> policy = assertInstanceOf(TimeBasedRollingPolicy.class, file.getRollingPolicy());

        assertEquals(logs.resolve("audiofix.%d{yyyy-MM-dd}.log").toString().replace('\\', '/'),
                policy.getFileNamePattern().replace('\\', '/'));
        assertEquals(14, policy.getMaxHistory());
    }

    @Test
    void oldFilesNeverTakeMoreThan50Mb() throws Exception {
        // the policy has no getter for it, so the setting is read from the file itself
        try (var in = LogbackConfigTest.class.getResourceAsStream("/logback.xml")) {
            assertTrue(new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("<totalSizeCap>50MB</totalSizeCap>"));
        }
    }

    @Test
    void consoleStaysForIde() {
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        assertInstanceOf(ConsoleAppender.class, root.getAppender("CONSOLE"));
    }

    @Test
    void appInfoIsLoggedButLibraryInfoIsNot() {
        assertTrue(context.getLogger("pl.audiofix.queue.JobQueue").isInfoEnabled());
        assertTrue(!context.getLogger("javafx.scene").isInfoEnabled(), "only warnings from libraries");
    }

    @SuppressWarnings("unchecked")
    private RollingFileAppender<ILoggingEvent> fileAppender() {
        Appender<ILoggingEvent> appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("FILE");
        assertNotNull(appender, "no FILE appender on the root logger");
        return assertInstanceOf(RollingFileAppender.class, appender);
    }
}
