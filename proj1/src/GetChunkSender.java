import message.ChunkMsg;
import message.GetChunkMsg;
import message.Message;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GetChunkSender extends MessageSender<GetChunkMsg> {
    private static final int MAX_RETRANSMIT = 5;
    private static final long COLLECTION_INTERVAL = 1; // in secs
    private ChunkMsg response;
    private final AtomicBoolean gotChunk;
    private final ScheduledExecutorService threadPool;
    private volatile int i;

    public AtomicBoolean isDone;

    public GetChunkSender(SockThread sockThread, GetChunkMsg message, MessageHandler handler, ScheduledExecutorService threadPool) {
        super(sockThread, message, handler);
        this.gotChunk = new AtomicBoolean(false);
        this.threadPool = threadPool;
        this.i = 0;
        this.isDone = new AtomicBoolean(false);
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

    public void restart() {
        super.send();
        this.threadPool.schedule(this,
                (long) (COLLECTION_INTERVAL * Math.pow(2, this.i)), TimeUnit.SECONDS);
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
        if (gotChunk.get()) {
            this.success.set(true);
            this.isDone.set(true);
            return;
        }

        ++this.i;
        if (this.i == MAX_RETRANSMIT) {
            this.isDone.set(true);
            return;
        }

        this.restart();
    }
}
