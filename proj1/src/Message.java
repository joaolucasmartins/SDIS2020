import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public interface Message {
    String CRLF = "" + 0xD + 0xA;
    int versionField = 0;
    int typeField = 1;
    int idField = 2;
    int fileField = 3;
    int chunkField = 4;
    int replicationField = 5;

    void send(MulticastSocket sock, InetAddress addr, int port) throws IOException;

    void log();
}
