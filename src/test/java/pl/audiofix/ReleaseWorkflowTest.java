package pl.audiofix;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The release workflow (#40) as text: GitHub runs it only after a tag is pushed, so the parts that decide
 * whether a release is published, and with what, are checked here. A pull request that changes it also
 * runs it without publishing.
 */
class ReleaseWorkflowTest {

    private static final Path RELEASE = Path.of(".github", "workflows", "release.yml");
    private static final Path BUILD = Path.of(".github", "workflows", "build.yml");
    private static final Path FFMPEG = Path.of(".github", "actions", "install-ffmpeg", "action.yml");

    @Test
    void versionTagsStartTheRelease() throws IOException {
        assertTrue(read(RELEASE).contains("tags: [ 'v*' ]"));
    }

    @Test
    void tagMustMatchPomVersion() throws IOException {
        // tag v1.0.0 on a commit with 1.0.0-SNAPSHOT or 1.0.1 must not publish anything
        String release = read(RELEASE);

        assertTrue(release.contains("if ($tag -ne \"v$version\")"), "no tag check");
        assertTrue(release.contains("-like \"*-SNAPSHOT\""), "a -SNAPSHOT version must not be released");
    }

    @Test
    void releaseIsTestedAndHasTheInstaller() throws IOException {
        // verify runs the tests; the installer profile adds jpackage and the shipped ffmpeg
        assertTrue(read(RELEASE).contains("mvn -B -ntp -Pinstaller verify"));
    }

    @Test
    void onlyTagsPublish() throws IOException {
        String release = read(RELEASE);
        String publish = release.substring(release.indexOf("- name: Publish the release"));

        assertTrue(publish.startsWith("- name: Publish the release\n        if: github.event_name == 'push'"),
                "a pull request must not create a release");
        assertTrue(publish.contains("--verify-tag"), "gh would otherwise create a missing tag itself");
    }

    @Test
    void releaseCandidatesArePreReleases() throws IOException {
        String release = read(RELEASE);

        assertTrue(release.contains("-match \"-(rc|beta|alpha)\""));
        assertTrue(release.contains("--prerelease"));
    }

    @Test
    void installerComesWithItsChecksum() throws IOException {
        assertTrue(read(RELEASE).contains("Get-FileHash $exe -Algorithm SHA256"));
    }

    @Test
    void changesToTheReleaseProcessAreTriedOnPullRequests() throws IOException {
        String release = read(RELEASE);

        for (String path : new String[] {".github/workflows/release.yml", ".github/actions/**", "src/installer/**", "pom.xml"}) {
            assertTrue(release.contains("- " + path), "a change to " + path + " is not tried before a release");
        }
    }

    @Test
    void buildAndReleaseTestWithTheSameFfmpeg() throws IOException {
        // one place for the ffmpeg used by the integration tests, so the two workflows cannot drift apart
        assertTrue(read(BUILD).contains("uses: ./.github/actions/install-ffmpeg"));
        assertTrue(read(RELEASE).contains("uses: ./.github/actions/install-ffmpeg"));
        assertTrue(read(FFMPEG).contains("Get-FileHash ffmpeg.zip -Algorithm SHA256"), "download is not checked");
    }

    /** With \n line ends, also when git checked the file out with \r\n on Windows. */
    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
