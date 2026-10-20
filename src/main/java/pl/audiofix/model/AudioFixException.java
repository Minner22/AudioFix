package pl.audiofix.model;


public class AudioFixException extends RuntimeException {

    private final UserMessage userMessage;

    public AudioFixException(String englishMessage, UserMessage userMessage) {

        super(englishMessage);
        this.userMessage = userMessage;
    }

    public AudioFixException(String englishMessage, UserMessage userMessage, Throwable cause) {

        super(englishMessage, cause);
        this.userMessage = userMessage;
    }

    public UserMessage userMessage() {

        return userMessage;
    }
}
