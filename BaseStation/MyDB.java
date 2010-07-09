import java.util.HashMap;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

class MyDB {
   private Sema logSema = new Sema();
   private Sema macHashSema = new Sema();
   private Sema lastMsgSema = new Sema();
   private Sema subscriberSema = new Sema();
   private Sema macRegistrySema = new Sema();
   private Sema logServerSema = new Sema();
   private HashMap<String,LockableSocket> macsToSockets = new HashMap<String, LockableSocket>();
   private HashMap<String,String> macsToLastMsg = new HashMap<String, String>();
   private HashMap<String,Boolean> registeredMacs = new HashMap<String, Boolean>();
   private LockableSocket subscriberSocket = null;
   private LockableSocket logServerSocket = null;
   private boolean reflectLock = false;
   private boolean reflectLogServerLock = false;
   private BufferedWriter out;
   private String logFileName;
   private boolean printLog;
   private boolean robustLog;
   
   public MyDB(String logFile, boolean printLog, boolean robustLog) throws IOException
   {
      this.logFileName = logFile;
      this.printLog = printLog;
      this.robustLog = robustLog;
      if (logFile != null)
      {
         out = new BufferedWriter(new FileWriter(logFile));
      }
   }
   
   public boolean isMacRegistered(String mac) {
   	  
   	  Boolean isRegistered = registeredMacs.get(mac);
   	  if (isRegistered == null)
   	  {
   	  	 return false;
   	  }
   	  return isRegistered;
   	  
   }
   
   public void registerMac(String mac) {
   	  
   	  registeredMacs.put(mac, true);
   	  
   }
   
   public void lockMacRegistry() {
      macRegistrySema.lock();
   }
   
   public void unlockMacRegistry() {
      macRegistrySema.unlock();
   }
   
   public void lockLog() {
      logSema.lock();
   }
   
   public void unlockLog() {
      logSema.unlock();
   }
   
   public void lockMacHash() {
      macHashSema.lock();
   }
   
   public void unlockMacHash() {
      macHashSema.unlock();
   }
   
   public void lockLastMsgHash() {
      lastMsgSema.lock();
   }
   
   public void unlockLastMsgHash() {
      lastMsgSema.unlock();
   }
   
   public void log(String msg) {
      //TODO: change to file dump
      String logEntry = System.currentTimeMillis() + ": " + msg;
      if (printLog)
      {
         if (logFileName != null)
         {
            try {
               out.write(logEntry + "\n");
               out.flush();
            }
            catch (Exception e)
            {
               System.out.print("^");
            }
         }
         System.out.println(logEntry);
      }
      lockLogSocket();
      if (logServerSocket != null && logServerSocket.isActive())
      {
      	logServerSocket.send(logEntry);
      }
      unlockLogSocket();
   }
   
   public LockableSocket getSocketByMac(String mac)
   {
      return macsToSockets.get(mac);
   }
   
   public void addMacSocketAssociation(String mac,  LockableSocket lsock)
   {
      macsToSockets.put(mac, lsock);
   }
   
   public void removeMacSocketAssociation(String mac)
   {
      macsToSockets.remove(mac);
   }
   
   public String getLastMsgByMac(String mac)
   {
      return macsToLastMsg.get(mac);
   }
   
   public void setLastMacMsg(String mac, String msg)
   {
      macsToLastMsg.put(mac, msg);
   }
   
   public void lockSubscriber()
   {
      subscriberSema.lock();
      if (subscriberSocket != null)
      {
         reflectLock = true;
         subscriberSocket.lockSend();
      }
      else
      {
         reflectLock = false;
      }
      
   }
   
   public void lockLogSocket()
   {
      logServerSema.lock();
      if (logServerSocket != null)
      {
         reflectLogServerLock = true;
         logServerSocket.lockSend();
      }
      else
      {
         reflectLogServerLock = false;
      }
      
   }
   
   public void setSubscriber(LockableSocket ls)
   {
      if (subscriberSocket != null)
      {
         if (reflectLock)
         {
            subscriberSocket.unlockSend();
         }
         subscriberSocket.close();
      }
      subscriberSocket = ls;
      reflectLock=false;
   }
   
   public void setLogServerSocket(LockableSocket ls)
   {
      if (logServerSocket != null)
      {
         if (reflectLogServerLock)
         {
            logServerSocket.unlockSend();
         }
         logServerSocket.close();
      }
      logServerSocket = ls;
      reflectLogServerLock=false;
   }
   
   public void unlockSubscriber()
   {
      if (reflectLock)
      {
         subscriberSocket.unlockSend();
      }
      subscriberSema.unlock();
   }
   
   public void unlockLogSocket()
   {
      if (reflectLogServerLock)
      {
         logServerSocket.unlockSend();
      }
      logServerSema.unlock();
   }
   
   public boolean sendSubscriber(String msg)
   {
      if (subscriberSocket != null && subscriberSocket.isActive())
      {
         if (!subscriberSocket.send(msg))
         {
         	//subscriberSocket.close();// Already handled in send failure code
         	setSubscriber(null);
         	return false;
         }
         return true;
      }
      return false;
   }
   
   public boolean isLogRobust()
   {
      return robustLog;
   }
   
}
