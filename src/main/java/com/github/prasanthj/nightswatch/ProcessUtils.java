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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessUtils {
  private static Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);

  static long getPid() {
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

  static int runCmdSync(List<String> cmd) {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Running command: " + cmd);
      }
      Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
      return p.waitFor();
    } catch (InterruptedException | IOException e) {
      return -1;
    }
  }

  static List<String> runCmd(List<String> cmd) {
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
