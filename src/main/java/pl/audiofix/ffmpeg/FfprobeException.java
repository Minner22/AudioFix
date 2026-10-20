package pl.audiofix.ffmpeg;

import pl.audiofix.model.AudioFixException;
import pl.audiofix.model.UserMessage;

public class FfprobeException extends AudioFixException {

    public FfprobeException(String englishMessage, UserMessage userMessage) {

        super(englishMessage, userMessage);
    }

    public FfprobeException(String englishMessage, UserMessage userMessage, Throwable cause) {

        super(englishMessage, userMessage, cause);
    }
}
