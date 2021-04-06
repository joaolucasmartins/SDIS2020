import message.Message;
import message.PutChunkMsg;
import message.StoredMsg;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PutChunkSender extends MessageSender<PutChunkMsg> {
    private static final int MAX_RETRANSMIT = 5;
    private static final long COLLECTION_INTERVAL = 1000; // in ms
    private final Queue<Message> receivedMessages;
    public PutChunkSender(SockThread sockThread, PutChunkMsg msg, MessageHandler handler) {
        super(sockThread, msg, handler);
        this.receivedMessages = new ConcurrentLinkedQueue<>();
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

    @Override
    public void notify(Message message) {
        if (checkIfStored(message)) {
            addMessage(message);
        }
    }

    @Override
    public void run() {
        int storedCnt = 0;
        for (int i=0; i < MAX_RETRANSMIT; ++i) {
            super.send();
            try {
                Thread.sleep((long) (COLLECTION_INTERVAL * Math.pow(2, i)));
            } catch (InterruptedException e) {
                this.success.set(false);
                return; // XAU
            }

            while (this.receivedMessages.poll() != null)
                ++storedCnt;

            if (storedCnt >= this.message.getReplication()) {
                this.success.set(true);
                return;
            }
        }
        this.success.set(false);

        this.xau();
    }
}
