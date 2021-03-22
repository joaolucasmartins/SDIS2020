package Message;

public class GetChunkMsg implements Message {
    public static final String type = "GETCHUNK";
    public static final int CRLFField = 5;
    private final String header;
    private final String fileId;
    private final Integer chunkNo;

    public GetChunkMsg(String version, String id, String fileId, int chunkNo) {
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

    public Integer getChunkNo() {
        return chunkNo;
    }

    @Override
    public byte[] getContent() {
        return header.getBytes();
    }

    @Override
    public String getType() {
        return type;
    }
}
