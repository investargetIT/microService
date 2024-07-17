package com.cyanrocks.microService.helper;

import com.cyanrocks.microService.anno.RestApi;
import com.cyanrocks.microService.em.Pl;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

public final class ClassHelper {
   public static ClassLoader getDefaultClassLoader() {
      ClassLoader cl = null;

      try {
         cl = MethodHandles.lookup().lookupClass().getClassLoader();
      } catch (Throwable var3) {
      }

      if (cl == null) {
         cl = Thread.currentThread().getContextClassLoader();
         if (cl == null) {
            try {
               cl = ClassLoader.getSystemClassLoader();
            } catch (Throwable var2) {
            }
         }
      }

      return cl;
   }

   public static final void getjarpms(final Set<ApiInfo> ais, Path file, final String rootpath) {
      try {
         FileSystem fs = FileSystems.newFileSystem(file, getDefaultClassLoader());
         Files.walkFileTree(fs.getPath(rootpath), new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
               String name = file.toString();
               if (name.startsWith(rootpath)) {
                  name = name.replaceFirst(rootpath, "");
               }

               if (name.endsWith(".class") && !name.contains("$")) {
                  String cln = name.replace('/', '.').substring(0, name.lastIndexOf("."));
                  ClassHelper.getPms(ais, cln);
               }

               return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (Exception var4) {
         Exception e = var4;
         throw new RuntimeException(e);
      }
   }

   public static void getPms(Set<ApiInfo> ais, String cln) {
      try {
         Class<?> contr = Class.forName(cln);
         if (contr.isAnnotationPresent(Controller.class) || contr.isAnnotationPresent(RestController.class)) {
            String[] classpaths = getpaths(contr);
            RestApi ra = null;
            if (contr.isAnnotationPresent(RestApi.class)) {
               ra = (RestApi)contr.getAnnotation(RestApi.class);
            }

            Method[] ms = contr.getMethods();
            Method[] var6 = ms;
            int var7 = ms.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               Method m = var6[var8];
               RestApi mra = null;
               if (m.isAnnotationPresent(RestApi.class)) {
                  mra = (RestApi)m.getAnnotation(RestApi.class);
               }

               if (m.isAnnotationPresent(RequestMapping.class) || m.isAnnotationPresent(PostMapping.class) || m.isAnnotationPresent(GetMapping.class)) {
                  if (mra == null && ra == null) {
                     collect(ais, classpaths, m);
                  } else if (mra == null) {
                     if (!ra.value().equals(Pl.PRIVATE)) {
                        collect(ais, classpaths, m);
                     }
                  } else if (!mra.value().equals(Pl.PRIVATE)) {
                     collect(ais, classpaths, m);
                  }
               }
            }
         }
      } catch (Throwable var11) {
         Throwable e = var11;
         e.printStackTrace();
      }

   }

   private static String[] getpaths(Class<?> contr) {
      String[] classpaths = null;
      String[] value;
      String[] path;
      if (contr.isAnnotationPresent(RequestMapping.class)) {
         RequestMapping crm = (RequestMapping)contr.getAnnotation(RequestMapping.class);
         value = crm.value();
         path = crm.path();
         classpaths = getmppaths(classpaths, value, path);
      } else if (contr.isAnnotationPresent(GetMapping.class)) {
         GetMapping crm = (GetMapping)contr.getAnnotation(GetMapping.class);
         value = crm.value();
         path = crm.path();
         classpaths = getmppaths(classpaths, value, path);
      } else if (contr.isAnnotationPresent(PostMapping.class)) {
         PostMapping crm = (PostMapping)contr.getAnnotation(PostMapping.class);
         value = crm.value();
         path = crm.path();
         classpaths = getmppaths(classpaths, value, path);
      }

      return classpaths;
   }

   private static String[] getmppaths(String[] classpaths, String[] value, String[] path) {
      if (value.length > 0 || path.length > 0) {
         classpaths = value.length > 0 ? value : path;
      }

      return classpaths;
   }

   private static void collect(Set<ApiInfo> ais, String[] classpaths, Method m) {
      String[] value;
      if (m.isAnnotationPresent(RequestMapping.class)) {
         RequestMapping pm = (RequestMapping)m.getAnnotation(RequestMapping.class);
         value = pm.value();
         collectApiInfo(ais, classpaths, value);
      } else if (m.isAnnotationPresent(GetMapping.class)) {
         GetMapping pm = (GetMapping)m.getAnnotation(GetMapping.class);
         value = pm.value();
         collectApiInfo(ais, classpaths, value);
      } else if (m.isAnnotationPresent(PostMapping.class)) {
         PostMapping pm = (PostMapping)m.getAnnotation(PostMapping.class);
         value = pm.value();
         collectApiInfo(ais, classpaths, value);
      }

   }

   private static void collectApiInfo(Set<ApiInfo> ais, String[] classpaths, String[] value) {
      String[] var3;
      int var4;
      int var5;
      String mp;
      if (value.length > 0) {
         var3 = value;
         var4 = value.length;

         for(var5 = 0; var5 < var4; ++var5) {
            mp = var3[var5];
            if (classpaths != null) {
               String[] var7 = classpaths;
               int var8 = classpaths.length;

               for(int var9 = 0; var9 < var8; ++var9) {
                  String clspath = var7[var9];
                  format(ais, clspath, mp);
               }
            } else {
               format(ais, "", mp);
            }
         }
      } else if (classpaths != null) {
         var3 = classpaths;
         var4 = classpaths.length;

         for(var5 = 0; var5 < var4; ++var5) {
            mp = var3[var5];
            add(ais, mp);
         }
      } else {
         add(ais, "");
      }

   }

   private static void format(Set<ApiInfo> ais, String clspath, String mp) {
      String name = String.format("%s%s%s", clspath, !clspath.endsWith("/") && !mp.startsWith("/") ? "/" : "", mp);
      if (name.startsWith("/")) {
         name = name.replaceFirst("/", "");
      }

      if (name.contains("//")) {
         name = name.replaceFirst("//", "/");
      }

      add(ais, name);
   }

   private static void add(Set<ApiInfo> ais, String name) {
      name = getApiName(name);
      ApiInfo get = new ApiInfo(name);
      get.setMethod(RequestMethod.GET);
      ApiInfo post = new ApiInfo(name);
      post.setMethod(RequestMethod.POST);
      ais.add(get);
      ais.add(post);
   }

   public static String getApiName(String name) {
      if (!name.startsWith("/")) {
         name = "/" + name;
      }

      return name;
   }
}
