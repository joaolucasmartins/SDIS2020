package state;

import utils.Pair;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FileInfo implements Serializable {
    private String filePath = null;  // only set if we are the initiator
    private Integer desiredRep;
    private final ConcurrentMap<Integer, Pair<Integer, Boolean>> chunkInfo;

    public FileInfo(int desiredRep) {
        this.desiredRep = desiredRep;
        this.chunkInfo = new ConcurrentHashMap<>();
    }

    public FileInfo(String filePath, int desiredRep) {
        this(desiredRep);
        this.filePath = filePath;
    }

    public void declareChunk(int chunkNo) {
        if (!this.chunkInfo.containsKey(chunkNo))
            this.chunkInfo.put(chunkNo, new Pair<>(0, false));
    }

    public boolean amIStoringChunk(int chunkNo) {
        if (!this.chunkInfo.containsKey(chunkNo)) return false;
        return this.chunkInfo.get(chunkNo).p2;
    }

    public void setAmStoringChunk(int chunkNo, boolean amStoring) {
        if (!this.chunkInfo.containsKey(chunkNo)) return;
        this.chunkInfo.get(chunkNo).p2 = amStoring;
    }

    // initiator
    public boolean isInitiator() {
        return this.filePath != null;
    }

    public String getFilePath() {
        return this.filePath;
    }

    // replication degree
    public int getDesiredRep() {
        return desiredRep;
    }

    public void setDesiredRep(int desiredRep) {
        this.desiredRep = desiredRep;
    }

    public int getChunkPerceivedRep(int chunkNo) {
        return this.chunkInfo.get(chunkNo).p1;
    }

    public void incrementChunkDeg(int chunkNo) {
        if (this.chunkInfo.containsKey(chunkNo)) {
            Pair<Integer, Boolean> chunk = this.chunkInfo.get(chunkNo);
            chunk.p1 += 1;
        } else {
            this.chunkInfo.put(chunkNo, new Pair<>(1, false));
        }
    }

    public void decrementChunkDeg(int chunkNo) {
        if (this.chunkInfo.containsKey(chunkNo)) {
            Pair<Integer, Boolean> chunk = this.chunkInfo.get(chunkNo);
            chunk.p1 -= 1;
        } else {
            this.chunkInfo.put(chunkNo, new Pair<>(0, false));
        }
    }

    // iterator
    public Map<Integer, Pair<Integer, Boolean>> getAllChunks() {
        return this.chunkInfo;
    }
}
