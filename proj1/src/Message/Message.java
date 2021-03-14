package Message;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public interface Message {
    String CRLF = String.valueOf((char) 0xD) + String.valueOf((char) 0xA);
    int versionField = 0;
    int typeField = 1;
    int idField = 2;
    int fileField = 3;
    int chunkField = 4;
    int replicationField = 5;
    int CRLFField = 6;

    byte[] getContent();

    void log();
}
