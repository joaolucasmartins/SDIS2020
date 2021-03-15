import Message.Message;
import Message.ChunkBackupMsg;
import Message.ChunkStoredMsg;

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
        } else {

            try {
                Message message = createMessage(receivedFields);
                System.out.println("Received " + Arrays.toString(receivedFields));

                if (message.getClass() == ChunkBackupMsg.class) {
                    ChunkBackupMsg backupMsg = (ChunkBackupMsg) message;
                    Message response = new ChunkStoredMsg(this.protocolVersion, this.selfID,
                            backupMsg.getFileId(), backupMsg.getChunkNo());
                    Random random = new Random();
                    this.MDBSock.send(response, random.nextInt(401)); //TODO make 401 a static member?
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
