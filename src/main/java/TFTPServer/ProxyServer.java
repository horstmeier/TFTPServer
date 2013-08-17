package TFTPServer;

import com.horstmeier.java.tftp.TFTPBaseServer;
import com.horstmeier.java.tftp.proxyserver.ProxyFileMapper;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by jens on 17.08.13.
 */
public class ProxyServer {
    public static void main(String[] args) {

        int port = 0;
        String baseUri;

        if (args.length != 2)
            usage();
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.printf("Illegal port number : %s\n", e.getMessage());
            usage();
        }

        baseUri = args[1];

        Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout()));
        ProxyFileMapper proxyFileMapper = new ProxyFileMapper(baseUri);
        try {
            TFTPBaseServer baseServer = new TFTPBaseServer(proxyFileMapper, port);
            new InputStreamReader(System.in).read();
            baseServer.shutdown();
            System.out.println("Server shut down.");
            System.exit(0);
        } catch (IOException e) {
            System.err.printf("Failed to run server : %s\n", e.getMessage());
            usage();
        }

    }

    private static void usage() {
        System.out.println("Usage: ProxyServer <port> <server url>");
        System.exit(1);
    }
}
