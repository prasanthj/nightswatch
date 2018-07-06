package com.github.prasanthj.nightswatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by prasanthj on 7/5/18.
 */
public class ProcessUtils {
  private static Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);
  public static long getPid() {
    String name = ManagementFactory.getRuntimeMXBean().getName();

    if (name != null) {
      int idx = name.indexOf("@");

      if (idx != -1) {
        String str = name.substring(0, name.indexOf("@"));
        try {
          return Long.valueOf(str);
        } catch (NumberFormatException nfe) {
          throw new IllegalStateException("Process PID is not a number: " + str);
        }
      }
    }
    throw new IllegalStateException("Unsupported PID format: " + name);
  }

  public static Process runCmdAsync(List<String> cmd) {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Running command async: " + cmd);
      }
      return new ProcessBuilder(cmd).start();
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static List<String> runCmd(List<String> cmd) {
    List<String> messages = new ArrayList<>();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Running command: " + cmd);
      }
      Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
      BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        messages.add(line);
      }
      p.waitFor();
    } catch (InterruptedException | IOException e) {
      messages.add(e.getMessage());
    }
    return messages;
  }
}
