import message.ChunkMsg;
import message.ChunkTCPMsg;
import message.GetChunkMsg;
import message.Message;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class GetChunkTCPSender extends MessageSender<GetChunkMsg> {
    private static final int MAX_RETRANSMIT = 5;
    private static final long COLLECTION_INTERVAL = 1000; // in ms
    private Message chunkTCPMsg;
    private byte[] response;
    private final AtomicBoolean gotChunk;

    public GetChunkTCPSender(SockThread sockThread, GetChunkMsg message, MessageHandler handler) {
        super(sockThread, message, handler);
        this.gotChunk = new AtomicBoolean(false);
    }

    public byte[] getResponse() {
        return response;
    }

    private boolean refersToSameChunk(Message message) {
        if (message.getType().equals(ChunkTCPMsg.type)) {
            if (message.getVersion().equals("2.0")) {
                ChunkTCPMsg chunkMsg = (ChunkTCPMsg) message;
                return chunkMsg.getChunkNo() == this.message.getChunkNo() &&
                        chunkMsg.getFileId().equals(this.message.getFileId());
            } else {
                ChunkMsg chunkMsg = (ChunkMsg) message;
                return chunkMsg.getChunkNo() == this.message.getChunkNo() &&
                        chunkMsg.getFileId().equals(this.message.getFileId());
            }
        }
        return false;
    }

    private boolean handleTcpVersion() {
        ChunkTCPMsg chunkTCPMsg = (ChunkTCPMsg) this.chunkTCPMsg;

        Socket socket;
        try {
            socket = new Socket(InetAddress.getByName(chunkTCPMsg.getIp()), chunkTCPMsg.getTcpPort());
        } catch (IOException e) {
            System.err.println("Failed to open TCP socket (GetChunkTCP)");
            return false;
        }

        boolean isGood = true;
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            this.response = inputStream.readAllBytes();
        } catch (IOException e) {
            System.err.println("Failed to read from TCP socket (GetChunkTCP)");
            isGood = false;
        }
        try {
            socket.close();
        } catch (IOException ioException) {
            System.err.println("Failed to close from TCP socket (GetChunkTCP)");
            isGood = false;
        }

        return isGood;
    }

    private boolean handleNormalVersion() {
        this.response = ((ChunkMsg) this.chunkTCPMsg).getChunk();
        return true;
    }

    @Override
    public void notify(Message message) { // TODO timeout
        if (refersToSameChunk(message)) {
            this.chunkTCPMsg = message;
            boolean success = this.chunkTCPMsg.getVersion().equals("2.0") ?
                    handleTcpVersion() :
                    handleNormalVersion();

            if (success) {
                this.gotChunk.set(true);
                this.xau();
            }
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < MAX_RETRANSMIT; ++i) {
            super.send();

            try {
                Thread.sleep((long) (COLLECTION_INTERVAL * Math.pow(2, i)));
            } catch (InterruptedException e) {
                this.xau();
                return;
            }

            if (this.gotChunk.get()) {
                this.success.set(true);
                return;
            }
        }
    }
}
