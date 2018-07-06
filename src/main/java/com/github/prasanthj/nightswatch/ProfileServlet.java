package com.github.prasanthj.nightswatch;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by prasanthj on 7/5/18.
 */
public class ProfileServlet extends HttpServlet {
  private static Logger LOG = LoggerFactory.getLogger(ProfileServlet.class);
  private static final String ASYNC_PROFILER_HOME_ENV = "ASYNC_PROFILER_HOME";
  private static final String DEFAULT_OUTPUT_DIR = "/tmp";
  private static final int DEFAULT_DURATION_SECONDS = 30;

  private Lock profilerLock = new ReentrantLock();
  private String pid;
  private String profilesOutputPath = DEFAULT_OUTPUT_DIR;
  private String asyncProfilerHome;
  private Process profProcess;
  private boolean profilerRunning;
  private boolean explicitlyStarted;
  private ProfileStatus profileStatus;
  private List<ProfileStatus.EventType> supportedEvents;

  public ProfileServlet() {
    this.asyncProfilerHome = System.getenv(ASYNC_PROFILER_HOME_ENV);
    try {
      this.pid = "" + ProcessUtils.getPid();
    } catch (IllegalStateException e) {
      this.pid = null;
    }
    LOG.info("Servlet process PID: {} asyncProfilerHome: {} outputDir: {}", pid, asyncProfilerHome, profilesOutputPath);
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    if (profilerRunning) {
      resp.getWriter().write("Profiler is running already..");
    } else {
      resp.getWriter().write("Profiler is not running.");
    }
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    if (asyncProfilerHome == null || asyncProfilerHome.trim().isEmpty()) {
      resp.getWriter().write("ASYNC_PROFILER_HOME env is not set.");
      return;
    }

    if (pid == null) {
      resp.getWriter().write("Unable to determine PID of current process.");
      return;
    }

    ProfileStatus.EventType eventType = null;
    if (req.getParameter("event") != null) {
      eventType = ProfileStatus.EventType.fromEventName(req.getParameter("event"));
    }

    int duration = DEFAULT_DURATION_SECONDS;
    if (req.getParameter("duration") != null) {
      try {
        duration = Integer.parseInt(req.getParameter("duration"));
      } catch (NumberFormatException e) {
        // ignore and use default
      }
    }

    profilerLock.lock();
    try {

      if (!profilerRunning) {
        profileStatus = new ProfileStatus(profilesOutputPath, eventType);
        populateSupportedEvents(profileStatus);
        String outputFile = profileStatus.getOutputFile();
        if (outputFile != null) {
          // if start and duration are specified, duration takes precedence
          explicitlyStarted = req.getParameter("start") != null && req.getParameter("duration") == null;
          LOG.info("Starting async-profiler.. explicitStart: {}", explicitlyStarted);
          List<String> cmd = new ArrayList<>();
          cmd.add(asyncProfilerHome + "/profiler.sh");
          if (explicitlyStarted) {
            cmd.add("start");
            cmd.add("-f");
          } else {
            cmd.add("-d");
            cmd.add("" + duration);
            profileStatus.setDurationSeconds(duration);
          }
          if (eventType != null) {
            cmd.add("-e");
            cmd.add(eventType.getEventName());
          }
          cmd.add("-f");
          cmd.add(outputFile);
          cmd.add(pid);
          profProcess = ProcessUtils.runCmdAsync(cmd);
          profileStatus.setStatus(ProfileStatus.Status.RUNNING);
          profilerRunning = true;
        }
      } else {
        if (explicitlyStarted) {
          List<String> cmd = new ArrayList<>();
          cmd.add(asyncProfilerHome + "/profiler.sh");
          if (req.getParameter("stop") != null) {
            LOG.info("Profiler stop requested..");
            cmd.add("stop");
            if (eventType != null) {
              cmd.add("-e");
              cmd.add(eventType.getEventName());
            }
            cmd.add("-f");
            cmd.add(profileStatus.getOutputFile());
            cmd.add(pid);
            profProcess = ProcessUtils.runCmdAsync(cmd);
            profileStatus.setStatus(ProfileStatus.Status.STOPPED);
            profileStatus.setDurationSeconds(
              (int) ((System.currentTimeMillis() - profileStatus.getStartTimestamp()) / 1000));
            profilerRunning = false;
          } else {
            LOG.info("Profiler started explicitly.. waiting for explicit stop..");
          }
        } else {
          if (profProcess != null && !profProcess.isAlive()) {
            profileStatus.setStatus(ProfileStatus.Status.STOPPED);
            profilerRunning = false;
            LOG.info("Last profiler run started at {} and stopped after {} seconds..",
              new Timestamp(profileStatus.getStartTimestamp()), profileStatus.getDurationSeconds());
          } else {
            LOG.info("Profiler started at {}.. wait until {} seconds for it to stop automatically..",
              new Timestamp(profileStatus.getStartTimestamp()), profileStatus.getDurationSeconds());
          }
        }
      }

      LOG.info(profileStatus.toString());
      resp.getWriter().write(profileStatus.toString());
    } finally {
      profilerLock.unlock();
    }
  }

  private void populateSupportedEvents(final ProfileStatus profileStatus) {
    // get the list of supported events once
    if (supportedEvents == null) {
      supportedEvents = new ArrayList<>();
      List<String> cmd = new ArrayList<>();
      cmd.add(asyncProfilerHome + "/profiler.sh");
      cmd.add("list");
      cmd.add(pid);
      List<String> outLines = ProcessUtils.runCmd(cmd);
      for (String out : outLines) {
        ProfileStatus.EventType et = ProfileStatus.EventType.fromEventName(out.trim());
        if (et != null) {
          supportedEvents.add(et);
        }
      }
      // if event types cannot be determined, add the minimum support CPU profiling
      if (supportedEvents.isEmpty()) {
        supportedEvents.add(ProfileStatus.EventType.CPU);
      }
    }
    profileStatus.setSupportedEvents(supportedEvents);
  }
}
