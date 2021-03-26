package File;

import Message.Message;
import utils.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DigestFile {
    private final static Integer CHUNK_LEN = 256;
    private static final int MAX_CHUNK_SIZE = 64000;
    private static final int MAX_CHUNK_NUM = 999999;
    private final static String FILE_DIR = "." + File.separator + "files" + File.separator;

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

    private static boolean surpassesMaxChunks(String filename) throws IOException {
        Path file = Paths.get(FILE_DIR + filename);
        return ((Files.size(file) / MAX_CHUNK_SIZE) > MAX_CHUNK_NUM);
    }

    public static void deleteFile(String fileId) {
        File fileDir = new File(FILE_DIR + File.separator + fileId);
        if (fileDir.listFiles() == null) return;

        // to delete a directory, the directory must be empty
        for (File f : fileDir.listFiles()) {
            f.delete();
        }
        fileDir.delete();
    }

    public static void writeChunk(Message message, String fileId, Integer chunkNo) throws IOException {
        byte[] content = message.getContent();
        writeChunk(fileId + File.separator + chunkNo, content, content.length);
    }

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

    public static byte[] readChunk(String chunkpath) throws IOException {
        FileInputStream inputFile = new FileInputStream(FILE_DIR + chunkpath);
        byte[] b = new byte[MAX_CHUNK_SIZE];
        int len = inputFile.read(b, 0, MAX_CHUNK_SIZE);
        return Arrays.copyOfRange(b, 0, len);
    }

    public static byte[] readChunk(String filename, int chunkNo) throws IOException {
        return readChunk(getHash(filename) + File.separator + chunkNo);
    }

    public static void divideFile(String filename) throws IOException {
        String fileId = getHash(filename);
        FileInputStream inputFile = new FileInputStream(FILE_DIR + filename);
        byte[] b = new byte[MAX_CHUNK_SIZE];
        int n, i = 0;

        if (surpassesMaxChunks(filename))
            throw new MasNaoTeVouAlocar();

        while ((n = inputFile.read(b, 0, MAX_CHUNK_SIZE)) >= MAX_CHUNK_SIZE) {
            final String chunkpath = fileId + File.separator + i;
            writeChunk(chunkpath, b, n);
            ++i;
        }

        final String chunkpath = fileId + File.separator + i;
        writeChunk(chunkpath, b, n);
    }

    public static void assembleFile(String filename, String file_id) throws IOException {
        byte[] b = new byte[MAX_CHUNK_SIZE];
        int i = 0;
        boolean done = false;

        File f = new File(FILE_DIR + filename);
        f.getParentFile().mkdirs();
        if (!f.createNewFile()) return;
        FileOutputStream file = new FileOutputStream(f);
        ;

        while (!done) {
            String chunkpath = FILE_DIR + file_id + File.separator + i;
            try {
                FileInputStream inputStream = new FileInputStream(chunkpath);
                int n = inputStream.read(b);
                file.write(b, 0, n);
            } catch (FileNotFoundException e) {
                done = true;
            }
            ++i;
        }
    }

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
       Map<String, Pair<Integer, Map<Integer, Integer>>> map = importMap("fileMap.txt");
        for (var entry : map.keySet()) {
           for (var entry2 : map.get(entry).p2.keySet()) {
               String hash = entry;
               Integer repDeg = map.get(entry).p1;
               Integer chunkNo = entry2;
               Integer perceviedDeg = map.get(entry).p2.get(entry2);
               System.out.println(hash + " " + repDeg.toString() + " " + chunkNo.toString() + " " + perceviedDeg.toString());
           }
       }
        try {
            exportMap(map, "test.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Pair<Integer, Map<Integer, Integer>>> importMap(String repMapName) {
        Scanner scanner;
        try {
            scanner = new Scanner(new File(repMapName));
        } catch (FileNotFoundException e) {
            return new HashMap<>();
        }
        String line, previousHash = "";
        Integer fileRepDegree = -1;
        Map<Integer, Integer> chunkMap = new HashMap<>();
        Map<String, Pair<Integer, Map<Integer, Integer>>> fileMap = new HashMap<>();
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            String[] contents = line.split(" ");
            String hash = contents[0];
            Integer chunkNo = Integer.valueOf(contents[2]),
                    perceivedRepDeg = Integer.valueOf(contents[3]);
            if (!previousHash.equals(hash) && !previousHash.equals("")) { // New hash, commit to fileMap
                fileMap.put(previousHash, new Pair<>(fileRepDegree, chunkMap));
                chunkMap = new HashMap<>(); // Reset chunk map
                chunkMap.put(chunkNo, perceivedRepDeg);
            } else {
                chunkMap.put(chunkNo, perceivedRepDeg);
            }
            fileRepDegree = Integer.valueOf(contents[1]);
            previousHash = hash;
        }
        fileMap.put(previousHash, new Pair<>(fileRepDegree, chunkMap));
        return fileMap;
    }

    public static void exportMap(Map<String, Pair<Integer, Map<Integer, Integer>>> map, String repMapName) throws IOException {
        BufferedWriter wr = new BufferedWriter(new FileWriter(repMapName));

        for (var hash : map.keySet()) {
            Pair<Integer, Map<Integer, Integer>> pair = map.get(hash);
            Integer repDeg = pair.p1;
            Map<Integer, Integer> chunkMap = pair.p2;
            for (var chunkNo : chunkMap.keySet()) {
                Integer perceviedDeg = chunkMap.get(chunkNo);
                wr.write(hash + " " + repDeg + " " + chunkNo + " " + perceviedDeg);
                System.out.println(hash + " " + repDeg + " " + chunkNo + " " + perceviedDeg + "?");
                wr.newLine();
            }
        }
        wr.close();
    }
}
