package com.cr.microService.helper;

import javax.websocket.Session;

public class ClientInfo implements Comparable<ClientInfo> {
   private ClientApi clientApi;
   private Session session;

   public ClientApi getClientApi() {
      return this.clientApi;
   }

   public void setClientApi(ClientApi clientApi) {
      this.clientApi = clientApi;
   }

   public Session getSession() {
      return this.session;
   }

   public ClientInfo(ClientApi clientApi, Session session) {
      this.clientApi = clientApi;
      this.session = session;
   }

   public void setSession(Session session) {
      this.session = session;
   }

   public ClientInfo(ClientApi clientApi) {
      this.clientApi = clientApi;
   }

   public int hashCode() {
      boolean prime = true;
      int result = 1;
      result = 31 * result + (this.clientApi == null ? 0 : this.clientApi.hashCode());
      return result;
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         ClientInfo other = (ClientInfo)obj;
         if (this.clientApi == null) {
            if (other.clientApi != null) {
               return false;
            }
         } else if (!this.clientApi.equals(other.clientApi)) {
            return false;
         }

         return true;
      }
   }

   public ClientInfo() {
   }

   public int compareTo(ClientInfo o) {
      return this.clientApi.compareTo(o.clientApi);
   }
}
