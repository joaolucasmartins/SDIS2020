import message.ChunkMsg;
import message.Message;

import java.util.Random;

public class ChunkSender extends MessageSender<ChunkMsg> {
    private final static int MAX_TIMEOUT=400;
    private boolean chunkAlreadySent;
    public ChunkSender(SockThread sockThread, ChunkMsg message, MessageHandler handler) {
        super(sockThread, message, handler);
        this.chunkAlreadySent = false;
    }

    @Override
    public void notify(Message message) {
        if (refersToSameChunk(message))
            chunkAlreadySent = true;
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
    public void run() {
        Random random = new Random();
        int timeout = random.nextInt(MAX_TIMEOUT + 1);
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if (!chunkAlreadySent)
                            ChunkSender.super.send();
                    }
                },
                timeout
        );
    }
}
