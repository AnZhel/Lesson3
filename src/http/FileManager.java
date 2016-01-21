package http;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
    private String path;
    private static ConcurrentHashMap<String, CacheChecker> map = new ConcurrentHashMap<String, CacheChecker>();

    public FileManager(String path) {
        // "c:\folder\" --> "c:\folder"
        if (path.endsWith("/") || path.endsWith("\\"))
            path = path.substring(0, path.length() - 1);

        this.path = path;
    }

    public byte[] get(String url) {
        try {
            CacheChecker cacheChecker = map.get(url);
            if (cacheChecker != null && cacheChecker.data != null && !cacheChecker.isTimeToKill()) // in cache
                return cacheChecker.data;

            // "c:\folder" + "/index.html" -> "c:/folder/index.html"
            String fullPath = path.replace('\\', '/') + url;

            RandomAccessFile f = new RandomAccessFile(fullPath, "r");
            try {
                byte[] buf = new byte[(int) f.length()];
                f.read(buf, 0, buf.length);
                cacheChecker = new CacheChecker(buf,25000);
            } finally {
                f.close();
            }

            map.put(url, cacheChecker); // put to cache
            return cacheChecker.data;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
