package com.horstmeier.java.tftp.proxyserver;

import com.horstmeier.java.tftp.interfaces.IFileNameMapper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by jens on 17.08.13.
 */
public class ProxyFileMapper implements IFileNameMapper {

    private String _serverBaseAddress;
    private Logger log = Logger.getLogger(ProxyFileMapper.class);
    public ProxyFileMapper(String serverBaseAddress) {
        _serverBaseAddress = serverBaseAddress.endsWith("/") ? serverBaseAddress : serverBaseAddress + "/";
    }

    @Override
    public InputStream openInputStream(String fileName) throws IOException {
        InputStream result;
        String address = _serverBaseAddress + fileName;

        return new EntityInputStream(address);
    }

    @Override
    public OutputStream openOutputStream(String fileName) throws IOException {
        return null;
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
