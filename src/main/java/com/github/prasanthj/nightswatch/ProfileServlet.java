package com.github.prasanthj.nightswatch;

import java.io.IOException;
import java.sql.Timestamp;
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
  public static final String ASYNC_PROFILER_HOME_ENV = "ASYNC_PROFILER_HOME";
  public static final int DEFAULT_DURATION_SECONDS = 30;

  private Lock profilerLock = new ReentrantLock();
  private long pid;
  private String profilesOutputPath;
  private String asyncProfilerHome;
  private Process profProcess;
  private boolean profilerRunning;
  private boolean explicitlyStarted;
  private int profilingDurationSeconds;
  private ProfileStatus profileStatus;

  public ProfileServlet() {
    this.asyncProfilerHome = System.getenv(ASYNC_PROFILER_HOME_ENV);
    this.profilesOutputPath = System.getProperty("java.io.tmpdir");
    try {
      this.pid = ProcessUtils.getPid();
    } catch (IllegalStateException e) {
      this.pid = -1;
    }
    this.profilingDurationSeconds = DEFAULT_DURATION_SECONDS;
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

    if (pid == -1) {
      resp.getWriter().write("Unable to determine PID of current process.");
      return;
    }

    profilerLock.lock();
    try {
      if (!profilerRunning) {
        // TODO: fill supported events once
        profileStatus = new ProfileStatus(profilesOutputPath);
        String outputFile = profileStatus.getOutputFile();
        if (outputFile != null) {
          explicitlyStarted = req.getParameter("start") != null;
          LOG.info("Starting async-profiler.. explicitStart: {}", explicitlyStarted);
          if (explicitlyStarted) {
            profProcess = ProcessUtils.runCmdAsync(asyncProfilerHome + "/profiler.sh",
              "start",
              "-f", outputFile,
              "" + pid);
          } else {
            profProcess = ProcessUtils.runCmdAsync(asyncProfilerHome + "/profiler.sh",
              "-d", "" + profilingDurationSeconds,
              "-f", outputFile,
              "" + pid);
            profileStatus.setDurationSeconds(profilingDurationSeconds);
          }
          profileStatus.setStatus(ProfileStatus.Status.RUNNING);
          profilerRunning = true;
        }
      } else {
        if (explicitlyStarted) {
          if (req.getParameter("stop") != null) {
            LOG.info("Profiler stop requested..");
            profProcess = ProcessUtils.runCmdAsync(asyncProfilerHome + "/profiler.sh",
              "stop",
              "-f", profileStatus.getOutputFile(),
              "" + pid);
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
}
