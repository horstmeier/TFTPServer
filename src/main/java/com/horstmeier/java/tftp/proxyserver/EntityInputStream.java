package com.horstmeier.java.tftp.proxyserver;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jens on 17.08.13.
 */
public class EntityInputStream extends InputStream {

    private final HttpGet _httpGet;
    private final Logger log = Logger.getLogger(ProxyFileMapper.class);
    private InputStream _baseStream;

    public EntityInputStream(String address) throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        _httpGet = new HttpGet(address);

        HttpResponse response = client.execute(_httpGet);
        try {
            log.info("Response : " + response.getStatusLine());
            HttpEntity entity = response.getEntity();
            _baseStream = entity.getContent();
        } catch (Exception e) {
            _baseStream = null;
        }
    }

    @Override
    public int read() throws IOException {
        if (_baseStream == null)
            throw new IOException("Failed to read from server");
        return _baseStream.read();
    }

    @Override
    protected void finalize() throws Throwable {
        if (_baseStream != null) {
            _baseStream.close();
        }
        if (_httpGet != null) {
            _httpGet.releaseConnection();
        }
        super.finalize();
    }
}
