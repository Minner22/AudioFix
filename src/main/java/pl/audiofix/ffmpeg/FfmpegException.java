package pl.audiofix.ffmpeg;

import pl.audiofix.model.AudioFixException;
import pl.audiofix.model.UserMessage;

public class FfmpegException extends AudioFixException {

    public FfmpegException(String englishMessage, UserMessage userMessage) {

        super(englishMessage, userMessage);
    }

    public FfmpegException(String englishMessage, UserMessage userMessage, Throwable cause) {

        super(englishMessage, userMessage, cause);
    }
}
