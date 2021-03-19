package Message;

import java.nio.charset.StandardCharsets;

import static java.lang.Integer.parseInt;

public class MessageCreator {
    public static Message createMessage(String[] message) throws NoSuchMessage {
        Message res;
        switch (message[Message.typeField]) {
            case (ChunkBackupMsg.type):
                String body = message[ChunkBackupMsg.chunkIndex];
                if (!body.substring(0, 4).equals(Message.CRLF + Message.CRLF)) {
                    res = null;
                    break; // TODO Throw new exception here?
                }
                res = new ChunkBackupMsg(message[Message.versionField], message[Message.idField],
                        message[Message.fileField],
                        parseInt(message[Message.chunkField]),
                        parseInt(message[Message.replicationField]),
                        body.substring(5).getBytes(StandardCharsets.UTF_8));

                break;
            case (ChunkStoredMsg.type):
                res = new ChunkStoredMsg(message[Message.versionField], message[Message.idField],
                        message[Message.fileField],
                        parseInt(message[Message.chunkField]));
                break;
            default:
                throw new NoSuchMessage(message[Message.typeField]);
        }
        return res;
    }
}
