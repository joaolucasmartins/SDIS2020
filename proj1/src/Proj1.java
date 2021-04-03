import file.DigestFile;
import message.PutChunkMsg;
import message.GetChunkMsg;
import message.RemovedMsg;
import message.DeleteMsg;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Proj1 implements TestInterface {
    public static int maxDiskSpaceB = -1;  // -1 means no limit
    // cmd line arguments
    private final String protocolVersion;
    private final String id;
    private final String accessPoint;
    // multicast sockets
    private final SockThread MCSock;
    private final SockThread MDBSock;
    private final SockThread MDRSock;
    private final MessageHandler messageHandler;


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
        return new SockThread(socket, addr, port);
    }

    public void cleanup() {
        this.MCSock.close();
        this.MDBSock.close();
        this.MDRSock.close();
        this.messageHandler.saveMap();
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
            if (cmd.equalsIgnoreCase("putchunk")) {
                try {
                    DigestFile.divideFile("filename.txt", 3);
                    PutChunkMsg putChunkMsg = new PutChunkMsg("1.0", this.id,
                            DigestFile.getHash("filename.txt"), 0, 9,
                            "filename.txt");
                    PutChunkSender putChunkSender = new PutChunkSender(this.MDBSock, putChunkMsg, this.messageHandler);
                    Thread t = new Thread(putChunkSender);
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        System.out.println("!!!!:warning:!!!!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (cmd.equalsIgnoreCase("getchunk")) {
                this.MDBSock.send(
                        new GetChunkMsg("1.0", this.id,
                                "0fe051a9f8f8de449f1b251d5ad4c78e62d5ff9393b7d9eb3e577e394354f4b4",
                                0));

            } else if (cmd.equalsIgnoreCase("removed")) {
                this.MDBSock.send(
                        new RemovedMsg("1.0", this.id,
                                "0fe051a9f8f8de449f1b251d5ad4c78e62d5ff9393b7d9eb3e577e394354f4b4",
                                0));

            } else if (cmd.equalsIgnoreCase("reclaim")) {
                try {
                    this.reclaim(0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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

        // trap sigint
        Proj1 finalProg = prog;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Exiting gracefully..");
            finalProg.cleanup();
        }));

        // setup the access point
        TestInterface stub;
        Registry registry = null;
        String rmiName = null;
        try {
            stub = (TestInterface) UnicastRemoteObject.exportObject(prog, 0);
            String[] rmiinfoSplit = prog.getAccessPointName().split(":");
            rmiName = rmiinfoSplit[0];
            if (rmiinfoSplit.length > 1)
                registry = LocateRegistry.getRegistry("localhost", Integer.parseInt(rmiinfoSplit[1]));
            else
                registry = LocateRegistry.getRegistry();
            registry.bind(rmiName, stub);
        } catch (Exception e) {
            System.err.println("Failed setting up the access point for use by the testing app.");
            // e.printStackTrace();
        }

        prog.mainLoop();
        prog.cleanup();

        // cleanup the access point
        if (registry != null) {
            try {
                registry.unbind(rmiName);
                UnicastRemoteObject.unexportObject(prog, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* USED BY THE TestApp (RMI) */
    @Override
    public String backup(String filePath, int replicationDegree) throws RemoteException {
        String hash;
        try {
            // TODO NAO ESCREVE CHUNKS, APENAS LER
            DigestFile.divideFile(filePath, replicationDegree);
            hash = DigestFile.getHash(filePath);
        } catch (IOException e) {
            throw new RemoteException("Couldn't divide file " + filePath);
        }
        for (Integer chunkNo: DigestFile.getChunksBellowRep(hash)) {

        }
        return "asd";
    }

    @Override
    public String restore(String filePath) throws RemoteException {
        try {
            String fileHash = DigestFile.getHash(filePath);
            int chunkNo = DigestFile.getChunkCount(filePath);
            if (chunkNo < 0) return "file " + filePath + " is too big";
            // TODO repetir while replication != 0 (atencao se temos o chunk connosco ou nao (reclaim))
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
    // returns capacity left to trim
    private long trimFiles(long capactityToTrim, boolean force) {
        if (capactityToTrim <= 0) return 0;

        long currentCap = capactityToTrim;

        for (var entry : DigestFile.replicationDegMap.entrySet()) {
            String fileId = entry.getKey();
            // int desiredRep = entry.getValue().p1;

            for (var chunkEntry : entry.getValue().p2.entrySet()) {
                int chunkNo = chunkEntry.getKey();
                int currRepl = chunkEntry.getValue();
                if (DigestFile.hasChunk(fileId, chunkNo) && (force || currRepl > 1) && currRepl > 0) {
                    DigestFile.deleteChunk(fileId, chunkNo);
                    DigestFile.decreaseChunkDeg(fileId, chunkNo);
                    currentCap -= DigestFile.getChunkSize(fileId, chunkNo);

                    RemovedMsg removedMsg = new RemovedMsg(this.protocolVersion, this.id, fileId, chunkNo);
                    this.MCSock.send(removedMsg);
                }
                if (currentCap <= 0) break;
            }
            if (currentCap <= 0) break;
        }

        return currentCap;
    }

    @Override
    public String reclaim(int newMaxDiskSpaceKB) throws RemoteException { // TODO Adicionar isto aos ENHANCE
        int newMaxDiskSpaceB = newMaxDiskSpaceKB * 1000;

        if (newMaxDiskSpaceB < 0) {
            Proj1.maxDiskSpaceB = -1;
            // infinite capacity => do nothing
            return "Max disk space set to infinite KBytes.";
        } else if (Proj1.maxDiskSpaceB >= 0) {
            int capacityDelta = newMaxDiskSpaceB - Proj1.maxDiskSpaceB;
            Proj1.maxDiskSpaceB = newMaxDiskSpaceB;
            // if max capacity is unchanged or increases, we don't need to do anything
            if (capacityDelta >= 0)
                return "Max disk space set to " + newMaxDiskSpaceKB + " KBytes.";
        } else {
            Proj1.maxDiskSpaceB = newMaxDiskSpaceB;
        }

        // remove things (trying to keep everything above 0 replication degree)
        long currentCap = DigestFile.getStorageSize() - Proj1.maxDiskSpaceB;
        System.err.println("Removing: " + currentCap);
        currentCap = trimFiles(currentCap, false);
        if (currentCap > 0) trimFiles(currentCap, true);

        if (DigestFile.getStorageSize() == Proj1.maxDiskSpaceB) this.MDBSock.leave();
        else this.MDBSock.join();

        return "reclaim";
    }

    @Override
    public String state() throws RemoteException {
        return this.toString();
    }
}
