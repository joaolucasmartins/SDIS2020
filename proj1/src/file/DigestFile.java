package file;

import message.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DigestFile {
    public static State state;
    private final static Integer CHUNK_LEN = 256;
    private static final int MAX_CHUNK_SIZE = 64000;
    private static final int MAX_CHUNK_NUM = 999999;
    private static String FILE_DIR = "." + File.separator + "files" + File.separator;
    private static final String REPMAPNAME = "repMap.txt";

    public static void setFileDir(String id) {
        FILE_DIR = "." + File.separator + ("files-" + id) + File.separator;
        File f = new File(FILE_DIR);
        f.mkdirs();
    }

    /* file metadata used to get a hash */
    private static String getBitString(String filename) throws IOException {
        Path file = Paths.get(FILE_DIR + filename);
        FileInputStream inputStream = new FileInputStream(FILE_DIR + filename);

        byte[] b = new byte[256];
        int len = inputStream.read(b, 0, CHUNK_LEN); // Read first 256 bytes

        // Hash with absolute path, owner, last modified time and first 256 bytes
        StringBuilder bitString = new StringBuilder(
                file.toAbsolutePath().toString() +
                        Files.getOwner(file) +
                        Files.getLastModifiedTime(file)
        );
        for (int i = 0; i < len; ++i) // Add 256 bytes
            bitString.append((char) b[i]);

        return bitString.toString();
    }

    /* get the hash that identifies a given file */
    public static String getHash(String filename) throws IOException {
        String bitString = getBitString(filename);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] bytes = digest.digest(bitString.getBytes(StandardCharsets.US_ASCII));
            StringBuilder r = new StringBuilder();
            for (byte b : bytes)
                r.append(String.format("%02x", b));
            return r.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /* checks if a file needs more chunks to be stored than the maximum allowed */
    private static boolean surpassesMaxChunks(String filename) throws IOException {
        Path file = Paths.get(FILE_DIR + filename);
        return ((Files.size(file) / MAX_CHUNK_SIZE) > MAX_CHUNK_NUM);
    }

    /* returns the number of chunks used to store a given file. -1 if the file is too big */
    public static int getChunkCount(String filename) throws IOException {
        if (surpassesMaxChunks(filename)) return -1;
        Path file = Paths.get(FILE_DIR + filename);
        return (int) ((Files.size(file) / MAX_CHUNK_SIZE) + 1);
    }

    /* deletes a directory (that represents a file by containing its chunks) and its contents */
    public static void deleteFile(String fileId) {
        File fileDir = new File(FILE_DIR + File.separator + fileId);
        if (fileDir.listFiles() == null) return;

        state.removeFileEntry(fileId);  // TODO check if working correctly

        // to delete a directory, the directory must be empty
        for (File f : Objects.requireNonNull(fileDir.listFiles())) {
            f.delete();
        }
        fileDir.delete();
    }

    public static void deleteChunk(String fileId, Integer chunkNo) {
        File chunk = new File(FILE_DIR + File.separator + fileId + File.separator + chunkNo.toString());
        chunk.delete();
    }

    public static long getChunkSize(String fileId, Integer chunkNo) {
        Path path = Paths.get(FILE_DIR + File.separator + fileId + File.separator + chunkNo.toString());
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    public static long getStorageSize() {
        long ret = 0;
        File fileDir = new File(FILE_DIR);
        if (fileDir.listFiles() == null) return ret;

        // to delete a directory, the directory must be empty
        for (File file : Objects.requireNonNull(fileDir.listFiles())) {
            if (file.isDirectory()) {
                for (File chunk : Objects.requireNonNull(file.listFiles())) {
                    ret += chunk.length();
                }
            }
        }
        return ret;
    }

    /* Write a chunk to a file */
    public static void writeChunk(String chunkpath, byte[] b, int n) throws IOException {
        String path = FILE_DIR + File.separator + chunkpath;
        File f = new File(path);
        f.getParentFile().mkdirs();
        if (!f.createNewFile()) return;
        if (n >= 0) {
            try (FileOutputStream chunk = new FileOutputStream(path)) {
                chunk.write(b, 0, n);
            } catch (Exception e) {
                System.out.println("no write :(" + chunkpath + " " + n);
            }
        }
    }

    public static void writeChunk(Message message, String fileId, Integer chunkNo) throws IOException {
        byte[] content = message.getContent();
        writeChunk(fileId + File.separator + chunkNo, content, content.length);
    }

    /* reads the contents of a chunk */
    public static byte[] readChunk(String chunkpath) throws IOException {
        FileInputStream inputFile = new FileInputStream(FILE_DIR + chunkpath);
        byte[] b = new byte[MAX_CHUNK_SIZE];
        int len = inputFile.read(b, 0, MAX_CHUNK_SIZE);
        return Arrays.copyOfRange(b, 0, len);
    }

    public static byte[] readChunk(String filename, int chunkNo) throws IOException {
        return readChunk(getHash(filename) + File.separator + chunkNo);
    }

    /* divide a file into chunks */
    public static List<byte[]> divideFile(String filename, int replicationDegree) throws IOException {
        List<byte[]> ret = new ArrayList<>();

        String fileId = getHash(filename);
        FileInputStream inputFile = new FileInputStream(FILE_DIR + filename);
        byte[] b = new byte[MAX_CHUNK_SIZE];
        int n, i = 0;

        if (surpassesMaxChunks(filename))
            throw new MasNaoTeVouAlocar();

        state.addFileEntry(fileId, true, replicationDegree);

        while ((n = inputFile.read(b, 0, MAX_CHUNK_SIZE)) >= MAX_CHUNK_SIZE) {
            ret.add(Arrays.copyOfRange(b, 0, n));
            state.declareChunk(fileId, i);  // only declares if it isn't declared yet
            ++i;
        }

        // end chunk
        state.declareChunk(fileId, i);  // only declares if it isn't declared yet
        ret.add(Arrays.copyOfRange(b, 0, n));

        return ret;
    }

    /* reassemble a file from its chunks */
    public static void assembleFile(String filename, List<byte[]> chunks) throws IOException {
        File f = new File(FILE_DIR + filename);
        f.getParentFile().mkdirs();
        f.createNewFile();
        FileOutputStream file = new FileOutputStream(f);

        for (byte[] chunk: chunks)
            file.write(chunk, 0, chunk.length);
    }

    /* returns whether or not we have this chnk stored */
    public static boolean hasChunk(String hash, Integer chunkNo) {
        File file = new File(FILE_DIR + hash +
                File.separator + chunkNo);
        return file.exists();
    }

    public static void main(String[] args) {
        //try {
        //String filename = "filename.rar";
        // String h = getHash(filename);
        // divideFile(filename);

        //     String id = "416ebf6f9e407ba10294e58cbcdc1ef55b0920cd6fd6255fe6767528ddf50aba";
        //     assembleFile(filename, id);
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        importMap();
        try {
            exportMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void importMap() {
        try {
            FileInputStream fileIn = new FileInputStream(FILE_DIR + File.separator + REPMAPNAME);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            DigestFile.state = (State) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException i) {
            DigestFile.state = new State();
        }
    }

    public static void exportMap() throws IOException {
        try {
            FileOutputStream fileOut = new FileOutputStream(FILE_DIR + File.separator + REPMAPNAME);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(DigestFile.state);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public static boolean chunkIsOk(String fileId, int chunkNo) {
        State.FileInfo p = state.getFileInfo(fileId);
        if (p == null) return false;

        int desiredRepDeg = p.getDesiredRep();
        Map<Integer, Integer> chunkInfo = p.getAllChunks();

        if (!chunkInfo.containsKey(chunkNo)) return false;
        return desiredRepDeg <= chunkInfo.get(chunkNo);
    }

    public static List<Integer> getChunksBellowRep(String fileId) {
        List<Integer> res = new ArrayList<>();
        State.FileInfo fileInfo = DigestFile.state.getFileInfo(fileId);
        Integer desiredRepDeg = fileInfo.getDesiredRep();
        Map<Integer, Integer> map = fileInfo.getAllChunks();
        for (Integer key : map.keySet()) {
            if (map.get(key) < desiredRepDeg)
                res.add(key);
        }
        return res;
    }
}
