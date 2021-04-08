package message;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ChunkTCPMsg extends Message{
    public static final String type = "CHUNK";
    private final int chunkNo;
    private int tcpPort;
    private String ip;

    public ChunkTCPMsg(String version, String id, String fileId, int chunkNo, byte[] body) {
        super(version, id, fileId);
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                Message.CRLF + Message.CRLF;
        this.chunkNo = chunkNo;
        String[] b = Arrays.toString(body).split(" ", 2);
        this.setTCPAddress(b[0], Integer.parseInt(b[1]));
    }

    public ChunkTCPMsg(String version, String id, String fileId, int chunkNo) {
        this(version, id, fileId, chunkNo, ("undefined -1").getBytes());
    }

    public void setTCPAddress(String ip, Integer port) {
        this.ip = ip;
        this.tcpPort = port;
    }

    public String getIp() {
        return ip;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    @Override
    public byte[] getContent() {
        byte[] headerBytes = super.getContent();
        byte[] bodyBytes = (this.ip + " " + this.tcpPort).getBytes();

        return ByteBuffer.allocate(headerBytes.length + bodyBytes.length)
                .put(headerBytes).put(bodyBytes).array();
    }

    public int getChunkNo() {
        return chunkNo;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getHeaderLen() {
        return 6;
    }

    @Override
    public String toString() {
        return type + " " + this.fileId + " chunkno " + this.chunkNo + " tcp port " + this.tcpPort;
    }
}
