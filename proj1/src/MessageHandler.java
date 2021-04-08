import file.DigestFile;
import message.*;
import state.State;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageHandler {
    private final String selfID;
    private final String protocolVersion;
    private final MessageCreator messageCreator;
    private final SockThread MCSock;
    private final SockThread MDBSock;
    private final SockThread MDRSock;
    private final List<Observer> observers;

    public MessageHandler(String selfID, String protocolVersion, SockThread MCSock, SockThread MDBSock, SockThread MDRSock) {
        this.selfID = selfID;
        this.protocolVersion = protocolVersion;
        this.messageCreator = new MessageCreator(protocolVersion);
        this.MCSock = MCSock;
        this.MDBSock = MDBSock;
        this.MDRSock = MDRSock;
        this.MCSock.setHandler(this);
        this.MDBSock.setHandler(this);
        this.MDRSock.setHandler(this);
        this.observers = new CopyOnWriteArrayList<>();
    }

    public void addObserver(Observer obs) {
        this.observers.add(obs);
    }

    public void rmObserver(Observer obs) {
        this.observers.remove(obs);
    }

    // TODO verify message came from the socket?
    public void handleMessage(byte[] receivedData) {
        int crlfCount = 0;
        int headerCutoff;
        for (headerCutoff = 0; headerCutoff < receivedData.length - 1; ++headerCutoff) {
            if (receivedData[headerCutoff] == 0xD && receivedData[headerCutoff + 1] == 0xA)
                ++crlfCount;
            if (crlfCount == 2)
                break;
        }

        final String[] header = new String(receivedData, 0, headerCutoff - 2).split(" ");
        byte[] body = (receivedData.length <= headerCutoff + 2) ?
                new byte[0] :
                Arrays.copyOfRange(receivedData, headerCutoff + 2, receivedData.length);

        if (header[Message.idField].equals(this.selfID)) {
            // System.out.println("We were the ones that sent this message. Skipping..");
            return;
        }

        // construct the reply
        Message message;
        try {
            message = messageCreator.createMessage(header, body);
        } catch (NoSuchMessage noSuchMessage) {
            System.err.println("No Such message " + header[Message.typeField]);
            return;
        }
        assert message != null;
        System.out.println("Received: " + message);

        // notify observers
        for (Observer obs : this.observers) {
            obs.notify(message);
        }

        try {
            Message response;
            switch (message.getType()) {
                case PutChunkMsg.type:
                    PutChunkMsg backupMsg = (PutChunkMsg) message;
                    // do not handle files we initiated the backup of
                    synchronized (State.st) {
                        if (State.st.isInitiator(backupMsg.getFileId())) break;

                        // always register the existence of this file
                        State.st.addFileEntry(backupMsg.getFileId(), backupMsg.getReplication());

                        // do not store duplicated chunks
                        if (State.st.amIStoringChunk(backupMsg.getFileId(), backupMsg.getChunkNo())) break;
                        // if we surpass storage space
                        if (!State.st.updateStorageSize(backupMsg.getChunk().length)) break;

                        try {
                            DigestFile.writeChunk(backupMsg.getFileId() + File.separator + backupMsg.getChunkNo(),
                                    backupMsg.getChunk(), backupMsg.getChunk().length);
                        } catch (IOException e) {
                            e.printStackTrace();
                            State.st.updateStorageSize(-backupMsg.getChunk().length);
                            return;
                        }
                        State.st.declareChunk(backupMsg.getFileId(), backupMsg.getChunkNo());
                        // Add selfId to map Entry
                        State.st.incrementChunkDeg(backupMsg.getFileId(), backupMsg.getChunkNo(), this.selfID);
                        State.st.setAmStoringChunk(backupMsg.getFileId(), backupMsg.getChunkNo(), true);

                        // unsub MDB when storage is full
                        if (this.protocolVersion.equals("2.0")) {
                            if (State.st.isStorageFull()) this.MDBSock.leave();
                        }
                    }

                    // send STORED reply message
                    response = new StoredMsg(this.protocolVersion, this.selfID,
                            backupMsg.getFileId(), backupMsg.getChunkNo());
                    StoredSender storedSender = new StoredSender(this.MCSock, (StoredMsg) response, this);
                    storedSender.run();
                    break;
                case StoredMsg.type:
                    StoredMsg storedMsg = (StoredMsg) message;
                    synchronized (State.st) {
                        State.st.incrementChunkDeg(storedMsg.getFileId(), storedMsg.getChunkNo(), storedMsg.getSenderId());
                    }
                    break;
                case DeleteMsg.type:
                    DeleteMsg delMsg = (DeleteMsg) message;
                    boolean sendIDeleted;
                    synchronized (State.st) {
                        // delete the file on the file system
                        // also updates state entry and space filled
                        sendIDeleted = DigestFile.deleteFile(delMsg.getFileId());

                        if (this.protocolVersion.equals("2.0")) {
                            // unsub MDB when storage is not full
                            if (!State.st.isStorageFull()) this.MDBSock.join();
                        }
                    }

                    if (this.protocolVersion.equals("2.0") && sendIDeleted) {
                        response = new IDeletedMsg(this.protocolVersion, this.selfID, delMsg.getFileId());
                        IDeletedSender iDeletedSender = new IDeletedSender(this.MCSock, (IDeletedMsg) response,
                                this);
                        iDeletedSender.run();
                    }
                    break;
                case IDeletedMsg.type:
                    if (this.protocolVersion.equals("2.0")) {
                        IDeletedMsg iDeletedMsg = (IDeletedMsg) message;
                        synchronized (State.st) {
                            State.st.removeUndeletedPair(iDeletedMsg.getSenderId(), iDeletedMsg.getFileId());
                        }
                    }
                    break;
                case GetChunkMsg.type:
                    GetChunkMsg getChunkMsg = (GetChunkMsg) message;
                    synchronized (State.st) {
                        if (!State.st.amIStoringChunk(getChunkMsg.getFileId(), getChunkMsg.getChunkNo()))
                            break;
                    }

                    MessageSender<? extends Message> chunkSender;
                    if (this.protocolVersion.equals("2.0")) {
                        response = new ChunkTCPMsg(this.protocolVersion, this.selfID,
                                getChunkMsg.getFileId(), getChunkMsg.getChunkNo());
                        chunkSender = new ChunkTCPSender(this.MDRSock, (ChunkTCPMsg) response, this);
                    } else {
                        response = new ChunkMsg(this.protocolVersion, this.selfID,
                                getChunkMsg.getFileId(), getChunkMsg.getChunkNo());
                        chunkSender = new ChunkSender(this.MDRSock, (ChunkMsg) response, this);
                    }
                    chunkSender.run();
                    break;
                case ChunkMsg.type:
                    break;
                case RemovedMsg.type:
                    RemovedMsg removedMsg = (RemovedMsg) message;
                    int repDegree;
                    boolean amInitiator;
                    synchronized (State.st) {
                        State.st.decrementChunkDeg(removedMsg.getFileId(), removedMsg.getChunkNo(), removedMsg.getSenderId());
                        if (State.st.isChunkOk(removedMsg.getFileId(), removedMsg.getChunkNo())) break;
                        // we can only serve a chunk if:
                        // we are storing it or we are the initiator
                        amInitiator = State.st.isInitiator(removedMsg.getFileId());
                        if (!State.st.amIStoringChunk(removedMsg.getFileId(), removedMsg.getChunkNo()) &&
                                !amInitiator)
                            break;
                        repDegree = State.st.getFileDeg(removedMsg.getFileId());
                    }

                    byte[] chunk;
                    if (amInitiator) {
                        chunk = DigestFile.divideFileChunk(State.st.getFileInfo(removedMsg.getFileId()).getFilePath(),
                                removedMsg.getChunkNo());
                    } else {
                        chunk = DigestFile.readChunk(removedMsg.getFileId() + File.separator + removedMsg.getChunkNo());
                    }

                    PutChunkMsg putChunkMsg = new PutChunkMsg(this.protocolVersion, this.selfID,
                            removedMsg.getFileId(), removedMsg.getChunkNo(), repDegree, chunk);
                    RemovedPutchunkSender removedPutchunkSender = new RemovedPutchunkSender(this.MDBSock, putChunkMsg,
                            this);
                    removedPutchunkSender.run();
                    break;
                default:
                    // unreachable
                    break;
            }
        } catch (
                Exception e) {
            e.printStackTrace();
            System.err.println("Failed constructing reply for " + message.getType());
        }

        // See if guy who sents the message has to remove
        if (this.protocolVersion.equals("2.0")) {
            Set<String> files = State.st.getFilesUndeletedByPeer(message.getSenderId());
            if (files != null) {
                for (String fileId : files) {
                    DeleteMsg deleteMsg = new DeleteMsg(this.protocolVersion, this.selfID, fileId);
                    this.MCSock.send(deleteMsg);
                }
            }
        }
    }
}
