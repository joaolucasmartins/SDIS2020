import message.ChunkMsg;
import message.ChunkTCPMsg;
import message.GetChunkMsg;
import message.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class GetChunkTCPSender extends MessageSender<GetChunkMsg> {
    private static final int MAX_RETRANSMIT = 5;
    private static final long COLLECTION_INTERVAL = 1000; // in ms
    private ChunkTCPMsg chunkTCPMsg;
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
        if (message.getType().equals(ChunkMsg.type)) {
            ChunkMsg chunkMsg = (ChunkMsg) message;
            return chunkMsg.getChunkNo() == this.message.getChunkNo() &&
                    chunkMsg.getFileId().equals(this.message.getFileId());
        }
        return false;
    }

    @Override
    public void notify(Message message) {
        if (refersToSameChunk(message)) {
            this.gotChunk.set(true);
            this.chunkTCPMsg = (ChunkTCPMsg) message;
            Socket socket;
            try {
                socket = new Socket(chunkTCPMsg.getIp(), chunkTCPMsg.getTcpPort());
            } catch (IOException e) {
                System.err.println("Failed to open TCP socket (GetChunkTCP)");
                this.xau();
                return;
            }
            try {
                this.response = socket.getInputStream().readAllBytes();
            } catch (IOException e) {
                System.err.println("Failed to read from TCP socket (GetChunkTCP)");
                try {
                    socket.close();
                } catch (IOException ioException) {
                    System.err.println("Failed to close from TCP socket (GetChunkTCP)");
                    this.xau();
                    return;
                }
            }
            this.xau();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < MAX_RETRANSMIT; ++i) {
            super.send();

            try {
                Thread.sleep((long) (COLLECTION_INTERVAL * Math.pow(2, i)));
            } catch (InterruptedException e) {
                return;
            }

            if (gotChunk.get()) {
                this.success.set(true);
                return;
            }
        }
    }
}
