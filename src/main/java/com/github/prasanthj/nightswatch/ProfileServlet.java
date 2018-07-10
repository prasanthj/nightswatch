/*
 * Copyright 2018 Prasanth Jayachandran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.prasanthj.nightswatch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
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
 * Servlet that runs async-profiler as web-endpoint.
 */
public class ProfileServlet extends HttpServlet {
  private static Logger LOG = LoggerFactory.getLogger(ProfileServlet.class);
  private static final String ASYNC_PROFILER_HOME_ENV = "ASYNC_PROFILER_HOME";
  private static final String PROFILER_SCRIPT = "/profiler.sh";

  private static final int DEFAULT_DURATION_SECONDS = 30;
  private static final String DEFAULT_OUTPUT_TYPE = "svg";
  private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
  private static final String ALLOWED_METHODS = "GET";
  private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  private static final String CONTENT_TYPE_SVG = "image/svg+xml; charset=utf-8";
  private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
  private static final String CONTENT_TYPE_TEXT = "text/plain; charset=utf-8";
  private static final String CONTENT_TYPE_BINARY = "application/octet-stream";

  private Lock profilerLock = new ReentrantLock();
  private String pid;
  private String asyncProfilerHome;
  private List<EventType> supportedEvents;

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

  public ProfileServlet() {
    this.asyncProfilerHome = System.getenv(ASYNC_PROFILER_HOME_ENV);
    try {
      this.pid = "" + ProcessUtils.getPid();
    } catch (IllegalStateException e) {
      this.pid = null;
    }
    LOG.info("Servlet process PID: {} asyncProfilerHome: {}", pid, asyncProfilerHome);
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    if (asyncProfilerHome == null || asyncProfilerHome.trim().isEmpty()) {
      setResponseHeader(resp, "text");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      resp.getWriter().write("ASYNC_PROFILER_HOME env is not set.");
      return;
    }

    if (pid == null) {
      setResponseHeader(resp, "text");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      resp.getWriter().write("Unable to determine PID of current process.");
      return;
    }

    // if pid is explicitly specified, use it else default to current process
    if (req.getParameter("pid") != null) {
      pid = req.getParameter("pid");
    }

    // default is CPU profile
    EventType eventType = EventType.CPU;
    if (req.getParameter("event") != null) {
      eventType = EventType.fromEventName(req.getParameter("event").trim().toLowerCase());
      eventType = eventType == null ? EventType.CPU : eventType;
    }

    // 30s default duration
    int duration = DEFAULT_DURATION_SECONDS;
    if (req.getParameter("duration") != null) {
      try {
        duration = Integer.parseInt(req.getParameter("duration"));
      } catch (NumberFormatException e) {
        // ignore and use default
      }
    }

    // default to svg flamegraph
    String output = DEFAULT_OUTPUT_TYPE;
    if (req.getParameter("output") != null) {
      output = req.getParameter("output").trim().toLowerCase();
    }

    // Options from async-profiler ./profiler.sh
    //  -e event          profiling event: cpu|alloc|lock|cache-misses etc.
    //  -d duration       run profiling for <duration> seconds
    //  -i interval       sampling interval in nanoseconds
    //  -j jstackdepth    maximum Java stack depth
    //  -b bufsize        frame buffer size
    //  -t                profile different threads separately
    //  -s                simple class names instead of FQN
    //  -o fmt[,fmt...]   output format: summary|traces|flat|collapsed|svg|tree|jfr
    //  --title string    SVG title
    //  --width px        SVG width
    //  --height px       SVG frame height
    //  --minwidth px     skip frames smaller than px
    //  --reverse         generate stack-reversed FlameGraph / Call tree
    String interval = req.getParameter("interval");
    String jstackDepth = req.getParameter("jstackdepth");
    String bufsize = req.getParameter("bufsize");
    String thread = req.getParameter("thread");
    String simple = req.getParameter("simple");
    String title = req.getParameter("title");
    String width = req.getParameter("width");
    String height = req.getParameter("height");
    String minwidth = req.getParameter("minwidth");
    String reverse = req.getParameter("reverse");
    profilerLock.lock();
    try {
      populateSupportedEvents();
      File outputFile = File.createTempFile("async-prof-pid-" + pid, "." + output);
      outputFile.deleteOnExit();
      List<String> cmd = new ArrayList<>();
      cmd.add(asyncProfilerHome + PROFILER_SCRIPT);
      cmd.add("-e");
      cmd.add(eventType.getEventName());
      cmd.add("-d");
      cmd.add("" + duration);
      cmd.add("-o");
      cmd.add(output);
      cmd.add("-f");
      cmd.add(outputFile.getAbsolutePath());
      if (interval != null) {
        cmd.add("-i");
        cmd.add(interval);
      }
      if (jstackDepth != null) {
        cmd.add("-j");
        cmd.add(jstackDepth);
      }
      if (bufsize != null) {
        cmd.add("-b");
        cmd.add(bufsize);
      }
      if (thread != null) {
        cmd.add("-t");
      }
      if (simple != null) {
        cmd.add("-s");
      }
      if (title != null) {
        cmd.add("--title");
        cmd.add(title);
      }
      if (width != null) {
        cmd.add("--width");
        cmd.add(width);
      }
      if (height != null) {
        cmd.add("--height");
        cmd.add(height);
      }
      if (minwidth != null) {
        cmd.add("--minwidth");
        cmd.add(minwidth);
      }
      if (reverse != null) {
        cmd.add("--reverse");
      }
      cmd.add(pid);
      int ret = ProcessUtils.runCmdSync(cmd);
      if (ret == 0) {
        setResponseHeader(resp, output);
        byte[] b = Files.readAllBytes(outputFile.toPath());
        OutputStream os = resp.getOutputStream();
        os.write(b);
        os.flush();
      } else {
        setResponseHeader(resp, "text");
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().write("Error executing async-profiler");
      }
    } finally {
      profilerLock.unlock();
    }
  }

  private void populateSupportedEvents() {
    // get the list of supported events once
    if (supportedEvents == null) {
      supportedEvents = new ArrayList<>();
      List<String> cmd = new ArrayList<>();
      cmd.add(asyncProfilerHome + PROFILER_SCRIPT);
      cmd.add("list");
      cmd.add(pid);
      List<String> outLines = ProcessUtils.runCmd(cmd);
      for (String out : outLines) {
        EventType et = EventType.fromEventName(out.trim());
        if (et != null) {
          supportedEvents.add(et);
        }
      }
      // if event types cannot be determined, add the minimum support CPU profiling
      if (supportedEvents.isEmpty()) {
        supportedEvents.add(EventType.CPU);
      }
    }
  }

  private void setResponseHeader(final HttpServletResponse response, final String output) {
    response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
    response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    if (output.equalsIgnoreCase("svg")) {
      response.setContentType(CONTENT_TYPE_SVG);
    } else if (output.equalsIgnoreCase("tree")) {
      response.setContentType(CONTENT_TYPE_HTML);
    } else if (output.equalsIgnoreCase("jfr")) {
      response.setContentType(CONTENT_TYPE_BINARY);
    } else {
      response.setContentType(CONTENT_TYPE_TEXT);
    }
  }
}