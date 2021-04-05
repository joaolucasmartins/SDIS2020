import file.DigestFile;
import message.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static message.MessageCreator.createMessage;

public class MessageHandler {
    public final int maxBackofMs = 401;
    private final String selfID;
    private final String protocolVersion;
    private final SockThread MCSock;
    private final SockThread MDBSock;
    private final SockThread MDRSock;
    private List<Observer> observers;

    public MessageHandler(String selfID, String protocolVersion, SockThread MCSock, SockThread MDBSock, SockThread MDRSock) {
        this.selfID = selfID;
        this.protocolVersion = protocolVersion;
        this.MCSock = MCSock;
        this.MDBSock = MDBSock;
        this.MDRSock = MDRSock;
        this.MCSock.setHandler(this);
        this.MDBSock.setHandler(this);
        this.MDRSock.setHandler(this);
        this.observers = new CopyOnWriteArrayList<>();
        DigestFile.importMap();
    }

    public void addObserver(Observer obs) {
        this.observers.add(obs);
    }

    public void rmObserver(Observer obs) {
        this.observers.remove(obs);
    }

    public void saveMap() {
        try {
            DigestFile.exportMap();
        } catch (IOException e) {
            e.printStackTrace(); // TODO handle this?
        }
    }

    private boolean hasSpace(int newSize) {
        return DigestFile.state.getMaxDiskSpaceB() < 0 ||
                (DigestFile.getStorageSize() + newSize <= DigestFile.state.getMaxDiskSpaceB());
    }

    public void handleMessage(SockThread sock, String received) {
        final String[] receivedFields = received.split(Message.CRLF, 3);
        final String[] header = receivedFields[0].split(" ");
        final String body = (receivedFields.length > 2) ? receivedFields[2] : null;

        if (header[Message.idField].equals(this.selfID)) {
            System.out.println("We were the ones that sent this message. Skipping..");
            return;
        }

        // construct the reply
        Message message = null;
        try {
            message = createMessage(header, body);
        } catch (NoSuchMessage noSuchMessage) {
            System.err.println("No Such message " + header[Message.typeField]);
            return;
        }
        assert message != null;

        // notify observers
        for (Observer obs : this.observers) {
            obs.notify(message);
        }

        try {
            Message response;
            switch (message.getType()) {
                case PutChunkMsg.type:
                    PutChunkMsg backupMsg = (PutChunkMsg) message;
                    // do not store duplicated chunks
                    if (DigestFile.hasChunk(backupMsg.getFileId(), backupMsg.getChunkNo())) break;
                    // if we surpass storage space
                    if (!this.hasSpace(backupMsg.getChunk().length)) break;

                    try {
                        DigestFile.writeChunk(backupMsg.getFileId() + File.separator + backupMsg.getChunkNo(),
                                backupMsg.getChunk(), backupMsg.getChunk().length);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    DigestFile.state.addFileEntry(backupMsg.getFileId(), false, backupMsg.getReplication());
                    DigestFile.state.incrementChunkDeg(backupMsg.getFileId(), backupMsg.getChunkNo());
                    // send STORED reply message
                    response = new StoredMsg(this.protocolVersion, this.selfID,
                            backupMsg.getFileId(), backupMsg.getChunkNo());

                    StoredSender storedSender = new StoredSender(this.MCSock, (StoredMsg) response, this);
                    storedSender.run(); // TODO make this part of thread pool
                    // unsub MDB when storage is full
                    if (DigestFile.getStorageSize() == DigestFile.state.getMaxDiskSpaceB()) this.MDBSock.leave();
                    break;
                case StoredMsg.type:
                    StoredMsg storedMsg = (StoredMsg) message;
                    DigestFile.state.incrementChunkDeg(storedMsg.getFileId(), storedMsg.getChunkNo());
                    break;
                case DeleteMsg.type:
                    DeleteMsg delMsg = (DeleteMsg) message;
                    DigestFile.deleteFile(delMsg.getFileId());

                    // sub MDB when storage is not full
                    if (DigestFile.getStorageSize() < DigestFile.state.getMaxDiskSpaceB())
                        this.MDBSock.join();
                    return;  // IMP file deletion doesn't send a reply
                case GetChunkMsg.type:
                    GetChunkMsg getChunkMsg = (GetChunkMsg) message;
                    if (DigestFile.hasChunk(getChunkMsg.getFileId(), getChunkMsg.getChunkNo())) {
                        response = new ChunkMsg(this.protocolVersion, this.selfID,
                                getChunkMsg.getFileId(), getChunkMsg.getChunkNo());
                        ChunkSender chunkSender = new ChunkSender(this.MDRSock, (ChunkMsg) response, this);
                        chunkSender.run(); // TODO make this part of thread pool
                    }
                    break;
                case ChunkMsg.type:
                    break;
                case RemovedMsg.type: // TODO Remove this
                    RemovedMsg removedMsg = (RemovedMsg) message;
                    DigestFile.state.decrementChunkDeg(removedMsg.getFileId(), removedMsg.getChunkNo());
                    if (DigestFile.hasChunk(removedMsg.getFileId(), removedMsg.getChunkNo()) &&
                            !DigestFile.chunkIsOk(removedMsg.getFileId(), removedMsg.getChunkNo())) {

                        int repDegree = DigestFile.state.getChunkDeg(removedMsg.getFileId(), removedMsg.getChunkNo());
                        byte[] chunk = DigestFile.readChunk(removedMsg.getFileId(), removedMsg.getChunkNo());
                        PutChunkMsg putChunkMsg = new PutChunkMsg(this.protocolVersion, this.selfID,
                                removedMsg.getFileId(), removedMsg.getChunkNo(), repDegree, chunk);
                        RemovedPutchunkSender removedPutchunkSender = new RemovedPutchunkSender(this.MDBSock, putChunkMsg, this);
                        removedPutchunkSender.run();
                    }
                    // TODO initiate the chunk backup subprotocol after random delay
                    // TODO if during this time, we get a PUTCHUNK for this chunk => back off
                    break;
                default:
                    // unreachable
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed constructing reply for " + message.getType());
        }
    }
}
