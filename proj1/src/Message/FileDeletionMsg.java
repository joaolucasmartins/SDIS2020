package Message;

public class FileDeletionMsg implements Message{
    static final String type = "DELETE";
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
    public void log() {
        System.out.println("Sent: " + this.header);
    }
}
