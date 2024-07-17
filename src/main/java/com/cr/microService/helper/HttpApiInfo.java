package com.cr.microService.helper;

public class HttpApiInfo {
   private String scheme = "http";
   private String host;
   private Integer port = 8080;
   private String contextPath;
   private Boolean isOnline = true;

   public String getScheme() {
      return this.scheme;
   }

   public int hashCode() {
      boolean prime = true;
      int result = 1;
      result = 31 * result + (this.contextPath == null ? 0 : this.contextPath.hashCode());
      result = 31 * result + (this.host == null ? 0 : this.host.hashCode());
      result = 31 * result + (this.port == null ? 0 : this.port.hashCode());
      result = 31 * result + (this.scheme == null ? 0 : this.scheme.hashCode());
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
         HttpApiInfo other = (HttpApiInfo)obj;
         if (this.contextPath == null) {
            if (other.contextPath != null) {
               return false;
            }
         } else if (!this.contextPath.equals(other.contextPath)) {
            return false;
         }

         if (this.host == null) {
            if (other.host != null) {
               return false;
            }
         } else if (!this.host.equals(other.host)) {
            return false;
         }

         if (this.port == null) {
            if (other.port != null) {
               return false;
            }
         } else if (!this.port.equals(other.port)) {
            return false;
         }

         if (this.scheme == null) {
            if (other.scheme != null) {
               return false;
            }
         } else if (!this.scheme.equals(other.scheme)) {
            return false;
         }

         return true;
      }
   }

   public HttpApiInfo(String host, Integer port) {
      this.host = host;
      this.port = port;
   }

   public void setScheme(String scheme) {
      this.scheme = scheme;
   }

   public Boolean getIsOnline() {
      return this.isOnline;
   }

   public void setIsOnline(Boolean isOnline) {
      this.isOnline = isOnline;
   }

   public HttpApiInfo(String contextPath, Boolean isOnline) {
      this.contextPath = contextPath;
      this.isOnline = isOnline;
   }

   public HttpApiInfo(String scheme, String host, Integer port, String contextPath) {
      this.scheme = scheme;
      this.host = host;
      this.port = port;
      this.contextPath = contextPath;
   }

   public HttpApiInfo(String contextPath) {
      this.contextPath = contextPath;
   }

   public HttpApiInfo(String host, Integer port, String contextPath) {
      this.host = host;
      this.port = port;
      this.contextPath = contextPath;
   }

   public String getHost() {
      return this.host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public Integer getPort() {
      return this.port;
   }

   public void setPort(Integer port) {
      this.port = port;
   }

   public HttpApiInfo() {
   }

   public String getContextPath() {
      return this.contextPath;
   }

   public String toString() {
      return "HttpApiInfo [scheme=" + this.scheme + ", host=" + this.host + ", port=" + this.port + ", contextPath=" + this.contextPath + "]";
   }

   public void setContextPath(String contextPath) {
      this.contextPath = contextPath;
   }

   public String getBaseUrl() {
      boolean b = this.scheme.equalsIgnoreCase("http") && this.port == 80 || this.scheme.equalsIgnoreCase("https") && this.port == 443;
      return String.format("%s://%s%s%s%s", this.scheme, this.host, b ? "" : ":", b ? "" : this.port, this.contextPath);
   }
}
