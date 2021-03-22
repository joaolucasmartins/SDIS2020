package Message;

import File.DigestFile;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ChunkBackupMsg implements Message {
    public static final String type = "PUTCHUNK";
    public static final int chunkIndex = 6;
    private final String header;
    private final String fileId;
    private final Integer chunkNo;
    private byte[] chunk;

    public ChunkBackupMsg(String version, String id, String fileId, int chunkNo, int replication, byte[] chunk) {
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                replication + " " +
                Message.CRLF + Message.CRLF;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.chunk = chunk;
    }
    public ChunkBackupMsg(String version, String id, String fileId, int chunkNo, int replication, String filename) {
        this(version, id, fileId, chunkNo, replication, new byte[0]);
        try {
            this.chunk = DigestFile.readChunk(filename, chunkNo);
        } catch (IOException e) {
            e.printStackTrace(); // TODO Fail if chunk isn't here
        }
    }

    public String getFileId() {
        return fileId;
    }

    public Integer getChunkNo() {
        return chunkNo;
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
    public String toString() {
        return type + " " + this.fileId + " chunkno. " + this.chunkNo;
    }
}
