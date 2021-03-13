import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestFile {
    final static Integer CHUNK_LEN = 256;
    private static final int MAX_CHUNK_SIZE = 64000;
    private static final int MAX_CHUNK_NUM = 999999;
    final static String FILE_DIR = "." + File.separator + "files" + File.separator;

    private String getBitString(String filename, FileInputStream inputStream) throws IOException {
        Path file = Paths.get(FILE_DIR + filename);

        byte[] b = new byte[256];
        int len = inputStream.read(b, 0, CHUNK_LEN); // Read first 256 bytes

        StringBuilder bitString = new StringBuilder(filename + Files.getOwner(file) + Files.getLastModifiedTime(file));
        for (int i=0; i<len; ++i)
            bitString.append((char) b[i]);

        return bitString.toString();
    }

    public String getHash(String filename, FileInputStream inputStream) throws IOException {
        String bitString = this.getBitString(filename, inputStream);
        System.out.println(bitString + "Here is bit string");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] bytes = digest.digest(bitString.getBytes(StandardCharsets.US_ASCII));
            StringBuilder r = new StringBuilder();
            for (byte b: bytes)
                r.append(String.format("%02x", b));
            return r.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void divideFile(FileInputStream inputFile, String fileId) throws IOException {
        byte[] b = new byte[MAX_CHUNK_SIZE];
        int n, i=0;

        while ((n = inputFile.read(b, 0, MAX_CHUNK_SIZE)) == MAX_CHUNK_SIZE) {
            final String chunkpath = FILE_DIR + fileId + File.separator + i;
            try (FileOutputStream chunk = new FileOutputStream(chunkpath)) {
                chunk.write(b, 0, n);
            } catch (Exception e) {
                System.out.println("Couldn't write chunk: TODO throw something");
                System.out.println("Chunkpath: " + chunkpath);
            }
            ++i;
        }
        final String chunkpath = FILE_DIR + fileId + File.separator + i;
        File f = new File(chunkpath);
        f.getParentFile().mkdirs();
        f.createNewFile();
        try (FileOutputStream chunk = new FileOutputStream(chunkpath)) {
            chunk.write(b, 0, n);
        } catch (Exception e) {
            System.out.println("no write :(");
        }
    }

    public void assembleFile() {

    }

    public static void main(String[] args) {
        DigestFile d = new DigestFile();
        try {
            String filename = "filename.rar";
            Path file = Paths.get(FILE_DIR + filename);
            if ((Files.size(file) / MAX_CHUNK_SIZE) > MAX_CHUNK_NUM)
                throw new MasNaoTeVouAlocar();
            FileInputStream f = new FileInputStream(FILE_DIR + filename);
            String id = d.getHash(filename, f);
            d.divideFile(f, id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
