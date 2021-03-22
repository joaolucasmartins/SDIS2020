package Message;

public class ChunkStoredMsg implements Message {
    static final String type = "STORED";
    private final String header;

    public ChunkStoredMsg(String version, String id, String fileId, int chunkNo) {
        this.header = version + " " +
                type + " " +
                id + " " +
                fileId + " " +
                chunkNo + " " +
                Message.CRLF + Message.CRLF;
    }

    @Override
    public byte[] getContent() {
        return header.getBytes();
    }

    @Override
    public void log() {
        System.out.println("Sent: " + header);
    }
}
