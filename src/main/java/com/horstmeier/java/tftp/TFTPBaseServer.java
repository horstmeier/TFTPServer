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

/*
 * This file is based on the great TFTP server implementation from Daniel Armbrust. It has been modified by
 * me to fit my personal needs. Basically I refactored to code to abstract away the concrete file system. This
 * allows me to server 'virtual' files if needed. In order to use this class you have to implement the IFileNameMapper
 * interface.
 *
 * The original implementation by Daniel Armbrust can be found at
 *
 * http://armbrust.dyndns.org/programs/codeSnippits/TFTPServer.java
 *
 * The version I used to create this version can be found at src/TFTPServer.java
 */

import com.horstmeier.java.tftp.interfaces.IFileNameMapper;
import org.apache.commons.net.io.FromNetASCIIOutputStream;
import org.apache.commons.net.io.ToNetASCIIInputStream;
import org.apache.commons.net.tftp.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;

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
 */

public class TFTPBaseServer implements Runnable
{
	private Logger log = Logger.getLogger(TFTPBaseServer.class);

	private final HashSet<TFTPTransfer> transfers_ = new HashSet<TFTPTransfer>();
	private volatile boolean shutdown_ = false;
	private TFTP serverTftp_;

    private final IFileNameMapper fileNameMapper_;

	private int port_;
	private Exception serverException = null;

	private int maxTimeoutRetries_ = 3;
	private int socketTimeout_;

	/**
	 * Start a TFTP Server on the default port (69).  Gets and Puts occur in the specified directory.
	 *
	 * The server will start in another thread, allowing this constructor to return immediately.
	 *
	 * If a get or a put comes in with a relative path that tries to get outside of the serverDirectory,
	 * then the get or put will be denied.
	 *
	 * GET_ONLY mode only allows gets, PUT_ONLY mode only allows puts, and GET_AND_PUT allows both.
	 *
	 * @param fileNameMapper An abstraction for the file system
     * @throws IOException If the server could not open the port
	 */
	public TFTPBaseServer(IFileNameMapper fileNameMapper) throws IOException
	{
        this(fileNameMapper, 69);
	}

	/**
	 * Start a TFTP Server on the specified port.  Gets and Puts occur in the specified directory.
	 *
	 * The server will start in another thread, allowing this constructor to return immediately.
	 *
	 * If a get or a put comes in with a relative path that tries to get outside of the serverDirectory,
	 * then the get or put will be denied.
	 *
	 * GET_ONLY mode only allows gets, PUT_ONLY mode only allows puts, and GET_AND_PUT allows both.
	 *
	 * @param fileNameMapper An abstraction for the file system
     * @param port The IP port to use
     * @throws IOException If the server could not open the port
	 */
	public TFTPBaseServer(IFileNameMapper fileNameMapper, int port) throws IOException
	{
        fileNameMapper_ = fileNameMapper;
        port_ = port;
		launch();
	}

	/**
	 * Set the max number of retries in response to a timeout.  Default 3.  Min 0.
	 * @param retries Number of retries
	 */
	public void setMaxTimeoutRetries(int retries)
	{
		if (retries < 0)
		{
			throw new RuntimeException("Invalid Value");
		}
		maxTimeoutRetries_ = retries;
	}

	/**
	 * Get the current value for maxTimeoutRetries
	 */
	public int getMaxTimeoutRetries()
	{
		return maxTimeoutRetries_;
	}

	/**
	 * Set the socket timeout in milliseconds used in transfers.  Defaults to the value here:
	 * http://commons.apache.org/net/apidocs/org/apache/commons/net/tftp/TFTP.html#DEFAULT_TIMEOUT
	 * (5000 at the time I write this)
	 * Min value of 10.
	 */
	public void setSocketTimeout(int timeout)
	{
		if (timeout < 10)
		{
			throw new RuntimeException("Invalid Value");
		}
		socketTimeout_ = timeout;
	}

	/**
	 * The current socket timeout used during transfers in milliseconds.
	 */
	public int getSocketTimeout()
	{
		return socketTimeout_;
	}

	/*
	 * start the server, throw an error if it can't start.
	 */
	private void launch() throws IOException
	{
		log.debug("Starting TFTP Server on port " + port_ + ".");

		serverTftp_ = new TFTP();

		//This is the value used in response to each client.
		socketTimeout_ = serverTftp_.getDefaultTimeout();

		//we want the server thread to listen forever.
		serverTftp_.setDefaultTimeout(0);

		serverTftp_.open(port_);

		Thread go = new Thread(this, "TFTPServer");
		go.setDaemon(true);
		go.start();
	}

