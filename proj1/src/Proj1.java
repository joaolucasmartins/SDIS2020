import File.DigestFile;
import Message.PutChunkMsg;
import Message.GetChunkMsg;
import Message.RemovedMsg;
import Message.DeleteMsg;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Proj1 implements TestInterface, Observer {
    private int maxDiskSpaceKB = -1;  // -1 means no limit
    // cmd line arguments
    private final String protocolVersion;
    private final String id;
    private final String accessPoint;
    // multicast sockets
    private final SockThread MCSock;
    private final SockThread MDBSock;
    private final SockThread MDRSock;
    private final MessageHandler messageHandler;

    public static Registry registry = null;
    public static String rmiName = null;

    public String getAccessPointName() {
        return this.accessPoint;
    }

    public Proj1(String[] args) throws IOException {
        // parse args
        if (args.length != 9) usage();

        this.protocolVersion = args[0];
        this.id = args[1];
        // set the file dir name for the rest of the program (create it if missing)
        DigestFile.setFileDir(this.id);
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

        this.messageHandler = new MessageHandler(this.id, this.protocolVersion,
                this.MCSock, this.MDBSock, this.MDRSock);

        System.out.println(this);
        System.out.println("Initialized program.");
    }

    private SockThread createSocketThread(InetAddress addr, Integer port) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(addr);
        return new SockThread(socket, addr, port);
    }

    public void cleanup() {
        this.MCSock.close();
        this.MDBSock.close();
        this.MDRSock.close();
        this.messageHandler.saveMap();

        // cleanup the access point
        if (Proj1.registry != null) {
            try {
                Proj1.registry.unbind(Proj1.rmiName);
                UnicastRemoteObject.unexportObject(this, false);
            } catch (Exception e) {
                System.err.println("Failed to unbind from rmi registry.");
            }
        }
    }

    private void mainLoop() {
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
                    DigestFile.divideFile("filename.txt", 3);
                    this.MDBSock.send(
                            new PutChunkMsg("1.0", this.id,
                                    DigestFile.getHash("filename.txt"),
                                    0, 9, "filename.txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (cmd.equals("getchunk")) {
                this.MDBSock.send(
                        new GetChunkMsg("1.0", this.id,
                                "0fe051a9f8f8de449f1b251d5ad4c78e62d5ff9393b7d9eb3e577e394354f4b4",
                                0));

            } else if (cmd.equals("removed")) {
                this.MDBSock.send(
                        new RemovedMsg("1.0", this.id,
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
        TestInterface stub = null;
        try {
            stub = (TestInterface) UnicastRemoteObject.exportObject(prog, 0);
            String[] rmiinfoSplit = prog.getAccessPointName().split(":");
            Proj1.rmiName = rmiinfoSplit[0];
            if (rmiinfoSplit.length > 1)
                Proj1.registry = LocateRegistry.getRegistry("localhost", Integer.parseInt(rmiinfoSplit[1]));
            else
                Proj1.registry = LocateRegistry.getRegistry();
            Proj1.registry.bind(Proj1.rmiName, stub);
        } catch (Exception e) {
            System.err.println("Failed setting up the access point for use by the testing app.");
            // e.printStackTrace();
        }

        // trap sigint
        Proj1 finalProg = prog;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.err.println("Exiting gracefully..");
                finalProg.cleanup();
            }
        }));

        prog.mainLoop();
        System.exit(0);
    }

    /* USED BY THE TestApp (RMI) */
    @Override
    public String backup(String filePath, int replicationDegree) throws RemoteException {
        String hash;
        try {
            DigestFile.divideFile(filePath, replicationDegree);
            hash = DigestFile.getHash(filePath);
        } catch (IOException e) {
            throw new RemoteException("Couldn't divide file " + filePath);
        }
        this.messageHandler.verifyRepDegree(hash);
        return "asd";
    }

    @Override
    public String restore(String filePath) throws RemoteException {
        try {
            String fileHash = DigestFile.getHash(filePath);
            int chunkNo = DigestFile.getChunkCount(filePath);
            if (chunkNo < 0) return "File " + filePath + " is too big";
            for (int currChunk = 0; currChunk < chunkNo; ++currChunk) {
                if (DigestFile.hasChunk(fileHash, currChunk)) continue;
                GetChunkMsg msg = new GetChunkMsg(this.protocolVersion, this.id, fileHash, 0);
                this.MCSock.send(msg);
            }
            return "Restored file " + filePath + " with hash " + fileHash + ".";
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException();
        }
        // return "restore";
    }

    @Override
    public String delete(String filePath) throws RemoteException {
        try {
            String fileHash = DigestFile.getHash(filePath);
            DeleteMsg msg = new DeleteMsg(this.protocolVersion, this.id, fileHash);
            this.MCSock.send(msg);
            // TODO repetir while replication != 0 (atencao se temos o chunk connosco ou nao (reclaim))
            return "Deleted file " + filePath + " with hash " + fileHash + ".";
        } catch (IOException e) {
            e.printStackTrace();
            throw new RemoteException();
        }
        // return "Deletion of " + filePath + " failed.";
    }

    // force == true => ignore if the the replication degree becomes 0
    private int trimFiles(int capactityToTrim, boolean force) {
        int capacityDelta = capactityToTrim;

        for (var entry : DigestFile.replicationDegMap.entrySet()) {
            String fileId = entry.getKey();
            // int desiredRep = entry.getValue().p1;

            for (var chunkEntry : entry.getValue().p2.entrySet()) {
                int chunkNo = chunkEntry.getKey();
                int currRepl = chunkEntry.getValue();
                if ((force || currRepl > 1) && currRepl > 0) {
                    RemovedMsg removedMsg = new RemovedMsg(this.protocolVersion, this.id, fileId, chunkNo);
                    this.MCSock.send(removedMsg);
                    DigestFile.decreaseChunkDeg(fileId, chunkNo);
                    capacityDelta -= DigestFile.getChunkSize(fileId, chunkNo);
                }
                if (capacityDelta <= 0) break;
            }
            if (capacityDelta <= 0) break;
        }

        return capacityDelta;
    }

    @Override
    public String reclaim(int maxCapacity) throws RemoteException {
        int capacityDelta = maxCapacity - this.maxDiskSpaceKB;
        this.maxDiskSpaceKB = maxCapacity;
        if (capacityDelta >= 0)  // if max capacity is unchanged or increases, we don't need to do anything
            return "Max disk space set to " + maxCapacity + " KBytes.";

        // remove things (trying to keep everything above 0 replication degree)
        capacityDelta = trimFiles(capacityDelta, false);
        if (capacityDelta > 0) trimFiles(capacityDelta, true);

        return "reclaim";
    }

    @Override
    public String state() throws RemoteException {
        return this.toString();
    }

    @Override
    public void notify(String notification) {
        System.err.println(notification);
    }
}
