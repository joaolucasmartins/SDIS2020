package file;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class State implements Serializable {
    public static class FileInfo implements Serializable {
        private String filePath = null;  // only set if we are the initiator
        private Integer desiredRep;
        private final ConcurrentMap<Integer, Integer> chunkInfo;

        public FileInfo(int desiredRep) {
            this.desiredRep = desiredRep;
            this.chunkInfo = new ConcurrentHashMap<>();
        }

        public FileInfo(String filePath, int desiredRep) {
            this(desiredRep);
            this.filePath = filePath;
        }

        public boolean isInitiator() {
            return this.filePath != null;
        }

        public String getFilePath() {
            return this.filePath;
        }

        public int getDesiredRep() {
            return desiredRep;
        }

        public void setDesiredRep(int desiredRep) {
            this.desiredRep = desiredRep;
        }

        public Map<Integer, Integer> getAllChunks() {
            return this.chunkInfo;
        }

        public int getChunk(int chunkNo) {
            return this.chunkInfo.get(chunkNo);
        }

        public void declareChunk(int chunkNo) {
            if (!this.chunkInfo.containsKey(chunkNo))
                this.chunkInfo.put(chunkNo, 0);
        }

        public void incrementChunkDeg(int chunkNo) {
            if (this.chunkInfo.containsKey(chunkNo))
                this.chunkInfo.replace(chunkNo, this.chunkInfo.get(chunkNo) + 1);
            else
                this.chunkInfo.put(chunkNo, 1);
        }

        public void decrementChunkDeg(int chunkNo) {
            if (this.chunkInfo.containsKey(chunkNo) && this.chunkInfo.get(chunkNo) > 0)
                this.chunkInfo.replace(chunkNo, this.chunkInfo.get(chunkNo) - 1);
            else
                this.chunkInfo.put(chunkNo, 0);
        }
    }

    private final ConcurrentMap<String, FileInfo> replicationMap;
    private volatile Long maxDiskSpaceB;
    private volatile transient long filledStorageSizeB;

    public State() {
        this.replicationMap = new ConcurrentHashMap<>();
        this.maxDiskSpaceB = -1L;
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
        this.filledStorageSizeB = DigestFile.getStorageSizea();
    }

    public synchronized long getFilledStorageB() {
        return this.filledStorageSizeB;
    }

    public synchronized boolean updateStorageSize(long sizeToAddB) {
        if (filledStorageSizeB < 0) return true;

        if (filledStorageSizeB + sizeToAddB < DigestFile.state.getMaxDiskSpaceKB()) {
            filledStorageSizeB += sizeToAddB;
            return true;
        }
        return false;
    }

    public synchronized boolean isStorageFull() {
        return this.maxDiskSpaceB < 0 || (this.filledStorageSizeB < this.maxDiskSpaceB);
    }

    public FileInfo getFileInfo(String fileId) {
        if (this.replicationMap.containsKey(fileId)) return null;
        return this.replicationMap.get(fileId);
    }

    public Map<String, FileInfo> getAllFilesInfo() {
        return this.replicationMap;
    }

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

    public int getChunkDeg(String fileId, int chunkNo) {
        if (!this.replicationMap.containsKey(fileId)) return 0;

        return this.replicationMap.get(fileId).getChunk(chunkNo);
    }

    // only declares if it isn't declared yet
    public void declareChunk(String fileId, int chunkNo) {
        if (!this.replicationMap.containsKey(fileId)) return;
        this.replicationMap.get(fileId).declareChunk(chunkNo);
    }

    public void incrementChunkDeg(String fileId, int chunkNo) {
        if (!this.replicationMap.containsKey(fileId)) return;
        this.replicationMap.get(fileId).incrementChunkDeg(chunkNo);
    }

    public void decrementChunkDeg(String fileId, int chunkNo) {
        if (!this.replicationMap.containsKey(fileId)) return;
        this.replicationMap.get(fileId).decrementChunkDeg(chunkNo);
    }

    public void removeFileEntry(String fileId) {
        this.replicationMap.remove(fileId);
    }
}
