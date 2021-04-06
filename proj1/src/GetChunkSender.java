import message.ChunkMsg;
import message.GetChunkMsg;
import message.Message;

import java.util.concurrent.Callable;

public class GetChunkSender extends MessageSender<GetChunkMsg> {
    private static final int MAX_RETRANSMIT = 5;
    private static final long COLLECTION_INTERVAL = 1000; // in ms
    private boolean gotChunk;
    private ChunkMsg response;
    public GetChunkSender(SockThread sockThread, GetChunkMsg message, MessageHandler handler) {
        super(sockThread, message, handler);
        gotChunk = false;
    }

    public ChunkMsg getResponse() {
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
            gotChunk = true;
            this.response = (ChunkMsg) message;
            this.xau();
        }
    }

    @Override
    public void run() {
        for (int i=0; i < MAX_RETRANSMIT; ++i) {
            super.send();
            try {
                Thread.sleep((long) (COLLECTION_INTERVAL * Math.pow(2, i)));
            } catch (InterruptedException e) {
                this.success.set(false);
                return; // XAU
            }

            if (gotChunk) {
                this.success.set(true);
                return;
            }
        }
        this.success.set(false);
    }
}
