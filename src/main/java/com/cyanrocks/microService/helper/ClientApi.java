package com.cyanrocks.microService.helper;

import java.util.HashSet;
import java.util.Set;

public class ClientApi implements Comparable<ClientApi> {
   private Set<ApiInfo> apis = new HashSet();
   private HttpApiInfo httpApiInfo;
   private Boolean sync = false;

   public Set<ApiInfo> getApis() {
      return this.apis;
   }

   public Boolean getSync() {
      return this.sync;
   }

   public ClientApi(Set<ApiInfo> apis, Boolean sync) {
      this.apis = apis;
      this.sync = sync;
   }

   public void setSync(Boolean sync) {
      this.sync = sync;
   }

   public int hashCode() {
      boolean prime = true;
      int result = 1;
      result = 31 * result + (this.httpApiInfo == null ? 0 : this.httpApiInfo.hashCode());
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
         ClientApi other = (ClientApi)obj;
         if (this.httpApiInfo == null) {
            if (other.httpApiInfo != null) {
               return false;
            }
         } else if (!this.httpApiInfo.equals(other.httpApiInfo)) {
            return false;
         }

         return true;
      }
   }

   public void setApis(Set<ApiInfo> apis) {
      this.apis = apis;
   }

   public HttpApiInfo getHttpApiInfo() {
      return this.httpApiInfo;
   }

   public String toString() {
      return "ClientApi [apis=" + this.apis + ", httpApiInfo=" + this.httpApiInfo + "]";
   }

   public ClientApi() {
   }

   public ClientApi(Set<ApiInfo> apis, HttpApiInfo httpApiInfo) {
      this.apis = apis;
      this.httpApiInfo = httpApiInfo;
   }

   public void setHttpApiInfo(HttpApiInfo httpApiInfo) {
      this.httpApiInfo = httpApiInfo;
   }

   public int compareTo(ClientApi o) {
      int c = this.httpApiInfo.getContextPath().compareTo(o.httpApiInfo.getContextPath());
      int h = this.httpApiInfo.getHost().compareTo(o.httpApiInfo.getHost());
      int p = this.httpApiInfo.getPort().compareTo(o.httpApiInfo.getPort());
      int s = this.httpApiInfo.getScheme().compareTo(o.httpApiInfo.getScheme());
      if (c == 0) {
         if (h == 0) {
            return p == 0 ? s : p;
         } else {
            return h;
         }
      } else {
         return c;
      }
   }
}
