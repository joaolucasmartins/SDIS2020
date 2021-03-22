package Message;

public class DeleteMsg implements Message {
    public static final String type = "DELETE";
    private final String header;
    private final String fileId;

    public DeleteMsg(String version, String id, String fileId) {
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
    public int getHeaderLen() {
        return 4;
    }

    @Override
    public String toString() {
        return type + " " + this.fileId;
    }
}
