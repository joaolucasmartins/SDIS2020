package state;

import utils.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FileInfo implements Serializable {
    private String filePath = null;  // only set if we are the initiator
    private Integer desiredRep;
    private final ConcurrentMap<Integer, Pair<List<String>, Boolean>> chunkInfo;

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
            this.chunkInfo.put(chunkNo, new Pair<>(new ArrayList<>(), false));
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
        return this.chunkInfo.get(chunkNo).p1.size();
    }

    public List<String> getPeersStoringChunk(int chunkNo) {
        return this.chunkInfo.get(chunkNo).p1;
    }

    public Set<String> getPeersStoringFile() {
        Set<String> res = new HashSet<>();
        for (var chunk : this.chunkInfo.keySet())
            res.addAll(this.chunkInfo.get(chunk).p1);
        return res;
    }

    public void removePerceivedFile(String fileId) {
        for (Integer chunk: this.chunkInfo.keySet())
            this.chunkInfo.get(chunk).p1.remove(fileId);
    }

    public void incrementChunkDeg(int chunkNo, String peerId) {
        if (this.chunkInfo.containsKey(chunkNo)) {
            Pair<List<String>, Boolean> chunk = this.chunkInfo.get(chunkNo);
            chunk.p1.add(peerId);
        } else {
            List<String> l = new ArrayList<>(){{ add(peerId); }};
            this.chunkInfo.put(chunkNo, new Pair<>(l, false));
        }
    }

    public void decrementChunkDeg(int chunkNo, String peerId) {
        if (this.chunkInfo.containsKey(chunkNo)) {
            Pair<List<String>, Boolean> chunk = this.chunkInfo.get(chunkNo);
            chunk.p1.remove(peerId);
        } else {
            this.chunkInfo.put(chunkNo, new Pair<>(new ArrayList<>(), false));
        }
    }

    // iterator
    public Map<Integer, Pair<List<String>, Boolean>> getAllChunks() {
        return this.chunkInfo;
    }

}
