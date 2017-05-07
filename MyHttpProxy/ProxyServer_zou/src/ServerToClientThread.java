import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by feihu on 2017/5/2.
 */
public class ServerToClientThread extends Thread {
    private final int BUFFER_SIZE = (1 << 10)*5;
    private InputStream serverIn;
    private OutputStream clientOut;
    private byte[] buffer = new byte[BUFFER_SIZE];
    boolean isGet;
    String url;
    public ServerToClientThread(InputStream in, OutputStream out, boolean isGet, String url) {
        this.serverIn = in;
        this.clientOut = out;
        this.isGet = isGet;
        this.url = url;
    }

    @Override
    public void run() {
        if (isGet)
            Utils.pipeForCache(serverIn,clientOut, url);
        else
        Utils.pipe(serverIn, clientOut);
    }
}
