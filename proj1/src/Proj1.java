import java.io.IOException;
import java.net.*;
import java.util.*;

/*
Multicast allows communication between different containers to be used on different
computers (in this case, you should use different terminals for this purpose).
The containers should be specified by a port and an IP address within the range
224.0.0.0 and 239.255.255.255, which are IPs reserved for multicast communications.

Periodically send its IP and port to the multicast group to know that it is
working correctly. Clients should join that group and then send messages with
the desired requests. In this case, you should use MulticastSockets instead
of DatagramSockets to achieve the desired multicast communication.
To do the "register" and "lookup" operations, you should use
the same method that you used in Lab1.

To create a more robust system, I advise you to use more than one thread.
You can use the functions provided by Timer and TimerTask (package “java.util”)
or ScheduledThreadPoolExecutor (package “java.util.concurrent”). The last one
is more sophisticated but also more difficult to implement and use.
*/

public class Proj1 {
    // cmd line arguments
    private final String protocolVersion;
    private final String id;
    private final String accessPoint;
    private final InetAddress MC;
    private final InetAddress MDB;
    private final InetAddress MDR;
    private final int MCPort;
    private final int MDBPort;
    private final int MDRPort;
    // multicast sockets
    private MulticastSocket MCSock, MDBSock, MDRSock;

    public Proj1(String[] args) throws IOException {
        // parse args
        if (args.length != 9) usage();

        this.protocolVersion = args[0];
        this.id = args[1];
        this.accessPoint = args[2];
        // MC
        this.MC = InetAddress.getByName(args[3]);
        this.MCPort = Integer.parseInt(args[4]);
        // MDB
        this.MDB = InetAddress.getByName(args[5]);
        this.MDBPort = Integer.parseInt(args[6]);
        // MDR
        this.MDR = InetAddress.getByName(args[7]);
        this.MDRPort = Integer.parseInt(args[8]);

        this.joinMulticasts();

        System.out.println(this);
        System.out.println("Initialized program.");
    }

    private void joinMulticasts() throws IOException {
        this.MCSock = new MulticastSocket(this.MCPort);
        this.MCSock.joinGroup(this.MC);

        this.MDBSock = new MulticastSocket(this.MDBPort);
        this.MDBSock.joinGroup(this.MDB);

        this.MDRSock = new MulticastSocket(this.MDRPort);
        this.MDRSock.joinGroup(this.MDR);
    }

    public void closeSockets() {
        this.MCSock.close();
        this.MDBSock.close();
        this.MDRSock.close();
    }

    private void mainLoop() {
        try {
            new ChunkBackupMsg("1.0", this.id,
                    "filete", 78, 9)
                    .send(this.MCSock, this.MC, this.MCPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SockThread mcThread = new SockThread(this.MCSock, this.id);
        SockThread mdbThread = new SockThread(this.MDBSock, this.id);
        SockThread mdrThread = new SockThread(this.MDRSock, this.id);

        mcThread.start();
        mdbThread.start();
        mdrThread.start();

        Scanner scanner = new Scanner(System.in);
        String cmd;
        do {
            cmd = scanner.nextLine();
            System.out.println("CMD: " + cmd);
        } while (!cmd.equalsIgnoreCase("EXIT"));

        // shush threads
        mcThread.interrupt();
        mdbThread.interrupt();
        mdrThread.interrupt();
    }

    @Override
    public String toString() {
        return "Protocol version: " + this.protocolVersion + "\n" +
                "Id: " + this.id + "\n" +
                "Service access point: " + this.accessPoint + "\n" +
                "MC: " + this.MC + ":" + this.MCPort + "\n" +
                "MC: " + this.MDB + ":" + this.MDBPort + "\n" +
                "MC: " + this.MDR + ":" + this.MDRPort;
    }

    private static void usage() {
        System.err.println("Usage: java -jar\n\t" +
                "Proj1 <protocol version> <peer id> <service access point>\n\t" +
                "<MC> <MDB> <MDR>");
        System.exit(1);
    }

    public static void main(String[] args) {
        Proj1 prog = null;
        try {
            prog = new Proj1(args);
        } catch (IOException e) {
            System.err.println("Couldn't initialize the program.");
            e.printStackTrace();
            usage();
        }
        assert prog != null;

        prog.mainLoop();
        prog.closeSockets();
    }
}
