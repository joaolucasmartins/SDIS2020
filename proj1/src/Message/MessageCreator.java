package Message;

import static java.lang.Integer.parseInt;

public class MessageCreator {
    private static boolean containsCRLF(String body) {
        return body.substring(0, 4).equals(Message.CRLF + Message.CRLF);
    }

    public static Message createMessage(String[] header, String body) throws NoSuchMessage {
        Message res = null;
        switch (header[Message.typeField]) {
            // Backup Subprotocol
            case (PutChunkMsg.type):
                res = new PutChunkMsg(header[Message.versionField],
                        header[Message.idField],
                        header[Message.fileField],
                        parseInt(header[Message.chunkField]),
                        parseInt(header[Message.replicationField]),
                        body.getBytes());
                break;
            case (StoredMsg.type):
                res = new StoredMsg(header[Message.versionField],
                        header[Message.idField],
                        header[Message.fileField],
                        parseInt(header[Message.chunkField]));
                break;
            // Restore Subprotocol
            case (GetChunkMsg.type):
                res = new GetChunkMsg(header[Message.versionField],
                        header[Message.idField],
                        header[Message.fileField],
                        parseInt(header[Message.chunkField]));
                break;
            case (ChunkMsg.type):
                res = new ChunkMsg(header[Message.versionField],
                        header[Message.idField],
                        header[Message.fileField],
                        parseInt(header[Message.chunkField]),
                        body.getBytes());
                break;
            // File deletion Subprotocol
            case (DeleteMsg.type):
                res = new DeleteMsg(header[Message.versionField],
                        header[Message.idField],
                        header[Message.fileField]);
                break;
            default:
                throw new NoSuchMessage(header[Message.typeField]);
        }

        assert res != null;
        return res;
    }
}
