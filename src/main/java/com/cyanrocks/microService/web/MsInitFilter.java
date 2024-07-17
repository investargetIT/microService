package com.cyanrocks.microService.web;

import com.cyanrocks.microService.helper.CoordinateUtil;
import java.lang.invoke.MethodHandles;
import javax.annotation.Resource;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ServletContextAware;

public class MsInitFilter implements ServletContextAware {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   @Value("${ws.mid.url:#{null}}")
   private String midDomain;
   @Value("${enable.api.auto.scan:#{null}}")
   private Boolean autoScan;
   @Resource
   private Environment environment;

   public void setServletContext(ServletContext servletContext) {
      try {
         log.info("欢迎使用无单点微服务设计");
         if (this.midDomain == null) {
            this.midDomain = this.environment.getProperty("ws.mid.url");
         }

         if (this.autoScan == null) {
            this.autoScan = (Boolean)this.environment.getProperty("enable.api.auto.scan", Boolean.class);
         }

         if (CoordinateUtil.WS_REMOTE_URL == null && this.midDomain != null) {
            CoordinateUtil.WS_REMOTE_URL = this.midDomain.trim();
         }

         if (this.autoScan != null) {
            CoordinateUtil.AUTO_SCAN = this.autoScan;
         }

         CoordinateUtil.connCoor(servletContext.getContextPath());
      } catch (Exception var3) {
         Exception e = var3;
         log.error("InitFilter  报错了", e);
      }

   }
}