	@Override
	protected void finalize() throws Throwable
	{
		shutdown();
        super.finalize();
	}

	/**
	 * check if the server thread is still running.
	 * @return true if running, false if stopped.
	 * @throws Exception throws the exception that stopped the server if the server is stopped from an exception.
	 */
	public boolean isRunning() throws Exception
	{
		if (shutdown_ && serverException != null)
		{
			throw serverException;
		}
		return !shutdown_;
	}

	public void run()
	{
		try
		{
			while (!shutdown_)
			{
				TFTPPacket tftpPacket;

				tftpPacket = serverTftp_.receive();

				TFTPTransfer tt = new TFTPTransfer(tftpPacket);
				synchronized(transfers_)
				{
					transfers_.add(tt);
				}

				Thread thread = new Thread(tt, "TFTPTransfer-" + tftpPacket.getAddress());
				thread.setDaemon(true);
				thread.start();
			}
		}
		catch (Exception e)
		{
			if (!shutdown_)
			{
				serverException = e;
				log.error("Unexpected Error in TFTP Server - Server shut down!", e);
			}
		}
		finally
		{
			shutdown_ = true;  //set this to true, so the launching thread can check to see if it started.
			if (serverTftp_ != null && serverTftp_.isOpen())
			{
				serverTftp_.close();
			}
		}
	}

	/**
	 * Stop the tftp server (and any currently running transfers) and release all opened network resources.
	 */
	public void shutdown()
	{
		shutdown_ = true;

		synchronized(transfers_)
		{
            for (TFTPTransfer aTransfers_ : transfers_) {
                aTransfers_.shutdown();
            }
		}

		try
		{
			serverTftp_.close();
		}
		catch (RuntimeException e)
		{
			// noop
		}
	}

	/*
	 * An instance of an ongoing transfer.
	 */
	private class TFTPTransfer implements Runnable
	{
		private TFTPPacket tftpPacket_;
		private boolean shutdown_ = false;
		TFTP transferTftp_ = null;

		public TFTPTransfer(TFTPPacket tftpPacket)
		{
			tftpPacket_ = tftpPacket;
		}

		public void shutdown()
		{
			shutdown_ = true;
			try
			{
				transferTftp_.close();
			}
			catch (RuntimeException e)
			{
				// noop
			}
		}

		public void run()
		{
			try
			{
                setupTransferTftp();

				if (tftpPacket_ instanceof TFTPReadRequestPacket)
				{
					handleRead(((TFTPReadRequestPacket) tftpPacket_));
				}
				else if (tftpPacket_ instanceof TFTPWriteRequestPacket)
				{
					handleWrite((TFTPWriteRequestPacket) tftpPacket_);
				}
				else
				{
					log.debug("Unsupported TFTP request (" + tftpPacket_ + ") - ignored.");
				}
			}
			catch (Exception e)
			{
				if (!shutdown_)
				{
					log.warn("Unexpected Error during TFTP file transfer.  Transfer aborted.", e);
				}
			}
			finally
			{
				try
				{
                    shutdownTransferTftp();
                }
				catch (Exception e)
				{
					//noop
				}
				synchronized(transfers_)
				{
					transfers_.remove(this);
				}
			}
		}

        private void shutdownTransferTftp() {
            if (transferTftp_ != null && transferTftp_.isOpen())
            {
                transferTftp_.endBufferedOps();
                transferTftp_.close();
            }
        }

        private void setupTransferTftp() throws SocketException {
            transferTftp_ = new TFTP();

            transferTftp_.beginBufferedOps();
            transferTftp_.setDefaultTimeout(socketTimeout_);

            transferTftp_.open();
        }

