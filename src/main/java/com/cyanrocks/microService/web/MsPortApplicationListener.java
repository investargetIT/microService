package com.cyanrocks.microService.web;

import com.cyanrocks.microService.helper.CoordinateUtil;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

public class MsPortApplicationListener implements ApplicationListener<WebServerInitializedEvent> {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   public void onApplicationEvent(WebServerInitializedEvent arg0) {
      CoordinateUtil.PORT = arg0.getWebServer().getPort();
      log.info("current microservice port:{}", CoordinateUtil.PORT);
   }
}
