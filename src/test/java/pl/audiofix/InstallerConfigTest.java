package pl.audiofix;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Files the installer profile in pom.xml uses (#14). Building the installer itself takes a minute and,
 * for the exe, WiX Toolset, so it is checked by hand: mvn -Pinstaller package.
 */
class InstallerConfigTest {

    private static final Path INSTALLER = Path.of("src", "installer");

    @Test
    void iconHasSizesFromSmallToLarge() throws IOException {
        ByteBuffer ico = ByteBuffer.wrap(Files.readAllBytes(INSTALLER.resolve("AudioFix.ico"))).order(ByteOrder.LITTLE_ENDIAN);

        assertEquals(0, ico.getShort(0), "not an .ico file");
        assertEquals(1, ico.getShort(2), "type 1 means icon");
        List<Integer> sizes = new ArrayList<>();
        for (int i = 0; i < ico.getShort(4); i++) {
            int width = Byte.toUnsignedInt(ico.get(6 + 16 * i));
            sizes.add(width == 0 ? 256 : width);
        }
        assertTrue(sizes.contains(16) && sizes.contains(32) && sizes.contains(48) && sizes.contains(256),
                "Explorer, Start menu and taskbar need these: " + sizes);
    }

    @Test
    void exeIsInstalledPerUserWithStartMenuAndShortcut() throws IOException {
        String exe = Files.readString(INSTALLER.resolve("exe.args"), StandardCharsets.UTF_8);

        assertTrue(exe.contains("--win-menu"), "no Start menu entry");
        assertTrue(exe.contains("--win-shortcut"), "no desktop shortcut");
        assertTrue(exe.contains("--win-per-user-install"), "an install for all users needs administrator rights");
    }

    @Test
    void everyVersionUpgradesTheSameInstallation() throws IOException {
        // without a fixed upgrade code each new installer would add a second AudioFix next to the old one
        Matcher uuid = Pattern.compile("--win-upgrade-uuid ([0-9a-f-]{36})")
                .matcher(Files.readString(INSTALLER.resolve("exe.args"), StandardCharsets.UTF_8));

        assertTrue(uuid.find(), "no fixed --win-upgrade-uuid");
        assertEquals("9baec39d-4aca-4dc7-bd01-70efbfffd09e", uuid.group(1), "changing it breaks upgrades of installed copies");
    }

    @Test
    void installsIntoDefaultFolderWithoutAsking() throws IOException {
        // a nested --install-dir (e.g. Programs/AudioFix) fails WiX validation ICE64 for per-user installs;
        // a folder chooser would ask on every upgrade, because the log folder makes the install folder non-empty
        String exe = Files.readString(INSTALLER.resolve("exe.args"), StandardCharsets.UTF_8);

        assertFalse(exe.contains("--install-dir"), "installs to %LOCALAPPDATA%\\AudioFix");
        assertFalse(exe.contains("--win-dir-chooser"));
    }

    @Test
    void installerTextsFitMsiCodePage() throws IOException {
        // the MSI database is Windows-1252: a Polish letter in the description stops WiX with LGHT0311
        Matcher description = Pattern.compile("<argument>--description</argument>\\s*<argument>([^<]*)</argument>")
                .matcher(Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8));

        assertTrue(description.find(), "no --description");
        assertTrue(Charset.forName("windows-1252").newEncoder().canEncode(description.group(1)), description.group(1));
    }

    @Test
    void appImageHasNoInstallerOptions() throws IOException {
        // jpackage stops with an error when an exe-only option is given for the app-image type
        assertEquals("", Files.readString(INSTALLER.resolve("app-image.args"), StandardCharsets.UTF_8).strip());
    }

    @Test
    void runtimeKeepsPolishFormatting() throws IOException {
        // without locale data "32,4 GB" turns into "32.4 GB" in the installed app, while every test still passes
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);

        assertTrue(pom.contains("jdk.localedata"), "locale data missing from the runtime modules");
        assertTrue(pom.contains("--include-locales=pl,en"), "only Polish and English locale data, the rest is 30 MB");
    }

    @Test
    void installerStartsTheLauncher() throws IOException {
        // Launcher sets the log folder before the first logger and works around JavaFX on the classpath
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);

        assertTrue(Pattern.compile("<argument>--main-class</argument>\\s*<argument>pl\\.audiofix\\.Launcher</argument>")
                .matcher(pom).find(), "jpackage must start pl.audiofix.Launcher");
    }
}
