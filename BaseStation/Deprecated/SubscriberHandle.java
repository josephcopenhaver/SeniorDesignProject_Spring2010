import java.net.*;
import java.io.*;
class SubscriberHandle extends Thread {
   
   private MyDB myDB;
   
   private LockableSocket srcSock;
   
   public static Sema instanceOrderSema = new Sema();
   
   public SubscriberHandle(MyDB myDB, LockableSocket sock)
   {
      super();
      this.myDB = myDB;
      srcSock = sock;
      instanceOrderSema.lock();
   }
   
   public void run()
   {
      log("SOH: running");
      myDB.lockSubscriber();
      instanceOrderSema.unlock();
      log("SOH: locking dblsc");
      //boolean v=myDB.closeSubscriber();
      myDB.setSubscriber(srcSock);
      log("SOH: setting subscriber");
      //myDB.setSubscriber(srcSock);
      log("SOH: unlocking dblsc");
      myDB.unlockSubscriber();
      log("SOH: entering try");
      
      try
      {
         String line;
         while (srcSock.isActive() && ((line = srcSock.readLine()) != null))
         {
            log("SOH: data");
            //String [] params = line.split("[|]");
            int spltIdx = line.indexOf('|');
            if (spltIdx > 0 && spltIdx < line.length())
            {
               String mac = line.substring(0,spltIdx);
               String msg = line.substring(spltIdx + 1,line.length());
               //myDB.unlockLog();
               //}
               log("getHashLock");
               myDB.lockMacHash();
               log("getHash");
               LockableSocket dstSocket = myDB.getSocketByMac(mac);
               log("releaseHashLock");
               myDB.unlockMacHash();
               log("valid");
               if (dstSocket != null && dstSocket.isActive())
               {
                  dstSocket.lockSend();
                  dstSocket.send(msg);
                  dstSocket.unlockSend();
                  //echo("I told him");
               }
               else 
               {
                  echo("E|" + mac);
               }
               
               //myDB.unlockLog();
            }
            else
            {
               log("Improper Message Format(" + line + ")");
               log("Subscriber msg unknown: " + line);
               //echo("what u talkin bout willis?");
            }
         }
         log("sock closed");
      }
      catch (Exception e)
      {
         log("big exception in SUH");
         e.printStackTrace();
      }
      _join();
      
   }
   
   private void echo(String msg)
   {
      srcSock.lockSend();
      srcSock.send(msg);
      srcSock.unlockSend();
   }
   
   public void _join()
   {
      log("Subscriber handle closing");
      if (srcSock != null)
      {
         srcSock.close();
      }
      //srcSock = null;
      //myDB = null;
      try {
         join();
      }
      catch(Exception e){}
   }
   
   public boolean isRunnable()
   {
      return srcSock.isActive();
   }
   
   private void log(String msg)
   {
      myDB.lockLog();
      myDB.log("SubscriberHandle: " + msg);
      myDB.unlockLog();
   }
}
