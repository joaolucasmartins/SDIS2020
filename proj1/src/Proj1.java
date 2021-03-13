import java.io.IOException;
import java.net.*;
import java.util.*;

/*
Multicast allows communication between different containers to be used on different
computers (in this case, you should use different terminals for this purpose).
The containers should be specified by a port and an IP address within the range
224.0.0.0 and 239.255.255.255, which are IPs reserved for multicast communications.

Periodically send its IP and port to the multicast group to know that it is
working correctly. Clients should join that group and then send messages with
the desired requests. In this case, you should use MulticastSockets instead
of DatagramSockets to achieve the desired multicast communication.
To do the "register" and "lookup" operations, you should use
the same method that you used in Lab1.

To create a more robust system, I advise you to use more than one thread.
You can use the functions provided by Timer and TimerTask (package “java.util”)
or ScheduledThreadPoolExecutor (package “java.util.concurrent”). The last one
is more sophisticated but also more difficult to implement and use.
*/

public class Proj1 {
    private final String protocolVersion;
    private final String id;
    private final String accessPoint;
    private final String MC;
    private final String MDB;
    private final String MDR;
    private final int MCPort;
    private final int MDBPort;
    private final int MDRPort;
    // comment

    private DatagramSocket uniSocket = null;

    public Proj1(String[] args) {
        // parse args
        if (args.length != 9) usage();

        this.protocolVersion = args[0];
        this.id = args[1];
        this.accessPoint = args[2];
        // MC
        this.MC = args[3];
        this.MCPort = Integer.parseInt(args[4]);
        // MDB
        this.MDB = args[5];
        this.MDBPort = Integer.parseInt(args[6]);
        // MDR
        this.MDR = args[7];
        this.MDRPort = Integer.parseInt(args[8]);

        System.err.println(this);
    }

    private void server() throws IOException {
        // create unicast socket
        int srvc_port = 0, mcast_port = 0;
        String mcast_addr = "";
        try {
            uniSocket = new DatagramSocket(srvc_port);
        } catch (SocketException e) {
            System.err.println("Couldn't bind server to specified port, " + srvc_port);
            System.exit(1);
        }
        System.out.println("Bound server to port: " + srvc_port);

        // create multicast socket and bind to given port number
        // thread sending ip + port every sec
        ServerAnnouncer sa = new ServerAnnouncer(mcast_addr, mcast_port, srvc_port);
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(sa, 0, 1000); // run every sec

        // main loop
        int num_entries = 0;
        Map<String, String> dns_table = new HashMap<>();
        byte[] inbuf = new byte[256];
        DatagramPacket request = new DatagramPacket(inbuf, inbuf.length);
        label:
        while (true) {
            // receive request packet
            try {
                Arrays.fill(inbuf, (byte) 0);
                uniSocket.receive(request);
            } catch (IOException e) {
                System.err.println("Failed receiving packet. Skipping...");
                continue;
            }

            // parse packet
            String in = new String(request.getData());
            String[] req_args = in.split("[\u0000| ]");

            System.out.println("Server: " + Arrays.toString(req_args));
            if (req_args.length == 0) {
                System.err.println("Empty request. Skipping...");
                continue;
            }

            byte[] reply_bytes;
            switch (req_args[0]) {
                case "REGISTER":
                    if (req_args.length != 3) {
                        System.err.println("Register is missing arguments. Skipping...");
                        reply_bytes = "-1".getBytes();
                    } else {
                        final String dns_name = req_args[1];
                        final String ip_addr = req_args[2];

                        if (dns_table.containsKey(dns_name)) { // dns name already registered
                            System.err.println("dns name already registered");
                            reply_bytes = "-1".getBytes();
                        } else { // register new dns name
                            dns_table.put(dns_name, ip_addr);
                            System.out.println("Registered: " + dns_table.get(dns_name));

                            reply_bytes = String.valueOf(num_entries).getBytes();
                            ++num_entries;
                        }
                    }
                    break;
                case "LOOKUP":
                    if (req_args.length != 2) {
                        System.err.println("Lookup is missing arguments. Skipping...");
                        reply_bytes = "-1".getBytes();
                    } else {
                        final String dns_name = req_args[1];

                        if (dns_table.containsKey(dns_name)) { // dns name found
                            reply_bytes = (dns_name + " " + dns_table.get(dns_name)).getBytes();
                        } else { // dns name not found
                            reply_bytes = "-1".getBytes();
                        }
                    }
                    break;
                case "CLOSE":
                    System.out.println("Got close, closing...");
                    break label;
                default:
                    System.err.println("Unkown request. Skipping...");
                    continue;
            }

            // send reply packet
            DatagramPacket reply = new DatagramPacket(reply_bytes, reply_bytes.length,
                    request.getAddress(), request.getPort());
            try {
                uniSocket.send(reply);
            } catch (IOException e) {
                System.err.println("Couldn't send reply. Skipping...");
            }
        }

        System.out.println("Quitting...");
        timer.cancel();
        sa.close();
        uniSocket.close();
    }

    @Override
    public String toString() {
        return "Protocol version: " + this.protocolVersion + "\n" +
                "Id: " + this.id + "\n" +
                "Service access point: " + this.accessPoint + "\n" +
                "MC: " + this.MC + ":" + this.MCPort + "\n" +
                "MC: " + this.MDB + ":" + this.MDBPort + "\n" +
                "MC: " + this.MDR + ":" + this.MDRPort;
    }

    private static void usage() {
        System.err.println("Usage: java -jar\n\tProj1 <protocol version> <peer id> <service access point>\n\t<MC> <MDB> <MDR>");
        System.exit(1);
    }

    public static void main(String[] args) {
        new Proj1(args);
    }
}
