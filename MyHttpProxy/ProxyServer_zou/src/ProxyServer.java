import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by feihu on 2017/5/5.
 */
public class ProxyServer {
    private int port = 8888;
    public static int count = 0;
    public ProxyServer() {
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();  //线程池
        Utils.initConfig();
        try {
            ServerSocket proxySocket = new ServerSocket(port);
            System.out.println("代理已启动，端口号："+port);
            while (true) {
                //接受来自client的连接并启动线程
                Socket clientSocket = proxySocket.accept();
                cachedThreadPool.execute(new ProxyThread(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("接收来自Client的连接请求出错");
        }
    }
}