        /*
         * Handle a tftp read request.
         */
		private void handleRead(TFTPReadRequestPacket trrp) throws IOException, TFTPPacketException
		{
			InputStream is = null;
			try
			{
				if (!fileNameMapper_.canRead())
				{
					transferTftp_.bufferedSend(new TFTPErrorPacket(trrp.getAddress(), trrp.getPort(), TFTPErrorPacket.ILLEGAL_OPERATION, "Read not allowed by server."));
					return;
				}

				try
				{
                    InputStream inputStream = fileNameMapper_.openInputStream(trrp.getFilename());
                    if (inputStream == null)
                        throw new FileNotFoundException(trrp.getFilename());
					is = new BufferedInputStream(inputStream);
				}
				catch (FileNotFoundException e)
				{
					transferTftp_.bufferedSend(new TFTPErrorPacket(trrp.getAddress(), trrp.getPort(), TFTPErrorPacket.FILE_NOT_FOUND, e.getMessage()));
					return;
				}
				catch (Exception e)
				{
					transferTftp_.bufferedSend(new TFTPErrorPacket(trrp.getAddress(), trrp.getPort(), TFTPErrorPacket.UNDEFINED, e.getMessage()));
					return;
				}

				if (trrp.getMode() == TFTP.NETASCII_MODE)
				{
					is = new ToNetASCIIInputStream(is);
				}

				byte[] temp = new byte[TFTPDataPacket.MAX_DATA_LENGTH];

				TFTPPacket answer;

				int block = 1;
				boolean sendNext = true;

				int readLength = TFTPDataPacket.MAX_DATA_LENGTH;

				TFTPDataPacket lastSentData = null;

				// We are reading a file, so when we read less than the
				// requested bytes, we know that we are at the end of the file.

                while (readLength == TFTPDataPacket.MAX_DATA_LENGTH && !shutdown_)
				{
					if (sendNext)
					{
						readLength = is.read(temp);
						if (readLength == -1)
						{
							readLength = 0;
						}

						lastSentData = new TFTPDataPacket(trrp.getAddress(), trrp.getPort(), block, temp, 0, readLength);
						transferTftp_.bufferedSend(lastSentData);
					}

					answer = null;

					int timeoutCount = 0;

					while (!shutdown_ && (answer == null || !answer.getAddress().equals(trrp.getAddress()) || answer.getPort() != trrp.getPort()))
					{
						// listen for an answer.
						if (answer != null)
						{
							// The answer that we got didn't come from the expected source, fire back an error, and continue listening.
							log.debug("TFTP Server ignoring message from unexpected source.");
							transferTftp_.bufferedSend(new TFTPErrorPacket(answer.getAddress(), answer.getPort(), TFTPErrorPacket.UNKNOWN_TID, "Unexpected Host or Port"));
						}
						try
						{
							answer = transferTftp_.bufferedReceive();
						}
						catch (SocketTimeoutException e)
						{
							if (timeoutCount >= maxTimeoutRetries_)
							{
								throw e;
							}
							//didn't get an ack for this data.  need to resend it.
							timeoutCount++;
							transferTftp_.bufferedSend(lastSentData);
                        }
					}

					if (answer == null || !(answer instanceof TFTPAckPacket))
					{
						if (!shutdown_)
						{
							log.error("Unexpected response from tftp client during transfer (" + answer + ").  Transfer aborted.");
						}
						break;
					}
					else
					{
						// once we get here, we know we have an answer packet from the correct host.
						TFTPAckPacket ack = (TFTPAckPacket) answer;
						if (ack.getBlockNumber() != block)
						{
							/*
							 * The origional tftp spec would have called on us to resend the previous data here,
							 * however, that causes the SAS Syndrome. http://www.faqs.org/rfcs/rfc1123.html section 4.2.3.1
							 * The modified spec says that we ignore  a duplicate ack.  If the packet was really lost, we will
							 * time out on receive, and resend the previous data at that point.
							 */
							sendNext = false;
						}
						else
						{
							// send the next block
							block++;
							if (block > 65535)
							{
								//wrap the block number
								block = 0;
							}
							sendNext = true;
						}
					}
				}
			}
			finally
			{
				try
				{
					if (is != null)
					{
						is.close();
					}
				}
				catch (IOException e)
				{
					// noop
				}
			}
		}

