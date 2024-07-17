package com.cr.microService.helper;

public class ApiClientInfo {
   private String srcIp;
   private String userAgent;
   private String apiRequestTraceId;

   public String getSrcIp() {
      return this.srcIp;
   }

   public void setSrcIp(String srcIp) {
      this.srcIp = srcIp;
   }

   public String getUserAgent() {
      return this.userAgent;
   }

   public ApiClientInfo(String srcIp, String userAgent) {
      this.srcIp = srcIp;
      this.userAgent = userAgent;
   }

   public ApiClientInfo() {
   }

   public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
   }

   public String getApiRequestTraceId() {
      return this.apiRequestTraceId;
   }

   public void setApiRequestTraceId(String apiRequestTraceId) {
      this.apiRequestTraceId = apiRequestTraceId;
   }
}
