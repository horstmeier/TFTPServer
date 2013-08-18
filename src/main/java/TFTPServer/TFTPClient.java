package TFTPServer;

import java.io.FileOutputStream;

public class TFTPClient {
    public static void main(String[] args) {
        if (args.length != 4) {
            usage();
            return;
        }
        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String file = args[2];
            String localFile = args[3];

            org.apache.commons.net.tftp.TFTPClient client = new org.apache.commons.net.tftp.TFTPClient();
            client.open();
            client.receiveFile(file, 1, new FileOutputStream(localFile), host, port);

        } catch (Exception e) {
            System.err.printf("Failed to read file : %s\n", e.getMessage());
            System.exit(1);
        }


    }

    public static void usage() {
        System.out.println("Usage: TFTPClient <host> <port> <file> <local name>");
    }
}
