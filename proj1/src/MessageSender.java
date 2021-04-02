import message.Message;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MessageSender<T extends Message> implements Runnable, Observer {
    protected SockThread sockThread;
    protected Queue<Message> receivedMessages;
    protected T message;
    protected final AtomicBoolean success;

    public MessageSender(SockThread sockThread, T message, MessageHandler handler) {
        this.sockThread = sockThread;
        this.message = message;
        this.receivedMessages = new ConcurrentLinkedQueue<>();
        this.success = new AtomicBoolean(false);
        handler.addObserver(this);
    }

    public void addMessage(Message message) {
        receivedMessages.add(message);
    }

    public void send() {
        this.sockThread.send(this.message);
    }
}
