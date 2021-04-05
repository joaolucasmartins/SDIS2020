import message.Message;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MessageSender<T extends Message> implements Runnable, Observer {
    protected SockThread sockThread;
    protected T message;
    protected final AtomicBoolean success;

    public MessageSender(SockThread sockThread, T message, MessageHandler handler) {
        this.sockThread = sockThread;
        this.message = message;
        this.success = new AtomicBoolean(false);
        handler.addObserver(this);
    }

    protected void send() {
        this.sockThread.send(this.message);
    }
}
