package Message;

public class FileDeletionMsg implements Message {
    public static final String type = "DELETE";
    private final String header;
    private final String fileId;

    public FileDeletionMsg(String version, String id, String fileId) {
        this.fileId = fileId;
        this.header = version + " " +
                type + " " +
                id + " " +
                this.fileId + " " +
                Message.CRLF + Message.CRLF;
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
    public String toString() {
        return type + " " + this.fileId;
    }
}
