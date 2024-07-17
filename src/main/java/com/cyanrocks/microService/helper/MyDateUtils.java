package com.cyanrocks.microService.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class MyDateUtils {
   public static DateTimeFormatter DTF_YMDHMS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
   public static DateTimeFormatter DTF_YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

   public static String getDate(Date d) {
      return d != null ? d.toInstant().atZone(ZoneId.systemDefault()).format(DTF_YMDHMS) : "";
   }

   public static Date getDate(String s) {
      if (s != null && s.trim().length() > 1) {
         return s.trim().split("\\s+").length == 2 ? Date.from(LocalDateTime.parse(s, DTF_YMDHMS).atZone(ZoneId.systemDefault()).toInstant()) : Date.from(LocalDate.parse(s, DTF_YMD).atStartOfDay(ZoneId.systemDefault()).toInstant());
      } else {
         return null;
      }
   }
}
