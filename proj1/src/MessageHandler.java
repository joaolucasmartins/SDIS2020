import File.DigestFile;
import Message.Message;
import Message.ChunkBackupMsg;
import Message.ChunkMsg;
import Message.ChunkStoredMsg;
import Message.GetChunkMsg;

import java.io.File;
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
        String[] receivedFields = received.split(" ", Message.idField + 1);
        if (receivedFields[Message.idField].equals(this.selfID)) {
            System.out.println("We were the ones that sent this message. Skipping..");
        } else {

            try { // TODO Check for sender id for multiple peers
                System.out.println("Received " + Arrays.toString(receivedFields));
                Message message = createMessage(received);

                if (message.getClass() == ChunkBackupMsg.class) {
                    ChunkBackupMsg backupMsg = (ChunkBackupMsg) message;
                    DigestFile.writeChunk(backupMsg.getFileId() + File.separator + backupMsg.getChunkNo(),
                            backupMsg.getContent(), backupMsg.getContent().length);
                    Message response = new ChunkStoredMsg(this.protocolVersion, this.selfID,
                            backupMsg.getFileId(), backupMsg.getChunkNo());
                    Random random = new Random();
                    this.MDBSock.send(response, random.nextInt(401)); //TODO make 401 a static member?
                }

                if (message.getClass() == GetChunkMsg.class) {
                    GetChunkMsg getChunkMsg = (GetChunkMsg) message;
                    if (DigestFile.hasChunk(getChunkMsg.getFileId(), getChunkMsg.getChunkNo())) {
                        // TODO Thread here
                        ChunkMsg response = new ChunkMsg(this.protocolVersion, this.selfID,
                                getChunkMsg.getFileId(), getChunkMsg.getChunkNo());
                        this.MCSock.send(response, new Random().nextInt(401));
                    }
                }

                if (message.getClass() == ChunkMsg.class) {
                    ChunkMsg getChunkMsg = (ChunkMsg) message;
                    DigestFile.writeChunk(getChunkMsg, getChunkMsg.getFileId(), getChunkMsg.getChunkNo());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
