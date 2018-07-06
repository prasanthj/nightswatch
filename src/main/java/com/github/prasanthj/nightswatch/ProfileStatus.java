package com.github.prasanthj.nightswatch;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Created by prasanthj on 7/6/18.
 */
public class ProfileStatus {
  public enum Status {
    RUNNING,
    STOPPED
  }

  public enum EventType {
    CPU("cpu"),
    ALLOC("alloc"),
    LOCK("lock"),
    CACHE_MISSES("cache-misses");

    private String eventName;
    EventType(final String eventName) {
      this.eventName = eventName;
    }

    public String getEventName() {
      return eventName;
    }

    public static EventType fromEventName(String eventName) {
      for (EventType eventType : values()) {
        if (eventType.getEventName().equalsIgnoreCase(eventName)) {
          return eventType;
        }
      }

      return null;
    }


    @Override
    public String toString() {
      return getEventName();
    }
  }

  private String profileId;
  private Status status;
  private String outputFile;
  private long startTimestamp;
  private int durationSeconds;

  // TODO: fill this
  private List<EventType> supportedEvents;

  public ProfileStatus(final String outputDir) {
    this.startTimestamp = System.currentTimeMillis();
    this.profileId = UUID.randomUUID().toString();
    File outFile = new File(outputDir, profileId);
    if (outFile.mkdirs()) {
      this.outputFile = outFile.getAbsolutePath() + "/profile.svg";
    }
  }

  public long getStartTimestamp() {
    return startTimestamp;
  }

  public String getProfileId() {
    return profileId;
  }

  public void setProfileId(final String profileId) {
    this.profileId = profileId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(final Status status) {
    this.status = status;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public void setOutputFile(final String outputFile) {
    this.outputFile = outputFile;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(final int durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public List<EventType> getSupportedEvents() {
    return supportedEvents;
  }

  public void setSupportedEvents(final List<EventType> supportedEvents) {
    this.supportedEvents = supportedEvents;
  }

  @Override
  public String toString() {
    return "profileId: " + profileId + ", status: " + status + " outputFile: " + outputFile +
      " startTimestamp: " + new Timestamp(startTimestamp) + " durationSeconds: " + durationSeconds +
      " supportedEvents: " + supportedEvents;
  }
}
