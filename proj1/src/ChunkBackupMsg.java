import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ChunkBackupMsg implements Message {
    private final String header;

    public ChunkBackupMsg(String version, String id, String fileId, int chunkNo, int replication) {
        this.header = version + " " +
                "PUTCHUNK" + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                replication + " " +
                CRLF + CRLF;
    }

    @Override
    public void send(MulticastSocket sock, InetAddress group, int port) throws IOException {
        byte[] packetContent = header.getBytes();
        DatagramPacket packet = new DatagramPacket(packetContent, packetContent.length, group, port);
        sock.send(packet);

        this.log();
    }

    @Override
    public void log() {
        System.out.println("Sent: " + header);
    }
}
