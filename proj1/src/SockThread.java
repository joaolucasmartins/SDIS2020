import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SockThread implements Runnable {
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final MulticastSocket sock;
    private final String selfID;

    public SockThread(MulticastSocket sock, String selfID) {
        this.sock = sock;
        this.selfID = selfID;
    }

    public void interrupt() {
        running.set(false);
    }

    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            byte[] packetData = new byte[64000 + 1000];
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length);

            try {
                this.sock.receive(packet);
            } catch (SocketException e) {
                // happens is the blocking call is interupted
                break;
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received: " + received);

            String[] receivedFields = received.split(" ");
            if (receivedFields[Message.idField].equals(this.selfID)) {
                System.out.println("We were the ones that sent this message. Skipping..");
            }
        }
    }
}
