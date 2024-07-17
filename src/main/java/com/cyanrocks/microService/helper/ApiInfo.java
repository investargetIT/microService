package com.cyanrocks.microService.helper;

import java.util.Objects;
import org.springframework.web.bind.annotation.RequestMethod;

public class ApiInfo {
   private String name;
   private RequestMethod method;

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public ApiInfo(String name) {
      this.name = name;
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.method, this.name});
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         ApiInfo other = (ApiInfo)obj;
         return this.method == other.method && Objects.equals(this.name, other.name);
      }
   }

   public RequestMethod getMethod() {
      return this.method;
   }

   public ApiInfo() {
   }

   public void setMethod(RequestMethod method) {
      this.method = method;
   }
}
