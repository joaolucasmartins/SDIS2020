import message.Message;
import message.PutChunkMsg;
import message.StoredMsg;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PutChunkSender extends MessageSender<PutChunkMsg> {
    private static final int MAX_RETRANSMIT = 5;
    private static final long COLLECTION_INTERVAL = 1; // in seconds
    private final Queue<Message> receivedMessages;
    private final ScheduledExecutorService threadPool;
    private int i;
    private int storedCnt;

    public PutChunkSender(SockThread sockThread, PutChunkMsg msg, MessageHandler handler, ScheduledExecutorService threadPool) {
        super(sockThread, msg, handler);
        this.receivedMessages = new ConcurrentLinkedQueue<>();
        this.threadPool = threadPool;
        this.i = 0;
        this.storedCnt = 0;
    }

    public void addMessage(Message message) {
        receivedMessages.add(message);
    }

    private boolean checkIfStored(Message message) {
        if (message.getType().equals(StoredMsg.type)) {
            StoredMsg storedMsg = (StoredMsg) message;
            boolean fileIdEqual = storedMsg.getFileId().equals(this.message.getFileId()),
                    chunkNoEqual = storedMsg.getChunkNo().equals(this.message.getChunkNo());
            return fileIdEqual && chunkNoEqual;
        }
        return false;
    }

    public void restart() {
        super.send();
        this.threadPool.schedule(this, (long) (COLLECTION_INTERVAL * Math.pow(2, this.i)),
                TimeUnit.SECONDS);
    }

    @Override
    public void notify(Message message) {
        if (checkIfStored(message)) {
            addMessage(message);
        }
    }

    @Override
    public void run() {
        // count the number of stored messages
        while (this.receivedMessages.poll() != null)
            ++storedCnt;

        if (storedCnt >= this.message.getReplication()) {
            this.xau();
            this.success.set(true);
            return;
        }

        ++this.i;
        if (this.i == MAX_RETRANSMIT) {
            this.xau();
            return;
        }

        this.restart();
    }
}
