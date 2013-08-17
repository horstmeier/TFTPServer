package com.horstmeier.java.tftp;

import com.horstmeier.java.tftp.proxyserver.ProxyFileMapper;
import junit.framework.TestCase;
import org.apache.commons.net.tftp.TFTPClient;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.ByteArrayOutputStream;

/**
 * Created by jens on 17.08.13.
 */
public class ProxyFileMapperTest extends TestCase {
    public void testProxyFileMapper() throws Exception {
        Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout()));
        ProxyFileMapper proxyFileMapper = new ProxyFileMapper("http://tools.ietf.org/");
        TFTPBaseServer baseServer = new TFTPBaseServer(proxyFileMapper, 8089);
        TFTPClient client = new TFTPClient();
        client.open();
        ByteArrayOutputStream os = new ByteArrayOutputStream(16*1024*1024);
        client.receiveFile("html/rfc1350", 1, os, "localhost", 8089);
        String result = os.toString();
        System.out.println(result);
        assert result.contains("Request For Comments: 1350");
        baseServer.shutdown();
    }
}
