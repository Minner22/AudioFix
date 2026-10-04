package pl.audiofix;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AudioFixAppTest {

    @Test
    public void mainViewFxmlIsOnClasspath() {

        assertNotNull(AudioFixApp.class.getResource("ui/main-view.fxml"));
    }
}
