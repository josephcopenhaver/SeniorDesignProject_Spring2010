import java.net.Socket;
import java.net.ServerSocket;

class LogServer extends Thread
{
	private int port;
	private MyDB myDB = null;
	private ServerSocket ss = null;
	public LogServer(MyDB myDB, int port)
	{
		this.myDB = myDB;
		this.port = port;
	}
	
	public void run()
	{
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
         log("Connection Established");
         myDB.lockLogSocket();
         myDB.setLogServerSocket(new LockableSocket(myDB, s));
         myDB.unlockLogSocket();
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
      // Could be circular logic, big no no
      //myDB.lockLog();
      //myDB.log("SensorServer: " + msg);
      //myDB.unlockLog();
      System.err.println("LogServer: " + msg);
   }
   
}