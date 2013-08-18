package com.horstmeier.java.tftp;

import com.horstmeier.java.tftp.interfaces.IFileNameMapper;
import junit.framework.TestCase;
import org.apache.commons.net.tftp.TFTPClient;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.*;

public class TFTPBaseServerTest extends TestCase {

    class TestFileMapperRead implements IFileNameMapper {

        @Override
        public InputStream openInputStream(String fileName) throws IOException {
            if (fileName.equals("$#")) {
                return new ByteArrayInputStream("Hello World".getBytes());
            }
            throw new IOException("Illegal file name");
        }

        @Override
        public OutputStream openOutputStream(String fileName) throws IOException {
            throw new IOException("Illegal operation");
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public boolean canWrite() {
            return false;
        }
    }

    public void setUp() throws Exception {

    }

    public void tearDown() throws Exception {

    }

    public void testBaseServer() throws Exception
    {
        Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout()));
        TFTPBaseServer baseServer = new TFTPBaseServer(new TestFileMapperRead(), 8088);
        TFTPClient client = new TFTPClient();
        client.open();
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        client.receiveFile("$#", 1, os, "localhost", 8088);
        assert os.toString().equals("Hello World");
        baseServer.shutdown();
    }
}
