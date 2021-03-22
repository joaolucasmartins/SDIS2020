package Message;

public class RemovedMsg implements Message {
    public static final String type = "REMOVED";
    public static final int CRLFField = 5;
    private final String header;
    private final String fileId;
    private final int chunkNo;

    public RemovedMsg(String version, String id, String fileId, int chunkNo) {
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                Message.CRLF + Message.CRLF;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public byte[] getContent() {
        return this.header.getBytes();
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
        return type + " " + this.fileId;
    }
}