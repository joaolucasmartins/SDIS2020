import File.DigestFile;
import Message.Message;
import Message.ChunkBackupMsg;
import Message.ChunkStoredMsg;
import Message.FileDeletionMsg;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Random;

import static Message.MessageCreator.createMessage;

public class MessageHandler {
    private final String selfID;
    private final String protocolVersion;
    private final SockThread MCSock;
    private final SockThread MDBSock;
    private final SockThread MDRSock;

    public MessageHandler(String selfID, String protocolVersion, SockThread MCSock, SockThread MDBSock, SockThread MDRSock) {
        this.selfID = selfID;
        this.protocolVersion = protocolVersion;
        this.MCSock = MCSock;
        this.MDBSock = MDBSock;
        this.MDRSock = MDRSock;
        this.MCSock.setHandler(this);
        this.MDBSock.setHandler(this);
        this.MDRSock.setHandler(this);
    }

    public void handleMessage(SockThread sock, String received) {
        String[] receivedFields = received.split(" ");
        if (receivedFields[Message.idField].equals(this.selfID)) {
            System.out.println("We were the ones that sent this message. Skipping..");
            return;
        }

        // handle the request
        final String msgType = receivedFields[Message.typeField];
        switch (msgType) {
            case FileDeletionMsg.type:
                // TODO delete file here
                return;  // file deletion doesn't send a reply
            default:
                break;
        }

        // construct the reply
        try {
            System.out.println("Received: " + Arrays.toString(receivedFields));
            Message message = createMessage(receivedFields);
            switch (message.getType()) {
                case ChunkBackupMsg.type:
                    ChunkBackupMsg backupMsg = (ChunkBackupMsg) message;
                    try {
                        DigestFile.writeChunk(backupMsg.getFileId() + File.separator + backupMsg.getChunkNo(),
                                backupMsg.getContent(), backupMsg.getContent().length);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    Message response = new ChunkStoredMsg(this.protocolVersion, this.selfID,
                            backupMsg.getFileId(), backupMsg.getChunkNo());
                    Random random = new Random();
                    this.MDBSock.send(response, random.nextInt(401)); //TODO make 401 a static member?
                    break;
                case ChunkStoredMsg.type:
                    break;
                default:
                    // unreachable
                    break;
            }
        } catch (Exception e) {
            System.err.println("Failed constructing reply for " + msgType);
        }
    }
}
