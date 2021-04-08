import message.ChunkMsg;
import message.GetChunkMsg;
import message.Message;

import java.util.concurrent.atomic.AtomicBoolean;

public class GetChunkSender extends MessageSender<GetChunkMsg> {
    private static final int MAX_RETRANSMIT = 5;
    private static final long COLLECTION_INTERVAL = 1000; // in ms
    private ChunkMsg response;
    private final AtomicBoolean gotChunk;

    public GetChunkSender(SockThread sockThread, GetChunkMsg message, MessageHandler handler) {
        super(sockThread, message, handler);
        this.gotChunk = new AtomicBoolean(false);
    }

    public byte[] getResponse() {
        return response.getChunk();
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
            this.response = (ChunkMsg) message;
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
