package com.cr.microService.helper;

import java.util.Objects;
import javax.websocket.Session;

public class WsClientInfo {
   private Session session;
   private boolean alive = true;
   private String baseUrl;

   public Session getSession() {
      return this.session;
   }

   public void setSession(Session session) {
      this.session = session;
   }

   public boolean isAlive() {
      return this.alive;
   }

   public void setAlive(boolean alive) {
      this.alive = alive;
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.baseUrl});
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         WsClientInfo other = (WsClientInfo)obj;
         return Objects.equals(this.baseUrl, other.baseUrl);
      }
   }

   public String getBaseUrl() {
      return this.baseUrl;
   }

   public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
   }

   public WsClientInfo(Session session, String baseUrl) {
      this.session = session;
      this.baseUrl = baseUrl;
   }

   public WsClientInfo() {
   }
}
