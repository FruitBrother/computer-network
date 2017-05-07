import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by feihu on 2017/5/5.
 */
public class ProxyThread implements Runnable {
    private final int MAX_SIZE = 4096;
    private final int TIMEOUT = 500;
    private Socket clientSocket;
    private Socket serverSocket;
    private InputStream clientIn;
    private OutputStream clientOut;
    private String host, method;
    private String url;
    private int port;
    private ArrayList<String> headers;
    public ProxyThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.clientSocket.setKeepAlive(true);
            this.clientSocket.setSoTimeout(TIMEOUT);
            clientIn = this.clientSocket.getInputStream();
            clientOut = this.clientSocket.getOutputStream();
        } catch (SocketException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void readHeaders() {
        headers = new ArrayList<>();
        char c;
        try {
            StringBuilder builder = new StringBuilder();
            while ((c = (char) clientIn.read()) != '\n') {
                builder.append(c);
                if (builder.length() == MAX_SIZE) {
                    return;
                }
            }
            //此时读取到的内容为method url version \r
            analyzeMethod(builder.toString());
            headers.add(builder.toString());
            //将剩余的请求行读入
            while (true) {
                builder = new StringBuilder();
                while ((c = (char) clientIn.read()) != '\n') {
                    builder.append(c);
                }
                String line = builder.toString();
                if (line.equals("\r")) {
                    //空行，结束
                    break;
                } else {
                    if (!line.startsWith("If-Modified-Since")) {
                        headers.add(line);
                    }
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private void analyzeMethod(String line) {
        String[] firstLine = line.split(" ");
        method = firstLine[0];
        url = firstLine[1];
        int tPort;
        if (method.equals("CONNECT")) {
            tPort = 443;
        } else {
            tPort = 80;
        }
        host = url;
        int index = host.indexOf("//");
        if (index != -1) {
            host = host.substring(index + 2);
        }
        index = host.indexOf("/");
        if (index != -1) {
            host = host.substring(0, index);
        }
        index = host.indexOf(":");  //寻找port
        if (index != -1) {
            port = Integer.parseInt(host.substring(index + 1));
            host = host.substring(0, index);
        }
        port = tPort;
    }

    /**
     * 将请求转化为字符串
     * @return
     */
    private String getHeaders() {
        StringBuilder builder = new StringBuilder();
        for (String line: headers
             ) {
            //需追加\n
            builder.append(line).append('\n');
        }
        builder.append("\r\n");  //空行
        return builder.toString();
    }

    private void setHeaders(ArrayList<String> headers) {
        this.headers = headers;
    }

    @Override
    public void run() {
        readHeaders();  //读取头部
        try {
            if (method != null) {
                System.out.println("接收请求：" + method + " " + url);
            }
            if (host == null) {
                clientOut.write("HTTP/1.1 500 Connection FAILED\r\n\r\n".getBytes());
                clientOut.flush();
                return;
            } else if (Utils.isBlock(host)
                    || Utils.isBlock(clientSocket.getInetAddress().toString(), host)) {
                System.out.println(host+"被屏蔽，拒绝访问");
                clientOut.write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
                clientOut.flush();
                return;
            } else if (Utils.getFishingSite(host) != null){
                //钓鱼
                System.out.println(Utils.getFishingSite(host));
                String newUrl = Utils.getFishingSite(host);
                URL ur = new URL(newUrl);
                URLConnection uc = ur.openConnection();
                Utils.pipe(uc.getInputStream(), clientOut);
                return;
            }
            serverSocket = new Socket(host, port);
            serverSocket.setKeepAlive(true);
            serverSocket.setSoTimeout(TIMEOUT);
            InputStream serverIn = serverSocket.getInputStream();
            OutputStream serverOut = serverSocket.getOutputStream();
            if (method.equals("GET")) {  //涉及到cache
                File cachedFile = Cache.getCachedFile(url);
                if (cachedFile == null) {
                    System.out.println(url+"未被cache");
                    serverOut.write(getHeaders().getBytes());
                    serverOut.flush();
                    Utils.pipe(clientIn, serverOut);
                    Utils.pipeForCache(serverIn, clientOut, url);
                } else {
                    String modifiedTime = Cache.getLastModified(cachedFile);
                    //查询是否修改了
                    headers.add("If-Modified-Since: "+modifiedTime+"\r");
                    serverOut.write(getHeaders().getBytes());
                    serverOut.flush();
                    Utils.pipe(clientIn, serverOut);
                    //接收server的回复
                    StringBuilder builder = new StringBuilder();
                    char chr;
                    while ((chr = (char) serverIn.read()) != '\n') {
                        builder.append(chr);
                    }
                    String line = builder.substring(0, builder.length()-1);
                    if (line.contains("200")) {
                        System.out.println(url+"已修改，更新proxy的cache");
                        clientOut.write((line+"\r\n").getBytes());
                        Utils.pipeForCache(serverIn, clientOut, url);
                    } else if (line.contains("304")){  //未修改
                        System.out.println(url+"未修改，使用proxy的cache");
                        InputStream fileIn = new FileInputStream(cachedFile);
                        while (((char)fileIn.read()) != '\n');
                        Utils.pipe(fileIn, clientOut);
                    } else {
                    }
                }
            } else {
                if (method.equals("CONNECT")) {
                    clientOut.write("HTTP/1.1 200 Connection established\r\n\r\n".getBytes());
                    clientOut.flush();
                    ServerToClientThread sc = new ServerToClientThread(serverIn, clientOut, false, url);
                    sc.start();
                    ClientToServerThread cs = new ClientToServerThread(clientIn, serverOut);
                    cs.start();
                    sc.join();
                    cs.join();
                } else if (method.equals("POST")) {
                    serverOut.write(getHeaders().getBytes());
                    serverOut.flush();
                    Utils.pipe(clientIn, serverOut);
                    Utils.pipe(serverIn, clientOut);
                }
            }
        } catch (IOException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
