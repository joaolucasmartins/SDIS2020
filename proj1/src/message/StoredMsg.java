package message;

public class StoredMsg implements Message {
    public static final String type = "STORED";
    private final String senderId;
    private final String header;
    private final String fileId;
    private final int chunkNo;

    public StoredMsg(String version, String id, String fileId, int chunkNo) {
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                Message.CRLF + Message.CRLF;

        this.fileId = fileId;
        this.senderId = id;
        this.chunkNo = chunkNo;
    }

    public String getFileId() {
        return fileId;
    }

    public Integer getChunkNo() {
        return chunkNo;
    }

    public String getSenderId() {
        return senderId;
    }

    @Override
    public byte[] getContent() {
        return header.getBytes();
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
    public String toString() {
        return type + " " + this.fileId + " chunkno. " + this.chunkNo;
    }
}