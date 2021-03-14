package Message;

public class ChunkBackupMsg implements Message {
    static final String type = "PUTCHUNK";
    private final String header;
    private final String fileId;
    private final Integer chunkNo;

    public ChunkBackupMsg(String version, String id, String fileId, int chunkNo, int replication) {
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                replication + " " +
                Message.CRLF + Message.CRLF;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        // TODO add missing chunk here
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
        return packetContent;
    }

    @Override
    public void log() {
        System.out.println("Sent: " + header);
    }
}
