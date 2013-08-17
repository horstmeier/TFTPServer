package com.horstmeier.java.tftp.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstraction for the file system within the TFTP server.
 */
public interface IFileNameMapper {
    InputStream openInputStream(String fileName) throws IOException;
    OutputStream openOutputStream(String fileName) throws IOException;
    boolean canRead();
    boolean canWrite();
}
