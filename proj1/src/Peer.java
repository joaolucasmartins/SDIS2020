import file.DigestFile;
import utils.Pair;
import file.State;
import message.PutChunkMsg;
import message.GetChunkMsg;
import message.RemovedMsg;
import message.DeleteMsg;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Peer implements TestInterface {
    private final ThreadPoolExecutor backupThreadPool;

    private boolean closed = false;
    // cmd line arguments
    private final String protocolVersion;
    private final String id;
    private final String accessPoint;
    // multicast sockets
    private final SockThread MCSock;
    private final SockThread MDBSock;
    private final SockThread MDRSock;
    private final MessageHandler messageHandler;

    public Registry registry = null;
    public String rmiName = null;

    public String getAccessPointName() {
        return this.accessPoint;
    }

    public Peer(String[] args) throws IOException {
        // parse args
        if (args.length != 9) usage();

        this.backupThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

        this.protocolVersion = args[0];
        this.id = args[1];
        // set the file dir name for the rest of the program (create it if missing)
        // and get info
        DigestFile.setFileDir(this.id);
        DigestFile.importMap();
        DigestFile.state.initFilledStorage();

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
        if (closed) return;
        closed = true;

        this.MCSock.close();
        this.MDBSock.close();
        this.MDRSock.close();
        this.messageHandler.saveMap();

        // shutdown executors
        this.backupThreadPool.shutdown();

        // cleanup the access point
        if (registry != null) {
            try {
                registry.unbind(rmiName);
                UnicastRemoteObject.unexportObject(this, true);
            } catch (Exception e) {
                System.err.println("Failed to unregister our RMI service.");
            }
        }
    }

    private void mainLoop() {
        this.MCSock.start();
        this.MDBSock.start();
        this.MDRSock.start();
        Scanner scanner = new Scanner(System.in);
        String cmd;
        do {
            cmd = scanner.nextLine();
            System.out.println("CMD: " + cmd);
            String filePath = "1b.txt";
            if (cmd.equalsIgnoreCase("backup")) {
                try {
                    this.backup(filePath, 2);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (cmd.equalsIgnoreCase("reclaim")) {
                try {
                    this.reclaim(0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (cmd.equalsIgnoreCase("removed")) {
                this.MCSock.send(new RemovedMsg(this.protocolVersion, this.id, "c1844545909fe089c87c73f089be8be3f7e27c40d63f4daab455a769b30cbbee", 0));
            } else if (cmd.equalsIgnoreCase("restore")) {
                try {
                    System.out.println(this.restore(filePath));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (cmd.equalsIgnoreCase("divide")) {
                try {
                    List<byte[]> chunks = DigestFile.divideFile(filePath, 3);
                    for (int i = 0; i < chunks.size(); ++i)
                        DigestFile.writeChunk(DigestFile.getHash(filePath) + File.separator + i, chunks.get(i), chunks.get(i).length);
                } catch (IOException e) {
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
        Peer prog = null;
        try {
            prog = new Peer(args);
        } catch (IOException e) {
            System.err.println("Couldn't initialize the program.");
            e.printStackTrace();
            usage();
        }
        assert prog != null;

        // trap sigint
        Peer finalProg = prog;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Exiting gracefully..");
            finalProg.cleanup();
        }));

        // setup the access point
        TestInterface stub;
        try {
            stub = (TestInterface) UnicastRemoteObject.exportObject(prog, 0);
            String[] rmiinfoSplit = prog.getAccessPointName().split(":");
            prog.rmiName = rmiinfoSplit[0];
            if (rmiinfoSplit.length > 1)
                prog.registry = LocateRegistry.getRegistry("localhost", Integer.parseInt(rmiinfoSplit[1]));
            else
                prog.registry = LocateRegistry.getRegistry();
            prog.registry.bind(prog.rmiName, stub);
        } catch (Exception e) {
            System.err.println("Failed setting up the access point for use by the testing app.");
            // e.printStackTrace();
        }

        prog.mainLoop();
        System.exit(0);
    }

    /* used by the TestApp (RMI) */
    @Override
    public String backup(String filePath, int replicationDegree) throws RemoteException {
        try {
            String fileId = DigestFile.getHash(filePath);
            List<byte[]> chunks = DigestFile.divideFile(filePath, replicationDegree);
            for (int i = 0; i < chunks.size(); ++i) {
                // only backup chunks that don't have the desired replication degree
                if (DigestFile.chunkIsOk(fileId, i)) continue;
                PutChunkMsg putChunkMsg = new PutChunkMsg("1.0", this.id,
                        fileId, i, replicationDegree, chunks.get(i));
                PutChunkSender putChunkSender = new PutChunkSender(this.MDBSock, putChunkMsg, this.messageHandler);

                this.backupThreadPool.execute(putChunkSender);
            }
        } catch (IOException e) {
            throw new RemoteException("Couldn't divide file " + filePath);
        }

        return "Backed up the file: " + filePath;
    }

    @Override
    public String restore(String filePath) throws RemoteException {
        try {
            List<Pair<Thread, GetChunkSender>> threads = new ArrayList<>();
            String fileHash = DigestFile.getHash(filePath);
            int chunkNo = DigestFile.getChunkCount(filePath); // TODO Esperar até o ultimo ter size 0 ou isto é 😎?
            if (chunkNo < 0) return "file " + filePath + " is too big";
            byte[][] chunks = new byte[chunkNo][];
            // TODO repetir while replication != 0 (atencao se temos o chunk connosco ou nao (reclaim))
            for (int currChunk = 0; currChunk < chunkNo; ++currChunk) {
                if (!DigestFile.hasChunk(fileHash, currChunk)) {
                    GetChunkMsg msg = new GetChunkMsg(this.protocolVersion, this.id, fileHash, 0);
                    GetChunkSender chunkSender = new GetChunkSender(this.MCSock, msg, this.messageHandler);
                    Thread t = new Thread(chunkSender);
                    threads.add(new Pair<>(t, chunkSender));
                    t.start();
                } else
                    chunks[currChunk] = DigestFile.readChunk(filePath, currChunk);
            }
            for (var pair : threads) {
                Thread t = pair.p1;
                GetChunkSender sender = pair.p2;
                t.join();
                if (!sender.success.get())
                    return "FAIL";
                chunks[sender.message.getChunkNo()] = sender.getResponse().getChunk();
            }
            DigestFile.assembleFile(filePath + "1", Arrays.asList(chunks));
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

        for (var entry : DigestFile.state.getAllFilesInfo().entrySet()) {
            String fileId = entry.getKey();
            // int desiredRep = entry.getValue().p1;

            for (var chunkEntry : entry.getValue().getAllChunks().entrySet()) {
                int chunkNo = chunkEntry.getKey();
                int currRepl = chunkEntry.getValue();
                if (DigestFile.hasChunk(fileId, chunkNo) && (force || currRepl > 1) && currRepl > 0) {
                    DigestFile.deleteChunk(fileId, chunkNo);
                    DigestFile.state.decrementChunkDeg(fileId, chunkNo);
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

    // TODO join/unjoin sempre (tratar dos returns
    @Override
    public String reclaim(int newMaxDiskSpaceKB) throws RemoteException { // TODO Adicionar isto aos ENHANCE
        long newMaxDiskSpaceB = newMaxDiskSpaceKB * 1000L;

        if (newMaxDiskSpaceB < 0) {
            DigestFile.state.setMaxDiskSpaceB(-1L);
            // infinite capacity => do nothing
            return "Max disk space set to infinite KBytes.";
        } else if (DigestFile.state.getMaxDiskSpaceB() >= 0) {
            long capacityDelta = newMaxDiskSpaceB - DigestFile.state.getMaxDiskSpaceB();
            DigestFile.state.setMaxDiskSpaceB(newMaxDiskSpaceB);
            // if max capacity is unchanged or increases, we don't need to do anything
            if (capacityDelta >= 0)
                return "Max disk space set to " + newMaxDiskSpaceKB + " KBytes.";
        } else {
            DigestFile.state.setMaxDiskSpaceB(newMaxDiskSpaceB);
        }

        long currentCap = DigestFile.state.getFilledStorageB() - DigestFile.state.getMaxDiskSpaceB();
        if (currentCap > 0) {
            // remove things (trying to keep everything above 0 replication degree)
            System.err.println("Removing: " + currentCap);
            currentCap = trimFiles(currentCap, false);
            if (currentCap > 0) trimFiles(currentCap, true);

            if (DigestFile.state.isStorageFull()) this.MDBSock.leave();
            else this.MDBSock.join();
        }

        return "Max disk space set to " + newMaxDiskSpaceKB + " KBytes.";
    }

    @Override
    public String state() throws RemoteException {
        StringBuilder filesIInitiated = new StringBuilder();
        filesIInitiated.append("Files I initiated the backup of:\n");
        StringBuilder chunksIStore = new StringBuilder();
        chunksIStore.append("Chunks I am storing:\n");

        for (var entry : DigestFile.state.getAllFilesInfo().entrySet()) {
            String fileId = entry.getKey();
            State.FileInfo fileInfo = entry.getValue();

            if (fileInfo.isInitiator()) {
                filesIInitiated.append("\tFile path: ").append(fileInfo.getFilePath()).append("\n");
                filesIInitiated.append("\t\tFile ID: ").append(fileId).append("\n");
                filesIInitiated.append("\t\tDesired replication degree: ").append(fileInfo.getDesiredRep()).append("\n");
                filesIInitiated.append("\t\tChunks:\n");
                for (var chunkEntry : fileInfo.getAllChunks().entrySet()) {
                    filesIInitiated.append("\t\t\tID: ").append(chunkEntry.getKey())
                            .append(" - Perceived rep.: ").append(chunkEntry.getValue()).append("\n");
                }
            } else {
                for (var chunkEntry : fileInfo.getAllChunks().entrySet()) {
                    int chunkId = chunkEntry.getKey();
                    int perceivedRep = chunkEntry.getValue();

                    chunksIStore.append("\tChunk ID: ").append(fileId).append(" - ").append(chunkId).append("\n");
                    chunksIStore.append("\t\tSize: ").append(DigestFile.getChunkSize(fileId, chunkId)).append("\n");
                    chunksIStore.append("\t\tDesired replication degree:").append(fileInfo.getDesiredRep()).append("\n");
                    chunksIStore.append("\t\tPerceived replication degree:").append(perceivedRep).append("\n");
                }
            }
        }

        long maxStorageSizeKB = DigestFile.state.getMaxDiskSpaceKB();
        return filesIInitiated
                .append(chunksIStore)
                .append("Storing ").append(Math.round(DigestFile.state.getFilledStorageB() / 1000.0))
                .append("KB of a maximum of ")
                .append(maxStorageSizeKB < 0 ? "infinite " : maxStorageSizeKB).append("KB.")
                .toString();
    }
}
