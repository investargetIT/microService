package com.cyanrocks.microService;

import com.cyanrocks.microService.web.MsInitFilter;
import com.cyanrocks.microService.web.MsPortApplicationListener;
import com.cyanrocks.microService.web.MyTraceRequestListener;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class MicroServiceConfiguration {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   @Bean
   @ConditionalOnMissingBean
   @ConditionalOnClass(
      name = {"org.springframework.boot.web.context.WebServerInitializedEvent"}
   )
   public MsPortApplicationListener msPortApplicationListener() {
      return new MsPortApplicationListener();
   }

   @Bean
   @ConditionalOnMissingBean
   public MsInitFilter msInitFilter() {
      return new MsInitFilter();
   }

   @Bean
   @ConditionalOnMissingBean
   public MyTraceRequestListener myTraceRequestListener() {
      return new MyTraceRequestListener();
   }

   @Bean
   @ConditionalOnMissingBean
   @ConditionalOnProperty(
      matchIfMissing = true,
      havingValue = "true",
      name = {"enable.api.cors"}
   )
   public CorsFilter corsFilter() {
      CorsConfiguration config = new CorsConfiguration();
      config.addAllowedOriginPattern("*");
      config.setAllowCredentials(true);
      config.addAllowedMethod("*");
      config.addAllowedHeader("*");
      config.addExposedHeader("Location");
      config.setExposedHeaders(Arrays.asList("JSESSIONID", "SESSION", "token", "Location", "Accept", "Access-Control-Allow-Headers", "Access-Control-Expose-Headers", "Access-Control-Allow-Origin", "Cookie", "Set-Cookie", "Set-Cookie2"));
      UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
      configSource.registerCorsConfiguration("/**", config);
      log.info("CorsFilter init end。。。。");
      return new CorsFilter(configSource);
   }
}
