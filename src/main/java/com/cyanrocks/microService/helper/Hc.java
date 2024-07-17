package com.cyanrocks.microService.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.cyanrocks.microService.web.MyTraceRequestListener;
import com.cyanrocks.microService.web.RdsWsClient;
import com.cr.myhttputils.FileDataInfo;
import com.cr.myhttputils.HttpDataInfo;
import com.cr.myhttputils.MyHttp;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.EncodeException;
import javax.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Hc {
   public static final String CONTENT_TYPE = "Content-Type";
   private static final int C_TIMEOUT = 5000;
   private static final int R_TIMEOUT = 50000;
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   public static String get(String name) {
      return get(name, 5000, 50000);
   }

   public static String call(String name) {
      return call(name, 5000, 50000);
   }

   public static HttpApiInfo getRandomApi(List<HttpApiInfo> hosts) {
      return hosts.size() > 1 ? (HttpApiInfo)hosts.get(ThreadLocalRandom.current().nextInt(hosts.size())) : (HttpApiInfo)hosts.get(0);
   }

   public static String get(String name, int connectTimeout, int readTimeout) {
      return new String(get(connectTimeout, readTimeout, name, "application/json;charset=UTF-8").getBody(), StandardCharsets.UTF_8);
   }

   public static String getIfOnline(String name, int connectTimeout, int readTimeout) {
      if (CoordinateUtil.getCasByApiInfo(CoordinateUtil.getApiInfo(name)).size() > 0) {
         return new String(get(connectTimeout, readTimeout, name, "application/json;charset=UTF-8").getBody(), StandardCharsets.UTF_8);
      } else {
         log.info("call ignored ,{} is not online ", name);
         return null;
      }
   }

   public static HttpDataInfo get(int connectTimeout, int readTimeout, String name, String contentType) {
      return getDataInfo(true, connectTimeout, readTimeout, name, contentType);
   }

   public static HttpDataInfo fetchData(int connectTimeout, int readTimeout, String name, String contentType) {
      return getDataInfo(false, connectTimeout, readTimeout, name, contentType);
   }

   public static String fetchContent(String name, int connectTimeout, int readTimeout) {
      return new String(fetchData(connectTimeout, readTimeout, name, "application/json;charset=UTF-8").getBody(), StandardCharsets.UTF_8);
   }

   private static HttpDataInfo getDataInfo(boolean trans, int connectTimeout, int readTimeout, String name, String contentType) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(name));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(name, ha);

         try {
            return getContentResponse(trans, realurl, connectTimeout, readTimeout, contentType);
         } catch (Exception var13) {
            Exception e = var13;
            if (isTimeout(e, name, ha)) {
               try {
                  return getContentResponse(trans, realurl, connectTimeout, readTimeout, contentType);
               } catch (Exception var12) {
                  Exception e1 = var12;
                  if (isTimeout(e1, name, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("get", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", name.split("[?]")[0]));
   }

   private static HttpDataInfo getContentResponse(boolean trans, String realurl, int connectTimeout, int readTimeout, String contentType) {
      Map<String, String> heads = getInitheads(getContentType(contentType));
      ApiClientInfo cl = MyTraceRequestListener.getApiClient();
      if (cl != null) {
         setTransData(trans, cl, heads);
         return MyHttp.getContent(realurl, heads, connectTimeout, readTimeout);
      } else {
         return MyHttp.getContent(realurl, heads, connectTimeout, readTimeout);
      }
   }

   private static Map<String, String> getContentType(String contentType) {
      HashMap<String, String> heads = new HashMap(2);
      if (contentType != null && contentType.trim().length() > 0) {
         heads.put("Content-Type", contentType);
      } else {
         heads.put("Content-Type", "text/html;charset=UTF-8");
      }

      return heads;
   }

   public static String call(String name, int connectTimeout, int readTimeout) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(name));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(name, ha);

         try {
            return callData(connectTimeout, readTimeout, realurl);
         } catch (Exception var11) {
            Exception e = var11;
            if (isTimeout(e, name, ha)) {
               try {
                  return callData(connectTimeout, readTimeout, realurl);
               } catch (Exception var10) {
                  Exception e1 = var10;
                  if (isTimeout(e1, name, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("get", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", name.split("[?]")[0]));
   }

   private static String callData(int connectTimeout, int readTimeout, String realurl) throws IOException {
      HttpDataInfo req = new HttpDataInfo();
      addHeads(req);
      req.setReadtimeout(readTimeout);
      req.setTimeout(connectTimeout);
      HttpDataInfo res = MyHttp.call(realurl, req);
      return MyHttp.getCallContent(res);
   }

   private static void addHeads(HttpDataInfo req) {
      ApiTokenInfo apiToken = MyTraceRequestListener.getApiToken();
      if (apiToken != null) {
         String name = apiToken.getName();
         Map<String, List<String>> headerFields = req.getHeaderFields();
         if (headerFields != null && headerFields.keySet().stream().noneMatch((k) -> {
            return k.equalsIgnoreCase(name);
         })) {
            headerFields.put(name, Arrays.asList(apiToken.getValue()));
         }
      }

   }

   public static <T> T getObj(String name, TypeReference<T> trf) {
      return MyJsonUtils.getObject(get(name), trf);
   }

   public static String getAccessTokenValue(HttpServletRequest req, String tokenName) {
      String token = req.getParameter(tokenName);
      if (token != null && token.trim().length() > 0) {
         return token;
      } else {
         Cookie[] cks = req.getCookies();
         if (cks != null) {
            Cookie[] var4 = cks;
            int var5 = cks.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               Cookie ck = var4[var6];
               if (ck.getName().equals(tokenName) && ck.getValue() != null) {
                  return ck.getValue();
               }
            }
         }

         return req.getHeader(tokenName);
      }
   }

   public static String getUrlByName(String name, Set<HttpApiInfo> has) {
      Map<String, HttpApiInfo> apiUrl = CoordinateUtil.getApiUrl(name, has);
      String next = (String)apiUrl.keySet().iterator().next();
      return next;
   }

   public static String upload(String contentType, String apiname, byte[] data, String inputName, String filename) {
      return uploadfile(contentType, apiname, data, inputName, filename, 5000, 50000);
   }

   public static String upload(String contentType, String apiname, byte[] data, String inputName, String filename, boolean forward) {
      return uploadfile(contentType, apiname, data, inputName, filename, 5000, 50000, forward);
   }

   public static String postMultipartFormData(byte[] filedata, String apiname, String inputName, String filename, String contentType, Map<String, String[]> inputs) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(apiname));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(apiname, ha);

         try {
            return postMultpart(filedata, inputName, filename, contentType, inputs, realurl);
         } catch (Exception var14) {
            Exception e = var14;
            if (isTimeout(e, apiname, ha)) {
               try {
                  return postMultpart(filedata, inputName, filename, contentType, inputs, realurl);
               } catch (Exception var13) {
                  Exception e1 = var13;
                  if (isTimeout(e1, apiname, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("upload", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", apiname.split("[?]")[0]));
   }

   public static String postMultipartFormData(String apiname, List<FileDataInfo> files, Map<String, String[]> inputs) {
      return postMultipartFormData(apiname, files, inputs, true);
   }

   public static String postMultipartFormData(String apiname, List<FileDataInfo> files, Map<String, String[]> inputs, boolean forward) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(apiname));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(apiname, ha);

         try {
            return postMultpart(files, inputs, realurl, forward);
         } catch (Exception var12) {
            Exception e = var12;
            if (isTimeout(e, apiname, ha)) {
               try {
                  return postMultpart(files, inputs, realurl, forward);
               } catch (Exception var11) {
                  Exception e1 = var11;
                  if (isTimeout(e1, apiname, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("upload", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", apiname.split("[?]")[0]));
   }

   private static String postMultpart(byte[] filedata, String inputName, String filename, String contentType, Map<String, String[]> inputs, String realurl) {
      ApiClientInfo cl = MyTraceRequestListener.getApiClient();
      Map<String, String> heads = new HashMap(4);
      heads.putAll(initTokenHeads());
      if (cl != null) {
         setTransData(true, cl, heads);
         return MyHttp.postMultipartFormData(filedata, realurl, inputName, filename, contentType, inputs, heads);
      } else {
         return MyHttp.postMultipartFormData(filedata, realurl, inputName, filename, contentType, inputs, heads);
      }
   }

   private static String postMultpart(List<FileDataInfo> files, Map<String, String[]> inputs, String realurl, boolean trans) {
      ApiClientInfo cl = MyTraceRequestListener.getApiClient();
      Map<String, String> heads = new HashMap(4);
      heads.putAll(initTokenHeads());
      if (cl != null) {
         setTransData(trans, cl, heads);
         return MyHttp.postMultipartFormData(realurl, files, inputs, heads);
      } else {
         return MyHttp.postMultipartFormData(realurl, files, inputs, heads);
      }
   }

   public static String uploadfile(String contentType, String apiname, byte[] data, String inputName, String filename, int timeout, int readtimeout) {
      return uploadfile(contentType, apiname, data, inputName, filename, timeout, readtimeout, true);
   }

   public static String uploadfile(String contentType, String apiname, byte[] data, String inputName, String filename, int timeout, int readtimeout, boolean forward) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(apiname));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(apiname, ha);

         try {
            return uploadfile(contentType, data, inputName, filename, timeout, readtimeout, realurl, forward);
         } catch (Exception var16) {
            Exception e = var16;
            if (isTimeout(e, apiname, ha)) {
               try {
                  return uploadfile(contentType, data, inputName, filename, timeout, readtimeout, realurl, forward);
               } catch (Exception var15) {
                  Exception e1 = var15;
                  if (isTimeout(e1, apiname, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("upload", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", apiname.split("[?]")[0]));
   }

   private static String uploadfile(String contentType, byte[] data, String inputName, String filename, int timeout, int readtimeout, String realurl, boolean trans) {
      ApiClientInfo cl = MyTraceRequestListener.getApiClient();
      Map<String, String> heads = new HashMap(4);
      heads.putAll(initTokenHeads());
      if (cl != null) {
         setTransData(trans, cl, heads);
         return MyHttp.uploadFile(data, realurl, inputName, filename, contentType, timeout, readtimeout, heads);
      } else {
         return MyHttp.uploadFile(data, realurl, inputName, filename, contentType, timeout, readtimeout, heads);
      }
   }

   private static void setTransData(boolean trans, ApiClientInfo cl, Map<String, String> heads) {
      if (trans) {
         heads.put("X-Real-IP", cl.getSrcIp());
         heads.put("User-Agent", cl.getUserAgent());
      }

      if (cl != null) {
         heads.put("My-Api-TraceId", cl.getApiRequestTraceId());
      }

   }

   public static byte[] getData(String name) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(name));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(name, ha);
         Map<String, String> heads = new HashMap(4);
         heads.putAll(initTokenHeads());

         try {
            return MyHttp.getData(heads, realurl);
         } catch (Exception var10) {
            Exception e = var10;
            if (isTimeout(e, name, ha)) {
               try {
                  return MyHttp.getData(heads, realurl);
               } catch (Exception var9) {
                  Exception e1 = var9;
                  if (isTimeout(e1, name, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("getData", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", name.split("[?]")[0]));
   }

   public static String postBody(String name, String jsonReqData) {
      return postBodyData(name, jsonReqData, (String)null, 5000, 50000);
   }

   public static String postBody(String name, String jsonReqData, int connectTimeout, int readTimeout) {
      return postBodyData(name, jsonReqData, (String)null, connectTimeout, readTimeout);
   }

   public static HttpDataInfo postBody(int connectTimeout, int readTimeout, String name, String jsonReqData) {
      return postBodyData(connectTimeout, readTimeout, name, jsonReqData, getContentType("application/json;charset=UTF-8"));
   }

   public static String postBody(String name, String jsonReqData, String contentType) {
      return postBodyData(name, jsonReqData, contentType, 5000, 50000);
   }

   public static String postBody(String name, String jsonReqData, String contentType, int connectTimeout, int readTimeout) {
      return postBodyData(name, jsonReqData, contentType, connectTimeout, readTimeout);
   }

   public static HttpDataInfo postBody(String name, String jsonReqData, int connectTimeout, int readTimeout, String contentType) {
      return postBodyData(connectTimeout, readTimeout, name, jsonReqData, getInitheads(getContentType(contentType)));
   }

   public static String postJsonBody(String name, String jsonReqData) {
      return postBodyData(name, jsonReqData, "application/json; charset=UTF-8", 5000, 50000);
   }

   public static String sendJsonBody(String name, String jsonReqData) {
      return sendBodyData(name, jsonReqData, "application/json; charset=UTF-8", 5000, 50000);
   }

   public static String callJsonBody(String name, String jsonReqData) {
      return callBodyData(name, jsonReqData, "application/json; charset=UTF-8", 5000, 50000);
   }

   public static String postJsonBody(String name, String jsonReqData, int connectTimeout, int readTimeout) {
      return postBodyData(name, jsonReqData, "application/json; charset=UTF-8", connectTimeout, readTimeout);
   }

   public static <T> T postObject(String name, Object obj, TypeReference<T> trf) {
      return MyJsonUtils.getObject(postObject(name, obj), trf);
   }

   public static String postJsonBody(String name, String jsonReqData, String tokenname, String tokenvalue) {
      Map<String, String> initheads = getInitheads(getContentType("application/json;charset=UTF-8"));
      initheads.put(tokenname, tokenvalue);
      return new String(getPostData(name, jsonReqData, initheads, 5000, 50000).getBody(), StandardCharsets.UTF_8);
   }

   public static String postObject(String name, Object obj, String tokenname, String tokenvalue) {
      String jsonReqData = null;
      if (obj != null) {
         if (obj.getClass() == String.class) {
            jsonReqData = obj.toString();
         } else {
            jsonReqData = MyJsonUtils.getJsonString(obj);
         }
      }

      return postJsonBody(name, jsonReqData, tokenname, tokenvalue);
   }

   public static <T> T postObject(String name, Object obj, String tokenname, String tokenvalue, TypeReference<T> trf) {
      return MyJsonUtils.getObject(postObject(name, obj, tokenname, tokenvalue), trf);
   }

   public static <T> T sendObject(String name, Object obj, TypeReference<T> trf) {
      return MyJsonUtils.getObject(sendObject(name, obj), trf);
   }

   public static String postObject(String name, Object obj) {
      if (obj != null) {
         return obj.getClass() == String.class ? postJsonBody(name, obj.toString()) : postJsonBody(name, MyJsonUtils.getJsonString(obj));
      } else {
         return postJsonBody(name, (String)null);
      }
   }

   public static String sendObject(String name, Object obj) {
      if (obj != null) {
         return obj.getClass() == String.class ? sendJsonBody(name, obj.toString()) : sendJsonBody(name, MyJsonUtils.getJsonString(obj));
      } else {
         return sendJsonBody(name, (String)null);
      }
   }

   public static String callObject(String name, Object obj) {
      if (obj != null) {
         return obj.getClass() == String.class ? callJsonBody(name, obj.toString()) : callJsonBody(name, MyJsonUtils.getJsonString(obj));
      } else {
         return callJsonBody(name, (String)null);
      }
   }

   public static byte[] postJsonData(String name, String jsonReqData) {
      return postGetBodyData(name, jsonReqData, "application/json; charset=UTF-8").getBody();
   }

   public static HttpDataInfo postJsonDataAndDownload(String name, String jsonReqData) {
      return postGetBodyData(name, jsonReqData, "application/json; charset=UTF-8");
   }

   public static String callBodyData(String name, String jsonReqData, String contentType, int connectTimeout, int readTimeout) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(name));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(name, ha);

         try {
            return callcontent(jsonReqData, contentType, connectTimeout, readTimeout, realurl);
         } catch (Exception var13) {
            Exception e = var13;
            if (isTimeout(e, name, ha)) {
               try {
                  return callcontent(jsonReqData, contentType, connectTimeout, readTimeout, realurl);
               } catch (Exception var12) {
                  Exception e1 = var12;
                  if (isTimeout(e1, name, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("postBodyData", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", name.split("[?]")[0]));
   }

   private static String callcontent(String jsonReqData, String contentType, int connectTimeout, int readTimeout, String realurl) throws IOException {
      HttpDataInfo req = new HttpDataInfo();
      addHeads(req);
      req.setBody(jsonReqData.getBytes(StandardCharsets.UTF_8));
      req.setContentType(contentType);
      req.setTimeout(connectTimeout);
      req.setReadtimeout(readTimeout);
      HttpDataInfo res = MyHttp.call(realurl, req);
      return MyHttp.getCallContent(res);
   }

   public static String postBodyData(String name, String jsonReqData, String contentType, int connectTimeout, int readTimeout) {
      return new String(postBodyData(connectTimeout, readTimeout, name, jsonReqData, getInitheads(getContentType(contentType))).getBody(), StandardCharsets.UTF_8);
   }

   public static HttpDataInfo postBodyData(int connectTimeout, int readTimeout, String name, String jsonReqData, Map<String, String> heads) {
      return getPostData(name, jsonReqData, getInitheads(heads), connectTimeout, readTimeout);
   }

   public static String postData(String name, String jsonReqData, Map<String, String> heads, int connectTimeout, int readTimeout) {
      return new String(getPostData(name, jsonReqData, getInitheads(heads), connectTimeout, readTimeout).getBody(), StandardCharsets.UTF_8);
   }

   private static HttpDataInfo getPostData(String name, String jsonReqData, Map<String, String> heads, int connectTimeout, int readTimeout) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(name));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(name, ha);

         try {
            return getPostResponse(true, jsonReqData, heads, connectTimeout, readTimeout, realurl);
         } catch (Exception var13) {
            Exception e = var13;
            if (isTimeout(e, name, ha)) {
               try {
                  return getPostResponse(true, jsonReqData, heads, connectTimeout, readTimeout, realurl);
               } catch (Exception var12) {
                  Exception e1 = var12;
                  if (isTimeout(e1, name, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("postBodyData", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", name.split("[?]")[0]));
   }

   public static String sendBodyData(String name, String jsonReqData, String contentType, int connectTimeout, int readTimeout) {
      Map<String, String> heads = getInitheads(getContentType(contentType));
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(name));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(name, ha);

         try {
            return getPostResponse(jsonReqData, heads, connectTimeout, readTimeout, realurl, false);
         } catch (Exception var14) {
            Exception e = var14;
            if (isTimeout(e, name, ha)) {
               try {
                  return getPostResponse(jsonReqData, heads, connectTimeout, readTimeout, realurl, false);
               } catch (Exception var13) {
                  Exception e1 = var13;
                  if (isTimeout(e1, name, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("postBodyData", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", name.split("[?]")[0]));
   }

   private static String getPostResponse(String jsonReqData, Map<String, String> heads, int connectTimeout, int readTimeout, String realurl, boolean trans) {
      return new String(getPostResponse(trans, jsonReqData, heads, connectTimeout, readTimeout, realurl).getBody(), StandardCharsets.UTF_8);
   }

   private static HttpDataInfo getPostResponse(boolean trans, String jsonReqData, Map<String, String> heads, int connectTimeout, int readTimeout, String realurl) {
      ApiClientInfo cl = MyTraceRequestListener.getApiClient();
      if (cl != null) {
         setTransData(trans, cl, heads);
         return MyHttp.postBody(realurl, heads, jsonReqData, connectTimeout, readTimeout);
      } else {
         return MyHttp.postBody(realurl, heads, jsonReqData, connectTimeout, readTimeout);
      }
   }

   private static Map<String, String> getInitheads(Map<String, String> initheads) {
      Map<String, String> heads = new HashMap(4);
      if (initheads != null) {
         heads.putAll(initheads);
      }

      if (heads.keySet().stream().noneMatch((k) -> {
         return k.equalsIgnoreCase("Content-Type");
      })) {
         heads.put("Content-Type", "text/html;charset=UTF-8");
      }

      Iterator var2 = initTokenHeads().entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry<String, String> en = (Map.Entry)var2.next();
         String tkey = (String)en.getKey();
         if (heads.keySet().stream().noneMatch((k) -> {
            return k.equalsIgnoreCase(tkey);
         })) {
            heads.put(tkey, (String)en.getValue());
         }
      }

      return heads;
   }

   private static Map<String, String> initTokenHeads() {
      Map<String, String> heads = new HashMap(1);
      ApiTokenInfo apiToken = MyTraceRequestListener.getApiToken();
      if (apiToken != null) {
         String name = apiToken.getName();
         if (heads.keySet().stream().noneMatch((k) -> {
            return k.equalsIgnoreCase(name);
         })) {
            heads.put(name, apiToken.getValue());
         }
      }

      return heads;
   }

   private static HttpDataInfo postGetBodyData(String name, String jsonReqData, String contentType) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(name));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         Map<String, String> heads = getInitheads(getContentType(contentType));
         String realurl = CoordinateUtil.getRealUrl(name, ha);

         try {
            return MyHttp.downloadByJson(realurl, jsonReqData, heads, 5000, 50000);
         } catch (Exception var12) {
            Exception e = var12;
            if (isTimeout(e, name, ha)) {
               try {
                  return MyHttp.downloadByJson(realurl, jsonReqData, heads, 5000, 50000);
               } catch (Exception var11) {
                  Exception e1 = var11;
                  if (isTimeout(e1, name, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("postGetBodyData", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", name.split("[?]")[0]));
   }

   public static String postForm(String name, String... params) {
      return getformData(name, MyHttp.getMapVs(params));
   }

   public static String postForm(String name, Map<String, String[]> inputs) {
      return getformData(name, inputs);
   }

   private static String getformData(String name, Map<String, String[]> inputs) {
      List<HttpApiInfo> hosts = CoordinateUtil.gethas(CoordinateUtil.getApiInfo(name));
      int sz = hosts.size();
      int i = 0;

      while(i < sz) {
         HttpApiInfo ha = getRandomApi(hosts);
         String realurl = CoordinateUtil.getRealUrl(name, ha);

         try {
            return getFormResponse(inputs, realurl);
         } catch (Exception var10) {
            Exception e = var10;
            if (isTimeout(e, name, ha)) {
               try {
                  return getFormResponse(inputs, realurl);
               } catch (Exception var9) {
                  Exception e1 = var9;
                  if (isTimeout(e1, name, ha)) {
                     hosts.remove(ha);
                     remove(ha);
                  }
               }
            } else {
               log.error("getformData", e);
            }

            ++i;
         }
      }

      throw new IllegalStateException(String.format("%s当前服务的主机都已经下线。。。请至少启动一台服务器。。", name.split("[?]")[0]));
   }

   private static String getFormResponse(Map<String, String[]> inputs, String realurl) {
      ApiClientInfo cl = MyTraceRequestListener.getApiClient();
      Map<String, String> heads = getInitheads(getContentType("application/x-www-form-urlencoded;charset=UTF-8"));
      if (cl != null) {
         setTransData(true, cl, heads);
         return MyHttp.postForm(realurl, inputs, 5000, 50000, heads);
      } else {
         return MyHttp.postForm(realurl, inputs, 5000, 50000, heads);
      }
   }

   private static boolean isTimeout(Throwable e, String name, HttpApiInfo ha) {
      StringWriter out = new StringWriter();
      e.printStackTrace(new PrintWriter(out));
      String error = out.toString();
      if (error.contains("Read timed out") || !error.contains("connect timed out") && !error.contains("SocketTimeoutException") && !error.contains("ConnectException") && (!error.contains("SocketException") || e.getMessage() == null || !e.getMessage().trim().contains("Connection reset")) && !error.contains("NoRouteToHostException")) {
         throw new IllegalStateException(String.format("%s %s服务访问报错", ha.getBaseUrl(), name.split("[?]")[0]), e);
      } else {
         log.warn("--{}--------访问服务：{} 超时了...........", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), name);
         return true;
      }
   }

   private static void sendObject(ClientApi ca) {
      Iterator var1 = RdsWsClient.WS_CLIENTS.iterator();

      while(var1.hasNext()) {
         WsClientInfo ws = (WsClientInfo)var1.next();
         Session session = ws.getSession();
         if (session != null && session.isOpen()) {
            try {
               session.getBasicRemote().sendObject(ca);
            } catch (EncodeException | IOException var5) {
               Exception e = var5;
               log.error("sendObject", e);
            }
         }
      }

   }

   private static void remove(HttpApiInfo ha) {
      CoordinateUtil.STE.submit(() -> {
         Iterator<ClientApi> ite = CoordinateUtil.CAS.iterator();

         while(ite.hasNext()) {
            HttpApiInfo httpApiInfo = ((ClientApi)ite.next()).getHttpApiInfo();
            if (httpApiInfo.equals(ha)) {
               ite.remove();
               log.warn("-----:{}:服务不可用自动下线...........", httpApiInfo);
               httpApiInfo.setIsOnline(false);
               sendObject(new ClientApi((Set)null, httpApiInfo));
            }
         }

      });
   }
}
