package com.horstmeier.java.tftp;

/**
 *    Copyright 2011 Daniel Armbrust
 *    Copyright 2013 Jens Horstmeier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.net.io.FromNetASCIIOutputStream;
import org.apache.commons.net.io.ToNetASCIIInputStream;
import org.apache.commons.net.tftp.TFTP;
import org.apache.commons.net.tftp.TFTPAckPacket;
import org.apache.commons.net.tftp.TFTPDataPacket;
import org.apache.commons.net.tftp.TFTPErrorPacket;
import org.apache.commons.net.tftp.TFTPPacket;
import org.apache.commons.net.tftp.TFTPPacketException;
import org.apache.commons.net.tftp.TFTPReadRequestPacket;
import org.apache.commons.net.tftp.TFTPWriteRequestPacket;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * A fully multi-threaded tftp server.  Can handle multiple clients at the same time. Implements RFC 1350 and
 * wrapping block numbers for large file support.
 * 
 * To launch, just create an instance of the class.  An IOException will be thrown if the server fails to start 
 * for reasons such as port in use, port denied, etc. 
 * 
 * To stop, use the shutdown method.
 * 
 * To check to see if the server is still running (or if it stopped because of an error), call the isRunning() method.
 * 
 * @author <A HREF="mailto:daniel.armbrust@gmail.com">Dan Armbrust</A>
 * @author Jens Horstmeier
 *
 * File was modified by Jens Horstmeier. See src/TFTPServer.java for the original code.
 */

public class TFTPServer extends TFTPBaseServer
{
	private Logger log = Logger.getLogger(TFTPServer.class);

	public TFTPServer(File serverReadDirectory, File serverWriteDirectory, Mode mode) throws IOException
	{
		super(new DefaultFileMapper(serverReadDirectory, serverWriteDirectory, mode));
	}

	public TFTPServer(File serverReadDirectory, File serverWriteDirectory, int port, Mode mode) throws IOException
	{
		super(new DefaultFileMapper(serverReadDirectory, serverWriteDirectory, mode), port);
	}
}

