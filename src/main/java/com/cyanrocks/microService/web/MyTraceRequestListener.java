package com.cyanrocks.microService.web;

import com.cyanrocks.microService.helper.ApiClientInfo;
import com.cyanrocks.microService.helper.ApiTokenInfo;
import com.cyanrocks.microService.helper.CoordinateUtil;
import com.cyanrocks.microService.helper.Hc;
import java.lang.invoke.MethodHandles;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;

public class MyTraceRequestListener implements ServletRequestListener {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   @Value("${api.token.name:token}")
   private String tokenName;
   @Value("${api.token.transmit:true}")
   private String tokenTransmit;
   public static final String MYAPITRACEID = "My-Api-TraceId";
   public static final String S_P = "P";
   private static final ThreadLocal<ApiTokenInfo> GLOBAL_TOKEN = new ThreadLocal<ApiTokenInfo>() {
      protected ApiTokenInfo initialValue() {
         return null;
      }
   };
   private static final ThreadLocal<ApiClientInfo> GLOBAL_CLIENT = new ThreadLocal<ApiClientInfo>() {
      protected ApiClientInfo initialValue() {
         return null;
      }
   };

   public void requestInitialized(ServletRequestEvent sre) {
      this.addTraceId(sre);
   }

   private void addTraceId(ServletRequestEvent sre) {
      HttpServletRequest req = (HttpServletRequest)sre.getServletRequest();
      ApiClientInfo ac = new ApiClientInfo(getUserIp(req), req.getHeader("user-agent"));
      if (CoordinateUtil.enableapiaotuscan()) {
         this.setTraceId(req, ac);
      }

      GLOBAL_CLIENT.set(ac);
      this.storageToken(req);
   }

   private void setTraceId(HttpServletRequest req, ApiClientInfo ac) {
      String traceId = req.getHeader("My-Api-TraceId");
      int localPort = req.getLocalPort();
      if (traceId == null) {
         traceId = String.format("%s", localPort);
      } else {
         if (traceId.length() > 500) {
            log.error("TraceId is overflow,{},{}", traceId, req.getRequestURI());
            throw new IllegalStateException(req.getRequestURI() + " TraceId is overflow :" + traceId);
         }

         traceId = String.format("%s%s%s", traceId, "P", localPort);
      }

      ac.setApiRequestTraceId(traceId);
   }

   public void requestDestroyed(ServletRequestEvent sre) {
      clientClean();
      removeToken();
   }

   public static String getUserIp(HttpServletRequest req) {
      String clientIp = req.getHeader("X-Real-IP");
      return clientIp == null ? req.getRemoteAddr() : clientIp;
   }

   private void storageToken(HttpServletRequest req) {
      if (Boolean.TRUE.toString().equalsIgnoreCase(this.tokenTransmit)) {
         String token = Hc.getAccessTokenValue(req, this.tokenName);
         if (!ObjectUtils.isEmpty(token)) {
            GLOBAL_TOKEN.set(new ApiTokenInfo(this.tokenName, token));
         }
      }

   }

   public static final ApiTokenInfo getApiToken() {
      return (ApiTokenInfo)GLOBAL_TOKEN.get();
   }

   public static final ApiClientInfo getApiClient() {
      return (ApiClientInfo)GLOBAL_CLIENT.get();
   }

   public static final void clientClean() {
      GLOBAL_CLIENT.remove();
   }

   public static void removeToken() {
      GLOBAL_TOKEN.remove();
   }
}
