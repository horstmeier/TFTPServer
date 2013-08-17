package com.horstmeier.java.tftp;

import com.horstmeier.java.tftp.interfaces.IFileNameMapper;

import java.io.*;

/**
 * The DefaultFileMapper emulates the previous behaviour of the TFTP server.
 */
public class DefaultFileMapper implements IFileNameMapper {



    private final File serverReadDirectory_;
    private final File serverWriteDirectory_;
    private final Mode mode_;

    public DefaultFileMapper(File serverReadDirectory, File serverWriteDirectory, Mode mode) throws IOException {
        serverReadDirectory_ = serverReadDirectory.getCanonicalFile();
        serverWriteDirectory_ = serverWriteDirectory.getCanonicalFile();
        mode_ = mode;
    }


    @Override
    public InputStream openInputStream(String fileName) throws IOException {
        if (mode_ == Mode.PUT_ONLY) {
            throw new IOException("Reading is not allowed.");
        }
        File f = buildSafeFile(serverReadDirectory_, fileName, false);
        return new FileInputStream(f);
    }

    @Override
    public OutputStream openOutputStream(String fileName) throws IOException {
        if (mode_ == Mode.GET_ONLY) {
            throw new IOException("Writing is not allowed");
        }
        File f = buildSafeFile(serverWriteDirectory_, fileName, true);
        return new FileOutputStream(f);
    }

    @Override
    public boolean canRead() {
        return mode_ != Mode.PUT_ONLY;
    }

    @Override
    public boolean canWrite() {
        return mode_ != Mode.GET_ONLY;
    }

    /*
		 * Utility method to make sure that paths provided by tftp clients do not get outside of the serverRoot directory.
		 */
    private File buildSafeFile(File serverDirectory, String fileName, boolean createSubDirs) throws IOException
    {
        File temp = new File(serverDirectory, fileName);
        temp = temp.getCanonicalFile();

        if (!isSubdirectoryOf(serverDirectory, temp))
        {
            throw new IOException("Cannot access files outside of tftp server root.");
        }

        //ensure directory exists (if requested)
        if (createSubDirs)
        {
            createDirectory(temp.getParentFile());
        }

        return temp;
    }

    /*
     * recursively create subdirectories
     */
    private void createDirectory(File file) throws IOException
    {
        File parent = file.getParentFile();
        if (parent == null)
        {
            throw new IOException("Unexpected error creating requested directory");
        }
        if (!parent.exists())
        {
            //recurse...
            createDirectory(parent);
        }

        if (parent.isDirectory())
        {
            if (file.isDirectory())
            {
                return;
            }
            boolean result = file.mkdir();
            if (!result)
            {
                throw new IOException("Couldn't create requested directory");
            }
        }
        else
        {
            throw new IOException("Invalid directory path - file in the way of requested folder");
        }
    }

    /*
     * recursively check to see if one directory is a parent of another.
     */
    private boolean isSubdirectoryOf(File parent, File child)
    {
        File childsParent = child.getParentFile();
        return childsParent != null && (childsParent.equals(parent) || isSubdirectoryOf(parent, childsParent));
    }
}
