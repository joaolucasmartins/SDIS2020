package message;

import file.DigestFile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ChunkMsg implements Message {
    public static final String type = "CHUNK";
    public static final int CRLFField = 5;
    private final String header;
    private final String senderId;
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
        this.senderId = id;
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

    public byte[] getChunk() {
        return chunk;
    }

    @Override
    public byte[] getContent() {
        byte[] headerBytes = header.getBytes();
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

    @Override
    public String getSenderId() {
        return senderId;
    }
}
