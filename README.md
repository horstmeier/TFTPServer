TFTPServer
==========

A simple TFTP server implementation for Java. This project is a fork of the TFTPServer project by Dan Armbrust.
This server was published (like this project) under an Apache 2.0 license. Since Dan's implementation is no easy
to find, I have added his work as TFTPServer.original in this distribution.

The difference between the implementation by Dan and this project is that the file system is factored out.
If you provide a class that implements IFileNameMapper, you can controll which kind of stream shall be returned
to the sender.

I basically kept the original behaviour implemented by Dan Armbruster by adding a DefaultFileNameMapper that
emulates his implementation.

The project contains a class named ProxyFileMapper. This class can be used to redirect TFTP request to a http
server.

You need some code like this to use the ProxyFileMapper:

    Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout()));

    ProxyFileMapper proxyFileMapper = new ProxyFileMapper("http://google.com/");
    TFTPBaseServer baseServer = new TFTPBaseServer(proxyFileMapper, 8089);

Now all TFTP request for a file named "x/y/z" will be mapped to the http://google.com/x/y/z. (Please don't use
google.com in your production code. This address was only used as a neutral address for demonstration)
