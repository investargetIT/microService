package com.cyanrocks.microService.code;

import com.cyanrocks.microService.helper.ClientApi;
import com.fasterxml.jackson.core.type.TypeReference;
import com.cyanrocks.microService.helper.MyJsonUtils;
import java.lang.invoke.MethodHandles;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestCode implements Decoder.Text<ClientApi>, Encoder.Text<ClientApi> {
   static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   public void init(EndpointConfig config) {
   }

   public void destroy() {
   }

   public String encode(ClientApi object) throws EncodeException {
      return MyJsonUtils.getJsonString(object);
   }

   public ClientApi decode(String s) throws DecodeException {
      return (ClientApi)MyJsonUtils.getObject(s, new TypeReference<ClientApi>() {
      });
   }

   public boolean willDecode(String s) {
      if (s != null && s.length() > 0 && s.startsWith("{") && s.contains("}")) {
         return true;
      } else {
         log.error(String.format("无法识别%s", s));
         return false;
      }
   }
}
