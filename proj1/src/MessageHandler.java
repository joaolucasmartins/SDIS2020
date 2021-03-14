import Message.Message;
import Message.ChunkBackupMsg;
import Message.ChunkStoredMsg;

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
        System.out.println();
        System.out.println("Received: " + received);

        String[] receivedFields = received.split(" ");
        if (receivedFields[Message.idField].equals(this.selfID)) {
            System.out.println("We were the ones that sent this message. Skipping..");
        } else {

            try {
                Message message = createMessage(receivedFields);
                System.out.println("Received " + receivedFields[Message.typeField]);

                if (message.getClass() == ChunkBackupMsg.class) {
                    ChunkBackupMsg backupMsg = (ChunkBackupMsg) message;
                    Message response = new ChunkStoredMsg(this.protocolVersion, this.selfID,
                            backupMsg.getFileId(), backupMsg.getChunkNo());
                    this.MDBSock.send(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