		/*
		 * handle a tftp write request.
		 */
		private void handleWrite(TFTPWriteRequestPacket twrp) throws IOException, TFTPPacketException
		{
			OutputStream bos = null;
			try
			{
				if (!fileNameMapper_.canWrite())
				{
					transferTftp_.bufferedSend(new TFTPErrorPacket(twrp.getAddress(), twrp.getPort(), TFTPErrorPacket.ILLEGAL_OPERATION, "Write not allowed by server."));
					return;
				}

				int lastBlock = 0;
				String fileName = twrp.getFilename();

				try
				{
                    OutputStream outputStream = fileNameMapper_.openOutputStream(fileName);

					if (outputStream == null)
					{
						transferTftp_.bufferedSend(new TFTPErrorPacket(twrp.getAddress(), twrp.getPort(), TFTPErrorPacket.FILE_EXISTS, "File already exists"));
						return;
					}
					bos = new BufferedOutputStream(outputStream);

					if (twrp.getMode() == TFTP.NETASCII_MODE)
					{
						bos = new FromNetASCIIOutputStream(bos);
					}
				}
				catch (Exception e)
				{
					transferTftp_.bufferedSend(new TFTPErrorPacket(twrp.getAddress(), twrp.getPort(), TFTPErrorPacket.UNDEFINED, e.getMessage()));
					return;
				}

				TFTPAckPacket lastSentAck = new TFTPAckPacket(twrp.getAddress(), twrp.getPort(), 0);
				transferTftp_.bufferedSend(lastSentAck);

				while (true)
				{
					//get the response - ensure it is from the right place.
					TFTPPacket dataPacket = null;

					int timeoutCount = 0;

					while (!shutdown_ && (dataPacket == null || !dataPacket.getAddress().equals(twrp.getAddress()) || dataPacket.getPort() != twrp.getPort()))
					{
						// listen for an answer.
						if (dataPacket != null)
						{
							// The data that we got didn't come from the expected source, fire back an error, and continue listening.
							log.debug("TFTP Server ignoring message from unexpected source.");
							transferTftp_.bufferedSend(new TFTPErrorPacket(dataPacket.getAddress(), dataPacket.getPort(), TFTPErrorPacket.UNKNOWN_TID, "Unexpected Host or Port"));
						}

						try
						{
							dataPacket = transferTftp_.bufferedReceive();
						}
						catch (SocketTimeoutException e)
						{
							if (timeoutCount >= maxTimeoutRetries_)
							{
								throw e;
							}
							// It didn't get our ack.  Resend it.
							transferTftp_.bufferedSend(lastSentAck);
							timeoutCount++;
                        }
					}

					if (dataPacket != null && dataPacket instanceof TFTPWriteRequestPacket)
					{
						//it must have missed our initial ack.  Send another.
						lastSentAck = new TFTPAckPacket(twrp.getAddress(), twrp.getPort(), 0);
						transferTftp_.bufferedSend(lastSentAck);
					}
					else if (dataPacket == null || !(dataPacket instanceof TFTPDataPacket))
					{
						if (!shutdown_)
						{
							log.error("Unexpected response from tftp client during transfer (" + dataPacket + ").  Transfer aborted.");
						}
						break;
					}
					else
					{
						int block = ((TFTPDataPacket)dataPacket).getBlockNumber();
						byte[] data = ((TFTPDataPacket)dataPacket).getData();
						int dataLength = ((TFTPDataPacket)dataPacket).getDataLength();
						int dataOffset = ((TFTPDataPacket)dataPacket).getDataOffset();

						if (block > lastBlock || (lastBlock == 65535 && block == 0))
						{
							//it might resend a data block if it missed our ack - don't rewrite the block.
							bos.write(data, dataOffset, dataLength);
							lastBlock = block;
						}

						lastSentAck = new TFTPAckPacket(twrp.getAddress(), twrp.getPort(), block);
						transferTftp_.bufferedSend(lastSentAck);
						if (dataLength < TFTPDataPacket.MAX_DATA_LENGTH)
						{
							//end of stream signal - The tranfer is complete.
							bos.close();

							//But my ack may be lost - so listen to see if I need to resend the ack.
							for (int i = 0; i < maxTimeoutRetries_; i++)
							{
								try
								{
									dataPacket = transferTftp_.bufferedReceive();
								}
								catch (SocketTimeoutException e)
								{
									//this is the expected route - the client shouldn't be sending any more packets.
									break;
								}

								if (dataPacket != null && (!dataPacket.getAddress().equals(twrp.getAddress()) || dataPacket.getPort() != twrp.getPort()))
								{
									//make sure it was from the right client...
									transferTftp_.bufferedSend(new TFTPErrorPacket(dataPacket.getAddress(), dataPacket.getPort(), TFTPErrorPacket.UNKNOWN_TID, "Unexpected Host or Port"));
								}
								else
								{
									// This means they sent us the last datapacket again, must have missed our ack.  resend it.
									transferTftp_.bufferedSend(lastSentAck);
								}
							}

							//all done.
							break;
						}
					}
				}
			}
			finally
			{
				if (bos != null)
				{
					bos.close();
				}
			}
		}
    }
}

