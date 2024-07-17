package com.cyanrocks.microService.helper;

public class ReqInfo {
   private String remoteAddr;

   public String getRemoteAddr() {
      return this.remoteAddr;
   }

   public void setRemoteAddr(String remoteAddr) {
      this.remoteAddr = remoteAddr;
   }

   public ReqInfo(String remoteAddr) {
      this.remoteAddr = remoteAddr;
   }

   public ReqInfo() {
   }
}
