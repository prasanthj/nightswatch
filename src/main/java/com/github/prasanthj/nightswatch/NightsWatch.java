package com.github.prasanthj.nightswatch;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Created by prasanthj on 7/5/18.
 */
public class NightsWatch {

  public static void main(String[] args) {
    Server server = new Server(9898);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new ProfileServlet()), "/prof");
    try {
      server.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
