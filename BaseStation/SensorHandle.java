public class SensorHandle extends Thread
{
   
   private MyDB myDB;
   
   private LockableSocket srcSock;
   
   private String mac="";
   
   public SensorHandle(MyDB myDB, LockableSocket lsock)
   {
      super();
      this.myDB = myDB;
      srcSock = lsock;
   }
   
   public void run()
   {
      try
      {
         if (srcSock.isActive())
         {
            if (myDB.isLogRobust()) log("Sensor connected");
            String line = srcSock.readLine();
            if (myDB.isLogRobust()) log("Sensor says he is: " + line);
            if (line != null && !line.equals(""))
            {
               myDB.lockMacHash();
               LockableSocket ls = myDB.getSocketByMac(line);
               if (ls != null && ls.isActive())
               {
                  ls.close();
                  //myDB.unlockMacHash();
                  if (myDB.isLogRobust()) log("ID (" + line + ") was already in use, killing older");
               }
               //else
               if (true)
               {
                  mac=line;
                  srcSock.setMac(mac);
                  myDB.addMacSocketAssociation(line, srcSock);
                  myDB.unlockMacHash();
                  myDB.lockLastMsgHash();
                  line = myDB.getLastMsgByMac(line);
                  myDB.unlockLastMsgHash();
                  myDB.lockMacRegistry();
                  myDB.registerMac(mac);
                  myDB.unlockMacRegistry();
                  if (line != null && srcSock.isActive())
                  {
                     echo(line);
                  }
                  
                  while (srcSock.isActive() && (line = srcSock.readLine()) != null)
                  {
                     if (myDB.isLogRobust()) log("Got (" + line + ")");
                  	if (line.equals(""))
                  	{
                  		log("Invalid Message Format(" + line + ")");
                  	}
                  	else
                  	{
                  	  boolean wasSent = false;
                  	  while (!wasSent)
                  	  {
                     	myDB.lockSubscriber();
                        wasSent = myDB.sendSubscriber(line);
                     	myDB.unlockSubscriber();
                     	if (!wasSent)
                     	{
                          if (!srcSock.isActive()) break; // Our src connection died, he thinks it was never sent
                           try {sleep(5000);}catch(Exception e){}
                     	}
                        //System.out.println("err");
                  	  }
                     	if (wasSent)
                     	{
                           echo("");
                     	}
                  	}
                  }
               }
            }
            else
            {
               log("First line was null or empty");
            }
         }
         if (srcSock.isActive())
         {
            srcSock.close();
         }
      }
      catch (Exception e)
      {
         log("big exception");
      }
       _join();
   }
   private void echo(String msg)
   {
      if (myDB.isLogRobust()) log("Echoing: (" + msg + ")");
      srcSock.lockSend();
      srcSock.send(msg);
      srcSock.unlockSend();
   }
   
   private void log(String msg)
   {
   	  myDB.lockLog();
      myDB.log("SensorHandle(" + mac + "): " + msg);
      myDB.unlockLog();
   }
   public void _join()
   {
      if (srcSock != null)
      {
         srcSock.close();
      }
      try {
         join();
      }
      catch(Exception e){}
   }
   
   public boolean isRunnable()
   {
      return srcSock.isActive();
   }
}
