package message;

import file.DigestFile;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PutChunkMsg implements Message {
    public static final String type = "PUTCHUNK";
    public static final int chunkIndex = 6;
    private final String header;
    private final String senderId;
    private final String fileId;
    private final Integer chunkNo;
    private final Integer replication;
    private byte[] chunk;

    public PutChunkMsg(String version, String id, String fileId, int chunkNo, int replication, byte[] chunk) {
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                replication + " " +
                Message.CRLF + Message.CRLF;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.senderId = id;
        this.replication = replication;
        this.chunk = chunk;
    }

    public String getFileId() {
        return fileId;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public byte[] getChunk() {
        return this.chunk;
    }

    public int getReplication() {
        return replication;
    }

    @Override
    public byte[] getContent() {
        byte[] packetContent = header.getBytes();
        return ByteBuffer.allocate(packetContent.length + this.chunk.length)
                .put(packetContent).put(this.chunk).array();
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
    public String getSenderId() {
        return senderId;
    }

    @Override
    public String toString() {
        return type + " " + this.fileId + " chunkno. " + this.chunkNo + " rep. " + this.replication;
    }
}
