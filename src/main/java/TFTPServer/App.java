package TFTPServer;

import com.horstmeier.java.tftp.Mode;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import com.horstmeier.java.tftp.TFTPServer;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        if (args.length != 1)
        {
            System.out.println("You must provide 1 argument - the base path for the server to serve from.");
            System.exit(1);
        }

        Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout()));
        try {
            TFTPServer ts = new TFTPServer(new File(args[0]), new File(args[0]), 8088, Mode.GET_AND_PUT);
            ts.setSocketTimeout(2000);

            System.out.println("TFTP Server running.  Press enter to stop.");
            new InputStreamReader(System.in).read();

            ts.shutdown();
            System.out.println("Server shut down.");
            System.exit(0);
        } catch (IOException e) {
            System.err.printf("Exception : %f\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
