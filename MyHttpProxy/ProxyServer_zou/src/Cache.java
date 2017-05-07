import java.io.*;

/**
 * Created by feihu on 2017/5/6.
 */
public class Cache {
    public static File getCachedFile(String url) {
        File file = new File("cache/"+url.hashCode()+".txt");
        if (file.exists()){
            return file;
        }
        return null;
    }

    public static String getLastModified(File cachedFile) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(cachedFile)));
            String line = br.readLine();
            br.close();
            return line.replace("Last-Modified: ", "");
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return null;
    }
}
