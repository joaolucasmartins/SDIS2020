import file.DigestFile;
import message.ChunkMsg;
import message.ChunkTCPMsg;
import message.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class ChunkTCPSender extends MessageSender<ChunkTCPMsg> {
    private final static int MAX_TIMEOUT=400;
    private final static int MAX_TIMEOUT_TCP=10000;
    private boolean chunkAlreadySent;
    private byte[] chunk;

    public ChunkTCPSender(SockThread sockThread, ChunkTCPMsg message, MessageHandler handler) {
        super(sockThread, message, handler);
        this.chunkAlreadySent = false;
        try {
            this.chunk = DigestFile.readChunk(message.getFileId() + File.separator + message.getChunkNo());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notify(Message message) {
        if (refersToSameChunk(message)) {
            chunkAlreadySent = true;
            this.xau();
        }
    }

    private boolean refersToSameChunk(Message message) {
        if (message.getType().equals(ChunkMsg.type)) {
            ChunkMsg chunkMsg = (ChunkMsg) message;
            return chunkMsg.getChunkNo() == this.message.getChunkNo() &&
                    chunkMsg.getFileId().equals(this.message.getFileId());
        }
        return false;
    }

    @Override
    public void run() {
        Random random = new Random();
        int timeout = random.nextInt(MAX_TIMEOUT + 1);
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Started running Chunk");
                        // Check if a connection was already opened
                        if (chunkAlreadySent)
                            return;
                        // Create Socket
                        ServerSocket serverSocket;
                        Socket socket = null;
                        try {
                            serverSocket = new ServerSocket(0);
                            serverSocket.setSoTimeout(MAX_TIMEOUT_TCP);
                        } catch (IOException e) {
                            System.err.println("Could not create socket to listen to (ChunkTCP)");
                            return;
                        }
                        // If a connection was already created while we opened socket close it and go bye
                        if (chunkAlreadySent) {
                            try {
                                serverSocket.close();
                            } catch (IOException e) {
                                System.err.println("Failed to close socket (ChunkTCP)");
                            }
                            return;
                        }
                        // Send ChunkTCP Message with the respective ip and port
                        Integer port = serverSocket.getLocalPort();
                        ChunkTCPSender.super.message.setTCPAddress(serverSocket.getInetAddress().getHostAddress(), port);
                        System.out.println("Sent chunk");
                        ChunkTCPSender.super.send();
                        // Wait for someone to connect
                        try {
                            socket = serverSocket.accept();
                        } catch (IOException e) {
                            System.err.println("Timed out while waiting for answer (ChunkTCP) TODO");
                            try {
                                serverSocket.close();
                            } catch (IOException ioException) {
                                System.err.println("Failed to close socket (ChunkTCP)");
                            }
                            return; // TODO this.xau
                        }
                        try {
                            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                            outputStream.write(ChunkTCPSender.this.chunk);
                        } catch (IOException e) {
                            System.err.println("Failed to send chunk message (ChunkTCP)");
                        }
                        try {
                            socket.close();
                            serverSocket.close();
                        } catch (IOException ioException) {
                            System.err.println("Failed to close socket (ChunkTCP)");
                        }
                    }
                },
                timeout
        );
    }
}
