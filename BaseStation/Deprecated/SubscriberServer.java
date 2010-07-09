import java.util.concurrent.*;
import java.util.*;
import java.net.*;

class SubscriberServer extends Thread {
   
   ServerSocket ss;
   //SubscriberHandle sh = null;
   MyDB myDB = null;
   int port;
   
   public SubscriberServer(MyDB myDB, int port)
   {
      super();
      this.myDB = myDB;
      //ss = new ServerSocket(port);
      this.port = port;
   }
   
   public void run()
   {
      try {
         log("Subscriber Server:Initializing server");
         ss = new ServerSocket(port);
         log("Subscriber Server: server initialized");
      }
      catch(Exception e){log("Failed to initialize server"); _join();}
      
      try {
       //Socket s;
       log("Subscriber Server: Entering try");
       while (true)
       {
         log("Subscriber Server: waiting for socket");
         Socket s=ss.accept();
         log("new Subscriber");
         SubscriberHandle sh = new SubscriberHandle(myDB, new LockableSocket(myDB, s));
         if (sh.isRunnable())
         {
            log("Starting new Subscriber");
            sh.start();
            log("New Subscriber started!");
         }
         else 
         {
            SubscriberHandle.instanceOrderSema.unlock();
            log("Socket failed to be instantiated");
         }
       }
      }
      catch (Exception e)
      {
         log("Subscriber server socket died");
      }

   }
   
   private void log(String msg)
   {
      myDB.lockLog();
      myDB.log("SubscriberServer: " + msg);
      myDB.unlockLog();
   }
   
   public void _join()
   {
      log("Subscriber server closing");
      try {
         ss.close();
      }catch(Exception e){}
      //port = -1;
      //ss = null;
      //myDB = null;
      try {
         join();
      }
      catch(Exception e){}
   }
   
}
