package pl.audiofix.model;

public enum JobStatus {

    PENDING,
    RUNNING,
    DONE,
    FAILED,
    CANCELLED;

    public boolean isFinished() {

        return this == DONE || this == FAILED || this == CANCELLED;
    }
}
