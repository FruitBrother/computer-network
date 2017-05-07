import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by feihu on 2017/5/2.
 */
public class ClientToServerThread extends Thread {
    private final int BUFFER_SIZE = (2 << 10)*5;
    private InputStream clientIn;
    private OutputStream serverOut;
    private byte[] buffer = new byte[BUFFER_SIZE];
    public ClientToServerThread(InputStream in, OutputStream out) {
        this.clientIn = in;
        this.serverOut = out;
    }

    @Override
    public void run() {
        Utils.pipe(clientIn, serverOut);
    }
}
