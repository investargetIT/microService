package com.cr.microService.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.cr.microService.code.RestCode;
import com.cr.microService.web.RdsWsClient;
import com.cr.myhttputils.MyHttp;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.ClientEndpointConfig.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoordinateUtil {
   public static final String SERVER_API = "/restcoordinate";
   public static final String FETCH_API = "/fetchApis";
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   public static volatile HttpApiInfo HAI = new HttpApiInfo();
   public static final ExecutorService STE = Executors.newSingleThreadExecutor();
   public static final String WS_MID_URL = "ws.mid.url";
   public static final String ENABLEAPIAOTUSCAN_KEY = "enable.api.auto.scan";
   public static volatile ConcurrentSkipListSet<ClientInfo> CLIENTS = new ConcurrentSkipListSet();
   public static volatile ConcurrentSkipListSet<ClientApi> CAS = new ConcurrentSkipListSet();
   private static ResourceBundle ACURL = null;
   public static volatile Set<ApiInfo> LOCAL_APIS;
   public static volatile String WS_REMOTE_URL;
   public static volatile boolean AUTO_SCAN;
   public static volatile int PORT;
   static String warRoot;
   static String jarRoot;
   public static final String USER_DIR;

   public static HttpApiInfo getHttpApiInfo(Session session) {
      Optional<HttpApiInfo> opt = CLIENTS.stream().filter((o) -> {
         return o.getSession().getId().equals(session.getId());
      }).map((o) -> {
         return o.getClientApi().getHttpApiInfo();
      }).findAny();
      return (HttpApiInfo)opt.orElse((HttpApiInfo) null);
   }

   private static boolean varyname(Set<ApiInfo> varys, ApiInfo api) {
      Iterator var2 = varys.iterator();

      while(var2.hasNext()) {
         ApiInfo ai = (ApiInfo)var2.next();
         String vary = ai.getName();
         String name = api.getName();
         if (vary.contains("/{") && name.contains("/") && vary.trim().endsWith("}")) {
            boolean b = vary.substring(0, vary.lastIndexOf("/")).equals(name.substring(0, name.lastIndexOf("/")));
            if (b) {
               return true;
            }
         }
      }

      return false;
   }

   public static Set<ClientApi> getCasByApiInfo(ApiInfo apiInfo) {
      return getCasByApiInfo(apiInfo, CAS);
   }

   public static Set<ClientApi> getCasByApiInfo(ApiInfo apiInfo, Set<ClientApi> cas) {
      Set<ClientApi> collect = (Set)cas.stream().filter((o) -> {
         return o.getApis().stream().anyMatch((a) -> {
            return a.getName().equals(apiInfo.getName());
         });
      }).collect(Collectors.toSet());
      if (collect.size() < 1) {
         collect = (Set)cas.stream().filter((o) -> {
            return varyname(o.getApis(), apiInfo);
         }).collect(Collectors.toSet());
      }

      return collect;
   }

   public static Map<String, HttpApiInfo> getApiUrl(String name, Set<HttpApiInfo> has) {
      HttpApiInfo url = getHttpApiInfo(getApiInfo(name), has);
      return getCurUrl(name, url);
   }

   public static Map<String, HttpApiInfo> getCurUrl(String name, HttpApiInfo url) {
      String key = getRealUrl(name, url);
      Map<String, HttpApiInfo> rt = new HashMap(1);
      rt.put(key, url);
      return rt;
   }

   public static String getRealUrl(String name, HttpApiInfo url) {
      String apiurl = url.getBaseUrl();
      if (!apiurl.endsWith("/")) {
         apiurl = apiurl + "/";
      }

      if (name.startsWith("/")) {
         name = name.replaceFirst("/", "");
      }

      String key = apiurl + name;
      return key;
   }

   public static ApiInfo getApiInfo(String name) {
      return new ApiInfo(ClassHelper.getApiName(name.split("[?;]")[0]));
   }

   public static void connCoor(String contextPath) {
      HAI.setContextPath(contextPath);
      if (WS_REMOTE_URL != null) {
         setScheme(ACURL);
         if (enableapiaotuscan()) {
            LOCAL_APIS.addAll(getapis());
         } else {
            log.info("enable.api.auto.scan:false");
         }

         if (WS_REMOTE_URL != null && WS_REMOTE_URL.trim().length() > 1) {
            RdsWsClient.STPOOL.submit(() -> {
               HAI.setPort(getApiPort());
               String apiHost = getApiHost();
               if (apiHost != null) {
                  String[] address = apiHost.trim().split(":");
                  HAI.setHost(address[0]);
                  if (address.length == 2) {
                     HAI.setPort(Integer.valueOf(address[1]));
                  }
               }

               if (HAI.getContextPath() != null) {
                  if (HAI.getPort() < 1) {
                     log.error("服务注册失败了，请指定微服务server.port端口,重新注册。");
                  } else if (!connectHbWs()) {
                     try {
                        TimeUnit.SECONDS.sleep(125L);
                        connectHbWs();
                     } catch (InterruptedException var2) {
                        InterruptedException e = var2;
                        log.error("", e);
                     }
                  }
               } else {
                  log.error("ContextPath is null ");
               }

            });
         }
      }

      try {
         TimeUnit.SECONDS.sleep(3L);
      } catch (InterruptedException var2) {
         InterruptedException e = var2;
         e.printStackTrace();
      }

   }

   public static boolean enableapiaotuscan() {
      String systemEnvValue = getSystemEnvValue("enable.api.auto.scan");
      if (systemEnvValue != null) {
         if (systemEnvValue.trim().equalsIgnoreCase("false")) {
            return false;
         }
      } else if (ACURL != null && ACURL.containsKey("enable.api.auto.scan")) {
         String scan = ACURL.getString("enable.api.auto.scan").trim();
         scan = getVariableValue(scan);
         if (scan.equalsIgnoreCase("false")) {
            return false;
         }
      }

      return AUTO_SCAN;
   }

   public static String getSystemEnvValue(String key) {
      String v = System.getProperty(key);
      return v != null ? v : System.getenv(key);
   }

   private static void setScheme(ResourceBundle acurl) {
      String key = "server.ssl.enabled";
      String systemEnvValue = getSystemEnvValue(key);
      if (systemEnvValue == null && acurl != null && acurl.containsKey(key)) {
         systemEnvValue = acurl.getString(key);
         systemEnvValue = getVariableValue(systemEnvValue);
      }

      if (systemEnvValue != null && "true".equalsIgnoreCase(systemEnvValue)) {
         HAI.setScheme("https");
      }

   }

   private static String getApiHost() {
      String key = "server.host";
      String host = getSystemEnvValue(key);
      if (host != null) {
         return host.trim();
      } else {
         String kv = ClassHelper.getDefaultClassLoader().getResource("acurl.properties") != null && ResourceBundle.getBundle("acurl").containsKey(key) ? ResourceBundle.getBundle("acurl").getString(key) : (ClassHelper.getDefaultClassLoader().getResource("application.properties") != null && ResourceBundle.getBundle("application").containsKey(key) ? ResourceBundle.getBundle("application").getString(key) : null);
         return getVariableValue(kv);
      }
   }

   public static int getPuaseTime() {
      return ThreadLocalRandom.current().nextInt(160) + 60;
   }

   public static synchronized void reconnectremote() {
      while(true) {
         try {
            if (!connectHbWs()) {
               TimeUnit.SECONDS.sleep((long)getPuaseTime());
               continue;
            }
         } catch (Exception var1) {
            Exception e = var1;
            e.printStackTrace();
            log.error("sleep error", e);
         }

         return;
      }
   }

   private static boolean isOpen(String url) {
      WsClientInfo wsc = (WsClientInfo)RdsWsClient.WS_CLIENTS.stream().filter((s) -> {
         return s.getBaseUrl().equals(url);
      }).findAny().orElse((WsClientInfo) null);
      return wsc != null && wsc.getSession() != null && wsc.getSession().isOpen();
   }

   public static synchronized boolean connectHbWs() {
      boolean r = true;
      String[] var1 = WS_REMOTE_URL.split(",");
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         String url = var1[var3];

         try {
            String serverUrl = contactUrl(url, "/restcoordinate");
            if (!isOpen(serverUrl)) {
               WebSocketContainer wsc = ContainerProvider.getWebSocketContainer();
               ClientEndpointConfig.Builder bd = Builder.create();
               List<Class<? extends Decoder>> decoders = new ArrayList(1);
               decoders.add(RestCode.class);
               bd.decoders(decoders);
               List<Class<? extends Encoder>> encoders = new ArrayList(1);
               encoders.add(RestCode.class);
               bd.encoders(encoders);
               ClientEndpointConfig clientConfig = bd.build();
               clientConfig.getUserProperties().put("wsUrl", serverUrl);
               wsc.connectToServer(RdsWsClient.class, clientConfig, URI.create(serverUrl));
            }
         } catch (Throwable var11) {
            Throwable e = var11;
            log.warn("连接{}服务器失败:{}", url, e.getMessage());
            r = false;
         }
      }

      log.info("connectHbWs:{}", r);
      return r;
   }

   public static String getWsUrl(ResourceBundle acurl) {
      String midurl = getSystemEnvValue("ws.mid.url");
      if (midurl == null && acurl != null && acurl.containsKey("ws.mid.url")) {
         midurl = acurl.getString("ws.mid.url");
         midurl = getVariableValue(midurl);
      }

      if (midurl != null && midurl.length() > 1) {
         return midurl.trim();
      } else {
         log.info("ws.mid.url  is not found!");
         return null;
      }
   }

   public static final String contactUrl(String domain, String path) {
      if (domain != null && path != null) {
         if (path.startsWith("/")) {
            path = path.substring(1);
         }

         String url = String.format("%s%s%s", domain.trim(), domain.trim().endsWith("/") ? "" : "/", path.trim());
         return url;
      } else {
         return "";
      }
   }

   public static String getVariableValue(String c) {
      if (c != null && c.trim().startsWith("${") && c.trim().endsWith("}")) {
         String[] split = c.split(":", 2);
         String key;
         if (split.length == 2) {
            key = split[0].substring(2);
            String kv = getSystemEnvValue(key.trim());
            if (kv == null) {
               String dv = split[1].trim();
               return dv.substring(0, dv.length() - 1);
            } else {
               return kv;
            }
         } else {
            key = c.trim().substring(2, c.length() - 1);
            return getSystemEnvValue(key);
         }
      } else {
         return c;
      }
   }

   private static String getPort() {
      String key = "server.port";
      String port = getSystemEnvValue(key);
      if (port != null) {
         return port.trim();
      } else {
         String kv = ClassHelper.getDefaultClassLoader().getResource("acurl.properties") != null && ResourceBundle.getBundle("acurl").containsKey(key) ? ResourceBundle.getBundle("acurl").getString(key) : (ClassHelper.getDefaultClassLoader().getResource("application.properties") != null && ResourceBundle.getBundle("application").containsKey(key) ? ResourceBundle.getBundle("application").getString(key) : "");
         return getVariableValue(kv);
      }
   }

   private static int getApiPort() {
      String port = getPort();
      if (port != null && port.trim().length() > 0) {
         Integer lp = Integer.valueOf(port.trim());
         return lp == 0 ? getRandomPort() : lp;
      } else {
         return getRandomPort();
      }
   }

   private static int getRandomPort() {
      for(int i = 0; i < 120; ++i) {
         if (PORT > 0) {
            return PORT;
         }

         try {
            TimeUnit.MILLISECONDS.sleep(200L);
         } catch (InterruptedException var2) {
            InterruptedException e = var2;
            e.printStackTrace();
         }
      }

      return 8080;
   }

   private static Set<ApiInfo> getapis() {
      Set<ApiInfo> apis = new HashSet();

      try {
         String[] classpaths = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
         String clp = classpaths[0];
         if (classpaths.length == 1) {
            ClassHelper.getjarpms(apis, Paths.get(clp), clp.endsWith(".war") ? warRoot : jarRoot);
         } else {
            Path clps = Paths.get(MethodHandles.lookup().lookupClass().getResource("/").toURI());
            if (Files.exists(clps, new LinkOption[0]) && Files.isDirectory(clps, new LinkOption[0])) {
               scandir(apis, clps);
               log.info("clps:{}", clps);
            } else {
               log.info("USER_DIR:{}", USER_DIR);
               String[] var4 = classpaths;
               int var5 = classpaths.length;

               for(int var6 = 0; var6 < var5; ++var6) {
                  String cp = var4[var6];
                  if (cp.startsWith(USER_DIR)) {
                     Path start = Paths.get(cp);
                     if (Files.isDirectory(start, new LinkOption[0])) {
                        scandir(apis, start);
                        log.info("classpath:{}", start);
                     }
                  }
               }
            }
         }
      } catch (Exception var9) {
         Exception e = var9;
         log.error("getapis", e);
      }

      return apis;
   }

   private static void scandir(final Set<ApiInfo> apis, final Path start) throws IOException {
      Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String name = file.getFileName().toString();
            if (name.endsWith(".class") && !name.contains("$")) {
               CoordinateUtil.scanclass(apis, start, file);
            }

            return FileVisitResult.CONTINUE;
         }

         public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
         }
      });
   }

   private static void scanclass(Set<ApiInfo> ais, Path start, Path file) {
      String cpn = file.toUri().toString().split(start.toUri().toString())[1];
      String cln = cpn.substring(0, cpn.lastIndexOf(".")).replace('/', '.');
      ClassHelper.getPms(ais, cln);
   }

   public static HttpApiInfo getHttpApiInfo(ApiInfo ai, Set<HttpApiInfo> collects) {
      if (collects.size() > 0) {
         return ((HttpApiInfo[])collects.toArray(new HttpApiInfo[0]))[ThreadLocalRandom.current().nextInt(collects.size())];
      } else {
         throw new IllegalStateException(String.format("%s服务不可用", ai));
      }
   }

   public static List<HttpApiInfo> gethas(ApiInfo ai) {
      if (CAS.size() > 0) {
         return getHttpApiInfos(ai);
      } else {
         fetchApis();
         return (List)(CAS.size() > 0 ? getHttpApiInfos(ai) : new ArrayList(0));
      }
   }

   private static List<HttpApiInfo> getHttpApiInfos(ApiInfo ai) {
      List<HttpApiInfo> collects = (List)CAS.stream().filter((o) -> {
         return o.getApis().stream().anyMatch((a) -> {
            return a.getName().equals(ai.getName());
         });
      }).map((o) -> {
         return o.getHttpApiInfo();
      }).collect(Collectors.toList());
      if (collects.size() < 1) {
         collects = (List)CAS.stream().filter((o) -> {
            return varyname(o.getApis(), ai);
         }).map((o) -> {
            return o.getHttpApiInfo();
         }).collect(Collectors.toList());
      }

      return collects;
   }

   private static void fetchApis() {
      if (WS_REMOTE_URL != null) {
         String[] var0 = WS_REMOTE_URL.split(",");
         int var1 = var0.length;

         for(int var2 = 0; var2 < var1; ++var2) {
            String url = var0[var2];

            try {
               URI uri = URI.create(url);
               boolean tls = uri.getScheme().equalsIgnoreCase("wss");
               String apiurl = contactUrl(String.format("%s://%s%s", tls ? "https" : "http", uri.getHost(), uri.getPort() > 0 ? ":" + uri.getPort() : ""), "/fetchApis");
               String json = MyHttp.getContent(apiurl, 1000, 3000);
               List<ClientApi> list = MyJsonUtils.getList(json, new TypeReference<List<ClientApi>>() {
               });
               if (list != null) {
                  CAS.addAll(list);
               }
            } catch (Exception var9) {
               Exception e = var9;
               log.warn(String.format("%s:%s", url, e.getMessage()));
            }
         }
      }

   }

   static {
      if (ClassHelper.getDefaultClassLoader().getResource("acurl.properties") != null) {
         ACURL = ResourceBundle.getBundle("acurl");
      } else if (ClassHelper.getDefaultClassLoader().getResource("application.properties") != null) {
         ACURL = ResourceBundle.getBundle("application");
      }

      if (WS_REMOTE_URL == null) {
         WS_REMOTE_URL = getWsUrl(ACURL);
      }

      log.info("ACURL inited .");
      LOCAL_APIS = new HashSet();
      AUTO_SCAN = true;
      PORT = 0;
      warRoot = "/WEB-INF/classes/";
      jarRoot = "/BOOT-INF/classes/";
      USER_DIR = System.getProperty("user.dir");
   }
}
