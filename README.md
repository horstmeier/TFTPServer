TFTPServer
==========

A simple TFTP server implementation for Java. This project is a fork of the TFTPServer project by Dan Armbrust. This server was published (like this project) under an Apache 2.0 license.

The difference between the implementation by Dan and this project is that the file system is factored out. If you provide a class that implements IFileNameMapper, you can controll which kind of stream shall be returned to the sender.

I basically kept the original behaviour implemented by Dan Armbruster by adding a DefaultFileNameMapper that emulates his implementation.

