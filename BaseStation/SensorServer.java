import java.net.Socket;
import java.net.ServerSocket;

public class SensorServer extends Thread
{
	
   ServerSocket ss;
   MyDB myDB = null;
   int port;
	
   public SensorServer(MyDB myDB, int port)
   {
      super();
      this.myDB=myDB;
      this.port=port;
   }
   
   public void run()
   {
   	log("Running: " + port);
   	while (true)
   	{
      try {
         //log("Sensor Server: Initializing server");
         ss = new ServerSocket(port);
         log("Serversocket Initialized");
      
      
      try {
       while (true)
       {
         Socket s=ss.accept();
         SensorHandle sh = new SensorHandle(myDB, new LockableSocket(myDB, s));
         if (sh.isRunnable())
         {
            sh.start();
         }
         else
         {
            log("A socket failed to have streams instantiated");
         }
       }
      }
      catch (Exception e2)
      {
         log("Serversocket failed to accept()");
      }
      }
      catch(Exception e)
      {
      	//log("Failed to create Serversocket");
      }
      // Try to close the dead socket or hanging duplicate
      try{ss.close();}catch(Exception e){}
      try
      {
      	sleep(5000);
      }
      catch(Exception e){}
   }
   }
   
   public void _join()
   {
      log("Thread closing");
      try {
         ss.close();
      }catch(Exception e){}
      try{
         join();
      }
      catch(Exception e){}
   }
   
   private void log(String msg)
   {
      myDB.lockLog();
      myDB.log("SensorServer: " + msg);
      myDB.unlockLog();
   }
}
