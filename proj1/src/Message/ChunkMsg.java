package Message;

import File.DigestFile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ChunkMsg implements Message {
    public static final String type = "CHUNK";
    public static final int CRLFField = 5;
    private final String header;
    private final String fileId;
    private final Integer chunkNo;
    private byte[] chunk;

    public ChunkMsg(String version, String id, String fileId, int chunkNo, byte[] chunk) {
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                Message.CRLF + Message.CRLF;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.chunk = chunk;
    }

    public ChunkMsg(String version, String id, String fileId, int chunkNo, String filename) {
        this(version, id, fileId, chunkNo, new byte[0]);
        try {
            this.chunk = DigestFile.readChunk(filename, chunkNo);
        } catch (IOException e) {
            e.printStackTrace(); // TODO Fail if chunk isn't here
        }
    }

    public ChunkMsg(String version, String id, String fileId, int chunkNo) {
        this(version, id, fileId, chunkNo, new byte[0]);
        try {
            this.chunk = DigestFile.readChunk(fileId + File.separator + chunkNo);
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
}
