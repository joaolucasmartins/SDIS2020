import message.Message;
import message.PutChunkMsg;
import message.StoredMsg;
import state.State;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PutChunkSender extends MessageSender<PutChunkMsg> {
    private static final int MAX_RETRANSMIT = 5;
    private static final long COLLECTION_INTERVAL = 1000; // in ms
    private final Queue<Message> receivedMessages;
    private int i;
    private int storedCnt;
    // used to reschedule himself // TODO pass threadpool to use
    private final ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

    public PutChunkSender(SockThread sockThread, PutChunkMsg msg, MessageHandler handler) {
        super(sockThread, msg, handler);
        this.receivedMessages = new ConcurrentLinkedQueue<>();
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
        this.threadPool.schedule(this,
                (long) (COLLECTION_INTERVAL * Math.pow(2, this.i)), TimeUnit.MILLISECONDS);
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
