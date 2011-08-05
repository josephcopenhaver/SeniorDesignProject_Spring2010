import java.net.Socket;

class SubscriberTargeter extends Thread
{
   
   private MyDB myDB;
   private String target;
   private int targetPort;
   private LockableSocket ls = null;
   
   public SubscriberTargeter(MyDB myDB, String target, int targetPort)
   {
      super();
      this.myDB = myDB;
      this.target = target;
      this.targetPort = targetPort;
   }
   
   public void run()
   {
		log("Running - Attempting to connect to " + target + " on port " + targetPort);
		while (true)
		{
			try
   	  		{
	   	  		Socket s = new Socket(target, targetPort);
   	  			ls = new LockableSocket(myDB, s);
   	  			log("Connected to Subscriber");
   	  			myDB.lockSubscriber();
   	  			myDB.setSubscriber(ls);
   	  			myDB.unlockSubscriber();
				
   	  			String line;
   	  			while (ls.isActive() && (line = ls.readLine()) != null)
   	  			{
	   	  			int spltIdx = line.indexOf('|');
            		if (spltIdx > 0 && spltIdx < line.length())
            		{
	               		String mac = line.substring(0,spltIdx);
               			String msg = line.substring(spltIdx + 1,line.length());
               			if (msg.equals(""))
               			{
               				log("Improper Message Format(" + line + ")");
               			}
               			else
               			{
               				myDB.lockMacHash();
               				LockableSocket dstSocket = myDB.getSocketByMac(mac);
               				myDB.unlockMacHash();
							boolean sendWorked = false;
	               			if (dstSocket != null && dstSocket.isActive())
               				{
               					dstSocket.lockSend();
                  				sendWorked = dstSocket.send(msg);
                  				dstSocket.unlockSend();
                  			}
               				if (!sendWorked)
               				{
               					// Check if the mac has been registered
               					// If so, store the mode for when it
               					// attempts to reconnect.
								// 
								// The subscriber application must resend
								// the mode setting periodically as a result
								// if they receive an error response
								// for the targeted sensor mac.
								// 
								// I am keeping it this way so that if the subscriber
								// ever bugs out and thinks that there are a million
								// sensers available, but there are only a couple,
								// then our software does not slow down because the
								// database is full of false macs.
               					if (myDB.isMacRegistered(mac))
               					{
               						myDB.lockLastMsgHash();
               						myDB.setLastMacMsg(mac, msg);
               						myDB.unlockLastMsgHash();
               					}
               					else
               					{
		                  			echo("E|" + mac);
               					}
		                  		// We echo back this error message if we
		                  		// are unable to set the mode for a sensor
		                  		// termination (mac) because it has never been seen.
               				}
               			}
            		}
            		else
            		{
               			log("Improper Message Format(" + line + ")");
            		}
   	  			}
			ls.close();
   	  		log("Disconnected from Subscriber");
	   	  	myDB.lockSubscriber();
   	  		myDB.setSubscriber(null);
   	  		myDB.unlockSubscriber();
			
			}
			catch (Exception e)
			{
				//log("ST Exploded on init");
			}
			
			try
			{
				sleep(5000);
			}
			catch(Exception e2){}
			
		}
   	  
   }
   
   private void echo(String msg)
   {
      ls.lockSend();
      ls.send(msg);
      ls.unlockSend();
   }
   
   public void _join()
   {
      log("Thread closing");
      if (ls != null)
      {
         ls.close();
      }
      try
      {
         join();
      }
      catch(Exception e){}
   }
   
   private void log(String msg)
   {
      myDB.lockLog();
      myDB.log("SubscriberTargeter: " + msg);
      myDB.unlockLog();
   }
}
