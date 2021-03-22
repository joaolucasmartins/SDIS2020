package Message;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.lang.Integer.parseInt;

public class MessageCreator {
    private static boolean containsCRLF(String body) {
        return body.substring(0, 4).equals(Message.CRLF + Message.CRLF);
    }
    public static Message createMessage(String received) throws NoSuchMessage { // TODO Make version correspond with peer and not incoming msg
        String[] message = received.split(" ", Message.CRLFField + 1);
        String body;
        Message res;

        switch (message[Message.typeField]) {
            // Backup Subprotocol
            case (ChunkBackupMsg.type):
                body = message[ChunkBackupMsg.CRLFField];
                if (!containsCRLF(body)) {
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
            // Restore Subprotocol
            case (GetChunkMsg.type):
                body = message[GetChunkMsg.CRLFField];
                if (!containsCRLF(body)) {
                    res = null;
                    break; // TODO Throw new exception here?
                }
                res = new GetChunkMsg(message[Message.versionField], message[Message.idField],
                        message[Message.fileField],
                        parseInt(message[Message.chunkField]));
                break;
            case (ChunkMsg.type):
                body = message[ChunkMsg.CRLFField];
                System.out.println("Body" + body);
                if (!containsCRLF(body)) {
                    res = null;
                    break; // TODO Throw new exception here?
                }
                res = new ChunkMsg(message[Message.versionField], message[Message.idField],
                        message[Message.fileField],
                        parseInt(message[Message.chunkField]),
                        body.substring(5).getBytes(StandardCharsets.UTF_8));
            case (RemovedMsg.type):
                body = message[RemovedMsg.CRLFField];
                System.out.println("Body" + body);
                if (!containsCRLF(body)) {
                    res = null;
                    break; // TODO Throw new exception here?
                }
                res = new RemovedMsg(message[Message.versionField], message[Message.idField],
                        message[Message.fileField],
                        parseInt(message[Message.chunkField]));
                break;
            default:
                throw new NoSuchMessage(message[Message.typeField]);
        }
        return res;
    }
}
