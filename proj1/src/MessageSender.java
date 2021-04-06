import message.Message;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MessageSender<T extends Message> implements Runnable, Observer {
    protected SockThread sockThread;
    protected T message;
    protected final AtomicBoolean success;
    private final MessageHandler handler;

    public MessageSender(SockThread sockThread, T message, MessageHandler handler) {
        this.sockThread = sockThread;
        this.message = message;
        this.success = new AtomicBoolean(false);
        this.handler = handler;
        this.handler.addObserver(this);
    }

    protected void xau() {
        this.handler.rmObserver(this);
    }

    protected void send() {
        this.sockThread.send(this.message);
    }
}
