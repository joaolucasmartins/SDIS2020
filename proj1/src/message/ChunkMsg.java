package message;

import file.DigestFile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ChunkMsg extends Message {
    public static final String type = "CHUNK";
    public static final int CRLFField = 5;
    private final int chunkNo;
    private byte[] chunk;

    public ChunkMsg(String version, String id, String fileId, int chunkNo, byte[] chunk) {
        super(version, id, fileId);
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                Message.CRLF + Message.CRLF;
        this.chunkNo = chunkNo;
        this.chunk = chunk;
    }

    public ChunkMsg(String version, String id, String fileId, int chunkNo) {
        this(version, id, fileId, chunkNo, new byte[0]);
        try {
            this.chunk = DigestFile.readChunk(fileId + File.separator + chunkNo);
        } catch (IOException e) {
            e.printStackTrace(); // TODO Fail if chunk isn't here
        }
    }

    public byte[] getChunk() {
        return chunk;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    @Override
    public byte[] getContent() {
        byte[] headerBytes = super.getContent();
        byte[] bodyBytes = this.chunk;

        return ByteBuffer.allocate(headerBytes.length + bodyBytes.length)
                .put(headerBytes).put(bodyBytes).array();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getHeaderLen() {
        return 5;
    }
}
