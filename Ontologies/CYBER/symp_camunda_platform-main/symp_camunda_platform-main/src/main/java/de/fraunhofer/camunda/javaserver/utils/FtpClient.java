package de.fraunhofer.camunda.javaserver.utils;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTPClient;

public class FtpClient {
		private static String getRemoteFileName(String path) {
			return path.substring(path.lastIndexOf("/") + 1);
		}

		private static String getRemoteHost(String path) {
			path = path.substring(path.indexOf("//") + 2);
			if(path.indexOf(":") != -1) {
				return path.substring(0, path.indexOf(":"));
			} else {
				return path.substring(0, path.indexOf("/"));
			}
		}

		private static int getRemotePort(String path) {
			path = path.substring(path.indexOf("//") + 2);
			if(path.indexOf(":") != -1) {
				return Integer.parseInt(path.substring(path.indexOf(":") + 1, path.indexOf("/")));
			} else {
				return 21;
			}
		}

		private static String getRemoteDirectory(String path) {
			path = path.substring(path.indexOf("//") + 2);
			if(path.indexOf("/") != path.lastIndexOf("/")) {
				return path.substring(path.indexOf("/"), path.lastIndexOf("/"));
			} else {
				return "";
			}
		}
	
	   /**
	    * FTP-Client-Download.
	    * @return true falls ok
	    */
	   public static boolean download( String localResultFile,
	         String fullPath) throws IOException
	   {
	      FTPClient        ftpClient = new FTPClient();
	      FileOutputStream fos = null;
	      boolean          resultOk = true;

	      try {
	    	 System.out.println("Trying to connect to " + getRemoteHost(fullPath) + " on port " + getRemotePort(fullPath));
	         ftpClient.connect( getRemoteHost(fullPath), getRemotePort(fullPath) );
	         System.out.println( ftpClient.getReplyString() );
	         
			 //ftpClient.enterLocalPassiveMode();
	         //System.out.println( ftpClient.getReplyString() );
	         
	         resultOk &= ftpClient.login("anonymous", "");
	         System.out.println( ftpClient.getReplyString() );
	         
	         ftpClient.changeWorkingDirectory(getRemoteDirectory(fullPath));	         
	         System.out.println( ftpClient.getReplyString() );
	         
	         fos = new FileOutputStream( localResultFile );
	         resultOk &= ftpClient.retrieveFile( getRemoteFileName(fullPath), fos );	         
	         System.out.println( ftpClient.getReplyString() );
	         
	         resultOk &= ftpClient.logout();
	         System.out.println( ftpClient.getReplyString() );
	      } finally {
	         try { if( fos != null ) { fos.close(); } } catch( IOException e ) {/* nothing to do */}
	         ftpClient.disconnect();
	      }

	      return resultOk;
	   }

	   /**
	    * FTP-Client-Upload.
	    * @return true falls ok
	    */
	   public static boolean upload( InputStream fis, String fullPath) throws IOException
	   {
	      FTPClient       ftpClient = new FTPClient();
	      boolean         resultOk = true;

	      try {
	         ftpClient.connect( getRemoteHost(fullPath), getRemotePort(fullPath) );
	         System.out.println( ftpClient.getReplyString() );
	         
			 //ftpClient.enterLocalPassiveMode();
	         //System.out.println( ftpClient.getReplyString() );
	         
	         resultOk &= ftpClient.login("anonymous", "");	         
	         System.out.println( ftpClient.getReplyString() );
	         
	         ftpClient.changeWorkingDirectory(getRemoteDirectory(fullPath));	         
	         System.out.println( ftpClient.getReplyString() );
	         
	         resultOk &= ftpClient.storeFile( getRemoteFileName(fullPath), fis );	         
	         System.out.println( ftpClient.getReplyString() );
	         
	         resultOk &= ftpClient.logout();
	         System.out.println( ftpClient.getReplyString() );
	      } finally {
	         try { if( fis != null ) { fis.close(); } } catch( IOException e ) {/* nothing to do */}
	         ftpClient.disconnect();
	      }

	      return resultOk;
	   }
}
