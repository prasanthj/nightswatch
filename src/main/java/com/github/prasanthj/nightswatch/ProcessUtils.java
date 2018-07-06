package com.github.prasanthj.nightswatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by prasanthj on 7/5/18.
 */
public class ProcessUtils {
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

  public static Process runCmdAsync(String... cmd) {
    try {
      return new ProcessBuilder(cmd).start();
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static List<String> stopCmd(Process process) {
    List<String> messages = new ArrayList<>();
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      process.destroy();
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        messages.add(baos.toString());
      }

      return messages;
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }
}
