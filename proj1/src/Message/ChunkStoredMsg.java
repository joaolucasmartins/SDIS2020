package Message;

public class ChunkStoredMsg implements Message {
    static public final String type = "STORED";
    private final String header;
    private final String fileId;
    private final int chunkNo;

    public ChunkStoredMsg(String version, String id, String fileId, int chunkNo) {
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                Message.CRLF + Message.CRLF;

        this.fileId = fileId;
        this.chunkNo = chunkNo;
    }

    @Override
    public byte[] getContent() {
        byte[] packetContent = header.getBytes();
        return packetContent;
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
