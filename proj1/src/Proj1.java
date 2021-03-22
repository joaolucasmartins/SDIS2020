import File.DigestFile;
import Message.ChunkBackupMsg;
import Message.GetChunkMsg;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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

public class Proj1 implements TestInterface {
    // cmd line arguments
    private final String protocolVersion;
    private final String id;
    private final String accessPoint;
    // multicast sockets
    private final SockThread MCSock;
    private final SockThread MDBSock;
    private final SockThread MDRSock;

    public String getAccessPointName() {
        return this.accessPoint;
    }

    public Proj1(String[] args) throws IOException {
        // parse args
        if (args.length != 9) usage();

        this.protocolVersion = args[0];
        this.id = args[1];
        this.accessPoint = args[2];
        // MC
        InetAddress MC = InetAddress.getByName(args[3]);
        Integer MCPort = Integer.parseInt(args[4]);
        this.MCSock = this.createSocketThread(MC, MCPort);
        // MDB
        InetAddress MDB = InetAddress.getByName(args[5]);
        Integer MDBPort = Integer.parseInt(args[6]);
        this.MDBSock = this.createSocketThread(MDB, MDBPort);
        // MDR
        InetAddress MDR = InetAddress.getByName(args[7]);
        Integer MDRPort = Integer.parseInt(args[8]);
        this.MDRSock = this.createSocketThread(MDR, MDRPort);

        System.out.println(this);
        System.out.println("Initialized program.");
    }

    private SockThread createSocketThread(InetAddress addr, Integer port) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(addr);
        return new SockThread(socket, addr, port);
    }

    public void closeSockets() {
        this.MCSock.close();
        this.MDBSock.close();
        this.MDRSock.close();
    }

    private void mainLoop() {
        MessageHandler handler = new MessageHandler(this.id, this.protocolVersion,
                this.MCSock, this.MDBSock, this.MDRSock);

        this.MCSock.start();
        this.MDBSock.start();
        this.MDBSock.start();
        Scanner scanner = new Scanner(System.in);
        String cmd;
        do {
            cmd = scanner.nextLine();
            System.out.println("CMD: " + cmd);
            if (cmd.equals("putchunk")) {
                try {
                    DigestFile.divideFile("filename.txt");
                    this.MDBSock.send(
                    new ChunkBackupMsg("1.0", this.id,
                            DigestFile.getHash("filename.txt"), 0, 9, "filename.txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (cmd.equals("getchunk")) {
                this.MDBSock.send(
                        new GetChunkMsg("1.0", this.id,
                                "0fe051a9f8f8de449f1b251d5ad4c78e62d5ff9393b7d9eb3e577e394354f4b4",
                                0));

            }
        } while (!cmd.equalsIgnoreCase("EXIT"));

        // shush threads
        this.MCSock.interrupt();
        this.MDBSock.interrupt();
        this.MDRSock.interrupt();
    }

    @Override
    public String toString() {
        return "Protocol version: " + this.protocolVersion + "\n" +
                "Id: " + this.id + "\n" +
                "Service access point: " + this.accessPoint + "\n" +
                "MC: " + this.MCSock +
                "MDB: " + this.MDBSock +
                "MDR: " + this.MDRSock;
    }

    private static void usage() {
        System.err.println("Usage: java -jar\n\t" +
                "Proj1 <protocol version> <peer id> <service access point>\n\t" +
                "<MC> <MDB> <MDR>");
        System.exit(1);
    }

    public static void main(String[] args) {
        // parse cmdline args
        Proj1 prog = null;
        try {
            prog = new Proj1(args);
        } catch (IOException e) {
            System.err.println("Couldn't initialize the program.");
            e.printStackTrace();
            usage();
        }
        assert prog != null;

        // setup the access point
        try {
            TestInterface stub = (TestInterface) UnicastRemoteObject.exportObject(prog, 0);
            String[] rmiinfoSplit = prog.getAccessPointName().split(":");
            Registry registry;
            if (rmiinfoSplit.length > 1) {

                System.out.println("connecting to " + Integer.parseInt(rmiinfoSplit[1]));
                registry = LocateRegistry.getRegistry("localhost", Integer.parseInt(rmiinfoSplit[1]));
            }
            else
                registry = LocateRegistry.getRegistry();
            registry.bind(rmiinfoSplit[0], stub);
        } catch (Exception e) {
            System.err.println("Setting up the access point for testing failed.");
            e.printStackTrace();
        }

        prog.mainLoop();
        prog.closeSockets();
    }

    /* USED BY THE TestApp (RMI) */
    @Override
    public String backup(String filePath, int replicationDegree) throws RemoteException {
        return "backup";
    }

    @Override
    public String restore(String filePath) throws RemoteException {
        return "restore";
    }

    @Override
    public String delete(String filePath) throws RemoteException {
        return "delete";
    }

    @Override
    public String reclaim(int maxCapacity) throws RemoteException {
        return "reclaim";
    }

    @Override
    public String state() throws RemoteException {
        return this.toString();
    }
}
