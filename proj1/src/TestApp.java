import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    private static void usage() {
        System.err.println("Usage: java TestApp <AccessPoint> <BACKUP|RESTORE|DELETE|RECLAIM|STATE> [opnd_1 [opnd_2]]\n");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) usage();

        String rmiinfo = args[0];
        String[] rmiinfoSplit = rmiinfo.split(":");
        String rminame = rmiinfoSplit[0];

        TestInterface stub = null;
        try {
            Registry registry;
            if (rmiinfoSplit.length > 1)
                registry = LocateRegistry.getRegistry("localhost", Integer.parseInt(rmiinfoSplit[1]));
            else
                registry = LocateRegistry.getRegistry();
            stub = (TestInterface) registry.lookup(rminame);
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Couldn't find/get the desired remote object.");
            e.printStackTrace();
            System.exit(1);
        }
        assert stub != null;

        String oper = args[1];
        switch (oper.toUpperCase()) {
            case "BACKUP":
                if (args.length != 4) usage();
                String filePath = args[2];
                int replicationDegree = Integer.parseInt(args[3]);
                System.out.println("BACKUP " + filePath + " " + replicationDegree);

                try {
                    String reply = stub.backup(filePath, replicationDegree);
                    System.out.println(reply);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case "RESTORE":
                if (args.length != 3) usage();
                filePath = args[2];
                System.out.println("RESTORE " + filePath);

                try {
                    String reply = stub.restore(filePath);
                    System.out.println(reply);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case "DELETE":
                if (args.length != 3) usage();
                filePath = args[2];
                System.out.println("DELETE " + filePath);

                try {
                    String reply = stub.delete(filePath);
                    System.out.println(reply);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case "RECLAIM":
                if (args.length != 3) usage();
                int maxCapacity = Integer.parseInt(args[2]);
                System.out.println("RECLAIM " + maxCapacity);

                try {
                    String reply = stub.reclaim(maxCapacity);
                    System.out.println(reply);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case "STATE":
                if (args.length != 2) usage();
                System.out.println("STATE");

                try {
                    String reply = stub.state();
                    System.out.println(reply);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.err.println("Unknown operation: " + oper);
                usage();
                break;
        }
    }
}
