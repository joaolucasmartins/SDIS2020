import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestFile {
    final static Integer CHUNK_LEN = 256;
    private String getBitString(String filename) throws IOException {
        Path file = Paths.get(filename);

        byte[] b = new byte[256];
        FileInputStream inputStream = new FileInputStream(filename);
        int len = inputStream.read(b, 0, CHUNK_LEN); // Read first 256 bytes

        String bitString = filename + " " + Files.getOwner(file) + " " + Files.getLastModifiedTime(file);
        for (int i=0; i<len; ++i)
            bitString += (char) b[i];

        return bitString;
    }

    public byte[] getHash(String filename) throws IOException {
        String bitString = this.getBitString(filename);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(bitString.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static void main(String[] args) {
        DigestFile d = new DigestFile();
        try {
            byte[] b = d.getHash("./src/filename.txt");
            for (byte by: b)
                System.out.print((char) by);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
