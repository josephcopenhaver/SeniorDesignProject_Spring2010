import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

public class LockableSocket {
   private Socket sock;
   private Sema myLock = new Sema();
   private BufferedReader in = null;
   private InputStreamReader isr_in = null;
   private InputStream is_in = null;
   private BufferedWriter out = null;
   private OutputStreamWriter osw_out = null;
   private OutputStream os_out = null;
   private MyDB myDB = null;
   private String mac = null;
   private boolean active = true;
   
   public LockableSocket(MyDB myDB, Socket sock) {
      this.myDB=myDB;
      this.sock = sock;
      try{
         is_in = sock.getInputStream();
         isr_in = new InputStreamReader(is_in);
         in = new BufferedReader(isr_in);
         os_out = sock.getOutputStream();
         osw_out = new OutputStreamWriter(os_out);
         out = new BufferedWriter(osw_out);
      }
      catch (Exception e)
      {
         if (active)
         {
         close();
         }
      }
   }
   public void lockSend()
   {
      myLock.lock();
   }
   public void unlockSend()
   {
      myLock.unlock();
   }
   public boolean send(String msg)
   {
      //if (myDB.isLogRobust()) log("Sending (" + msg + ")");
      if (mac != null)
      {
         myDB.lockLastMsgHash();
         myDB.setLastMacMsg(mac, msg);
         myDB.unlockLastMsgHash();
      }
      try
      {
         out.write(msg + "\n");
         out.flush();
         return true;
      }
      catch (Exception e)
      {
		if (active)
		{
			close();
		}
      }
      return false;
   }
   public void setMac(String mac)
   {
      this.mac=mac;
   }
   
   public String readLine()
   {
      try {
         return in.readLine();
      }
      catch (Exception e)
      {
         if (active)
         {
	         close();
         }
         return null;
      }
   }
   
   public void close()
   {
		if (active)
		{
      		active=false;
			if (mac != null)
			{
				myDB.lockMacHash();
				myDB.removeMacSocketAssociation(mac);
				myDB.unlockMacHash();
			}
			try {is_in.close();}catch (Exception e){}
			try {out.close();}catch (Exception e){}
			try {osw_out.close();}catch (Exception e){}
			try {os_out.close();}catch (Exception e){}
			try {sock.close();}catch (Exception e){}
		}
   }
   
   public boolean isActive()
   {
      return active;
   }
   
   /**/
   private void log(String msg)
   {
      myDB.lockLog();
      myDB.log("LockableSocket: " + msg);
      myDB.unlockLog();
   }
   /**/
   
}
