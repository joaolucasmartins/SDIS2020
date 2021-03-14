package Message;

import static java.lang.Integer.parseInt;

public class MessageCreator {
    public static Message createMessage(String[] message) throws NoSuchMessage {
        Message res;
        switch (message[Message.typeField]) {
            case (ChunkBackupMsg.type):
                res = new ChunkBackupMsg(message[Message.versionField], message[Message.idField],
                        message[Message.fileField],
                        parseInt(message[Message.chunkField]),
                        parseInt(message[Message.replicationField]));
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