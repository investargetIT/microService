package com.cr.microService.helper;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyJsonUtils {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   public static final ObjectMapper OM = new ObjectMapper();

   public static <T> T getObject(String json, Class<T> clazz) {
      try {
         return OM.readValue(json, clazz);
      } catch (Exception var3) {
         Exception e = var3;
         e.printStackTrace();
         log.error("MyJsonUtils:{}", e);
         return null;
      }
   }

   public static <T> T getObject(String json, TypeReference<T> trf) {
      try {
         return OM.readValue(json, trf);
      } catch (Exception var3) {
         Exception e = var3;
         e.printStackTrace();
         log.error("MyJsonUtils:{}", e);
         return null;
      }
   }

   public static <T> List<T> getList(String json, TypeReference<List<T>> trf) {
      try {
         return (List)OM.readValue(json, trf);
      } catch (Exception var3) {
         Exception e = var3;
         e.printStackTrace();
         log.error("MyJsonUtils:{}", e);
         return null;
      }
   }

   public static String getJsonString(Object o) {
      try {
         if (o != null) {
            return OM.writeValueAsString(o);
         }

         log.error("getJsonString obj is null ");
      } catch (Exception var2) {
         Exception e = var2;
         e.printStackTrace();
         log.error("MyJsonUtils:{}", e);
      }

      return "{}";
   }

   static {
      OM.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
      OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      OM.setSerializationInclusion(Include.NON_NULL);
   }
}
