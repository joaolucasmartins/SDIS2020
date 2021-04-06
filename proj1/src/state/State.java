package state;

import file.DigestFile;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// this class is a singleton
public class State implements Serializable {
    public static final String REPMAPNAME = "repMap.txt";
    public static State st = State.importMap();

    private final ConcurrentMap<String, FileInfo> replicationMap;
    private volatile Long maxDiskSpaceB;
    private volatile transient long filledStorageSizeB;

    private State() {
        this.replicationMap = new ConcurrentHashMap<>();
        this.maxDiskSpaceB = -1L;
    }

    public static State getState() {
        return st;
    }

    // STORAGE
    public synchronized Long getMaxDiskSpaceB() {
        return maxDiskSpaceB;
    }

    public synchronized Long getMaxDiskSpaceKB() {
        return maxDiskSpaceB < 0 ? -1 : maxDiskSpaceB / 1000;
    }

    public synchronized void setMaxDiskSpaceB(Long maxDiskSpaceB) {
        this.maxDiskSpaceB = maxDiskSpaceB;
    }

    public synchronized void initFilledStorage() {
        this.filledStorageSizeB = DigestFile.getStorageSize();
    }

    public synchronized long getFilledStorageB() {
        return this.filledStorageSizeB;
    }

    public synchronized boolean updateStorageSize(long sizeToAddB) {
        if (maxDiskSpaceB < 0) { // is infinite
            filledStorageSizeB += sizeToAddB;
            return true;
        }

        if (filledStorageSizeB + sizeToAddB < maxDiskSpaceB) {
            filledStorageSizeB += sizeToAddB;
            return true;
        }
        return false;
    }

    public synchronized boolean isStorageFull() {
        return this.maxDiskSpaceB > 0 && (this.filledStorageSizeB < this.maxDiskSpaceB);
    }

    public boolean isChunkOk(String fileId, int chunkNo) {
        int desiredRepDeg = State.st.getFileDeg(fileId);
        int chunkDeg = State.st.getChunkDeg(fileId, chunkNo);

        return chunkDeg >= desiredRepDeg;
    }

    public boolean isInitiator(String fileId) {
        if (!this.replicationMap.containsKey(fileId)) return false;
        return this.replicationMap.get(fileId).isInitiator();
    }

    // ADD
    public void addFileEntry(String fileId, String filePath, int desiredRep) {
        if (!this.replicationMap.containsKey(fileId)) {
            this.replicationMap.put(fileId, new FileInfo(filePath, desiredRep));
        } else {
            FileInfo fileInfo = this.replicationMap.get(fileId);
            fileInfo.setDesiredRep(desiredRep);
        }
    }

    public void addFileEntry(String fileId, int desiredRep) {
        if (!this.replicationMap.containsKey(fileId)) {
            this.replicationMap.put(fileId, new FileInfo(desiredRep));
        } else {
            FileInfo fileInfo = this.replicationMap.get(fileId);
            fileInfo.setDesiredRep(desiredRep);
        }
    }

    public void removeFileEntry(String fileId) {
        this.replicationMap.remove(fileId);
    }

    public void declareChunk(String fileId, int chunkNo) {
        // only declares if it isn't declared yet
        if (!this.replicationMap.containsKey(fileId)) return;
        this.replicationMap.get(fileId).declareChunk(chunkNo);
    }

    // REPLICATION DEGREE
    public int getFileDeg(String fileId) {
        // file desired rep
        if (!this.replicationMap.containsKey(fileId)) return 0;

        return this.replicationMap.get(fileId).getDesiredRep();
    }

    public int getChunkDeg(String fileId, int chunkNo) {
        // perceived chunk rep
        if (!this.replicationMap.containsKey(fileId)) return 0;

        return this.replicationMap.get(fileId).getChunkPerceivedRep(chunkNo);
    }

    public void incrementChunkDeg(String fileId, int chunkNo) {
        if (!this.replicationMap.containsKey(fileId)) return;
        this.replicationMap.get(fileId).incrementChunkDeg(chunkNo);
    }

    public void decrementChunkDeg(String fileId, int chunkNo) {
        if (!this.replicationMap.containsKey(fileId)) return;
        this.replicationMap.get(fileId).decrementChunkDeg(chunkNo);
    }

    // OTHER
    public boolean amIStoringChunk(String fileId, int chunkNo) {
        if (!this.replicationMap.containsKey(fileId)) return false;
        return this.replicationMap.get(fileId).amIStoringChunk(chunkNo);
    }

    // ITERATION
    public FileInfo getFileInfo(String fileId) {
        if (!this.replicationMap.containsKey(fileId)) return null;
        return this.replicationMap.get(fileId);
    }

    public Map<String, FileInfo> getAllFilesInfo() {
        return this.replicationMap;
    }

    // FOR SERIALIZATION
    public static State importMap() {
        State ret;
        try {
            FileInputStream fileIn = new FileInputStream(DigestFile.FILE_DIR + File.separator + REPMAPNAME);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            ret = (State) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException i) {
            ret = new State();
        }

        ret.initFilledStorage();

        return ret;
    }

    public static void exportMap() throws IOException {
        try {
            FileOutputStream fileOut = new FileOutputStream(DigestFile.FILE_DIR + File.separator + REPMAPNAME);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(State.st);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }
}