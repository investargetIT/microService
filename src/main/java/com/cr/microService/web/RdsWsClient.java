package com.cr.microService.web;

import com.cr.microService.helper.ClientApi;
import com.cr.microService.helper.CoordinateUtil;
import com.cr.microService.helper.MyJsonUtils;
import com.cr.microService.helper.WsClientInfo;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdsWsClient extends Endpoint {
   public static final String WS_URL_NAME = "wsUrl";
   public static final ScheduledExecutorService STPOOL = Executors.newScheduledThreadPool(2);
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   public static final Set<WsClientInfo> WS_CLIENTS = new HashSet();

   @OnMessage
   public void message(ClientApi api) {
      CoordinateUtil.STE.submit(() -> {
         if (api.getHttpApiInfo().getIsOnline()) {
            CoordinateUtil.CAS.add(api);
            log.info(String.format("%s服务上线%s个服务", api.getHttpApiInfo().getBaseUrl(), api.getApis().size()));
         } else {
            log.info(String.format("%s服务下线", api.getHttpApiInfo().getBaseUrl()));
            CoordinateUtil.CAS.remove(api);
         }

      });
   }

   @OnOpen
   public void onOpen(final Session session, EndpointConfig config) {
      int maxbufsize = session.getMaxBinaryMessageBufferSize() * 800;
      session.setMaxBinaryMessageBufferSize(maxbufsize);
      session.setMaxTextMessageBufferSize(maxbufsize);
      String wsurl = config.getUserProperties().get("wsUrl").toString();
      WS_CLIENTS.add(new WsClientInfo(session, wsurl));
      session.addMessageHandler(new MessageHandler.Whole<ClientApi>() {
         public void onMessage(ClientApi message) {
            RdsWsClient.this.message(message);
         }
      });
      session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
         public void onMessage(PongMessage message) {
            RdsWsClient.this.pong(message, session);
         }
      });
      STPOOL.submit(() -> {
         try {
            TimeUnit.SECONDS.sleep(5L);
            log.info("服务注册开始...{}", wsurl);
            session.getBasicRemote().sendText(MyJsonUtils.getJsonString(new ClientApi(CoordinateUtil.LOCAL_APIS, CoordinateUtil.HAI)));
            log.info("...服务注册成功.,本次注册的服务数量:{}", CoordinateUtil.LOCAL_APIS.size());
         } catch (Exception var5) {
            Exception e = var5;
            log.error("send data json error ", e);

            try {
               session.close();
            } catch (IOException var4) {
               IOException e1 = var4;
               log.error("", e1);
            }
         }

      });
      log.info("WS_CLIENTS:{}", WS_CLIENTS.size());
   }

   @OnClose
   public void onClose(Session session, CloseReason cr) {
      WsClientInfo wc = (WsClientInfo)WS_CLIENTS.stream().filter((s) -> {
         return s.getSession().getId().equals(session.getId());
      }).findAny().orElse((WsClientInfo) null);
      log.error("{}...和中央服务器连接已断开....{}.", cr.getCloseCode(), wc.getBaseUrl());
      WS_CLIENTS.remove(wc);
      log.info("WS_CLIENTS:{}", WS_CLIENTS.size());
      CoordinateUtil.reconnectremote();
   }

   @OnMessage
   public void pong(PongMessage pm, Session session) {
      WS_CLIENTS.forEach((c) -> {
         if (c.getSession() != null && c.getSession().getId().equals(session.getId())) {
            c.setAlive(true);
         }

      });
   }

   @OnError
   public void onError(Session session, Throwable a) {
      log.error("client error", a);

      try {
         if (session.isOpen()) {
            session.close();
         }
      } catch (IOException var4) {
         IOException e = var4;
         log.error("close error ", e);
      }

   }

   static {
      STPOOL.scheduleWithFixedDelay(() -> {
         WS_CLIENTS.forEach((c) -> {
            try {
               if (c.getSession() != null && c.getSession().isOpen()) {
                  if (c.isAlive()) {
                     c.setAlive(false);
                     c.getSession().getBasicRemote().sendPing(ByteBuffer.wrap("".getBytes()));
                  } else {
                     c.getSession().close();
                  }
               }
            } catch (Exception var2) {
               Exception e = var2;
               log.error("error", e);
            }

         });
      }, 117L, 331L, TimeUnit.SECONDS);
   }
}
