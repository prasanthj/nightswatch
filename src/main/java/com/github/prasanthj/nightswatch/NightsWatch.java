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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NightsWatch {
  private static Logger LOG = LoggerFactory.getLogger(NightsWatch.class);

  public static void main(String[] args) {
    int port = 9898;
    if (args.length == 1) {
      port = Integer.parseInt(args[0]);
    }
    Server server = new Server(port);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new ProfileServlet()), "/prof");
    try {
      server.start();
    } catch (Exception e) {
      LOG.error("Unable to start jetty server", e);
    }
  }
}
