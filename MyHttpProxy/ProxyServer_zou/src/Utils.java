import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by feihu on 2017/5/3.
 */
public class Utils {
    private static HashSet<String> blockSet;  //被屏蔽的网站
    private static HashMap<String, HashSet<String>> userBlockMap;  //用户以及被屏蔽的网站
    private static HashMap<String, String> fishingMap;  //钓鱼映射
    public static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'",Locale.US);
    private static final int BUFFER_SIZE = (2<<10)*5;
    public static void pipe(InputStream in, OutputStream out) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                if (len > 0) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
            }
        } catch (IOException e) {
        }
    }
    public static void pipeForCache(InputStream in, OutputStream out, String url) {
        byte[] buffer = new byte[BUFFER_SIZE];
        File file = new File("cache/"+url.hashCode()+".txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
        }
        OutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
        }
        try {
            fileOut.write((Utils.sdf.format(new Date())+"\r\n").getBytes());
        } catch (IOException e) {
        }
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                if (len > 0) {
                    fileOut.write(buffer, 0, len);
                    out.write(buffer, 0, len);
                    out.flush();
                }
            }
        } catch (IOException e) {
        }
        try {
            fileOut.close();
        } catch (IOException e) {
        }
    }
    public static void initConfig() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("config/block.txt")));
            blockSet = new HashSet<>();
            while (br.ready()) {
                blockSet.add(br.readLine());
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream("config/userBlock.txt")
            ));
            userBlockMap = new HashMap<>();
            while (br.ready()) {
                String[] line = br.readLine().split(" ");
                HashSet<String> userBlock = new HashSet<>();
                for (int i = 1; i < line.length; i++) {
                    userBlock.add(line[i]);
                }
                userBlockMap.put(line[0], userBlock);
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream("config/fish.txt")
            ));
            fishingMap = new HashMap<>();
            while (br.ready()) {
                String[] line = br.readLine().split(" ");
                fishingMap.put(line[0], line[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询一个host是否被屏蔽
     * @param host
     * @return
     */
    public static boolean isBlock(String host) {
        return blockSet.contains(host);
    }

    /**
     * 查询某用户是否不允许访问该host
     * @param user
     * @param host
     * @return
     */
    public static boolean isBlock(String user, String host) {
        return userBlockMap.containsKey(user) &&
                userBlockMap.get(user).contains(host);
    }

    /**
     * 查询被引导向的网站
     * @param host
     * @return
     */
    public static String getFishingSite(String host) {
        if (fishingMap.containsKey(host)) {
            return fishingMap.get(host);
        } else {
            return null;
        }
    }

}
