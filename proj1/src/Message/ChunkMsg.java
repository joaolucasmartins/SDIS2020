package Message;

import File.DigestFile;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ChunkMsg implements Message {
    static final String type = "CHUNK";
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
    public void log() {
        System.out.println("Sent: " + header);
    }
}
