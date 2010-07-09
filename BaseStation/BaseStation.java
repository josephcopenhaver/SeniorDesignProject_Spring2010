import java.io.IOException;

class BaseStation extends Thread {
   
   private static String VERSION="1.3";
   private MyDB myDB = null;
   SensorServer ses = null;
   SubscriberTargeter st = null;
   //Integer logPort = null;
   
   ///int suport, seport;
   //int seport;
   
   private LogServer lsrv = null;
   
   public BaseStation(String log, boolean doPrints, boolean robustLog, Integer logPort, int seport, String target, int targetPort) throws IOException
   {
      super();
      myDB = new MyDB(log, doPrints, robustLog);
      //this.seport=seport;
      //this.suport=suport;
      //this.logPort = logPort;
      st = new SubscriberTargeter(myDB, target, targetPort);
      ses = new SensorServer(myDB, seport);
      if (logPort != null)
      {
      	lsrv = new LogServer(myDB, logPort);
      }
      
   }
   
   public void run()
   {
      try{
      
      log("VERSION=" + VERSION);
      
      if (lsrv != null)
      {
      	System.out.println("Starting log srv...");
      	lsrv.start();
      	System.out.println("Starting log srv started!");
      }
      
      System.out.println("Starting Subscriber Targeter ...");
      st.start();
      System.out.println("Subscriber Targeter started!");
      
      System.out.println("Starting Sensor server...");
      ses.start();
      System.out.println("Sensor server started!");
      
      
      st.join();
      ses.join();
      System.out.println("servers closed.");
      }
      catch(Exception e)
      {
         log("SUPER ERROR");
         e.printStackTrace();
         System.exit(1);
      }
      
   }
   
   public void _join()
   {
      log("Joining Main");
      
      try {
         
         join();
         
      }
      catch(Exception e) {
         
         log("Failed to join!");
         
         e.printStackTrace();
         
         System.exit(1);
         
      }
      
   }
   
   private void log(String msg)
   {
      myDB.lockLog();
      myDB.log("MAIN: " + msg);
      myDB.unlockLog();
   }
   
   public static void main(String[] args) throws IOException
   {
      String log = "server.log";
      String target = "127.0.0.1";
      int targetPort = 8000;
      boolean doPrints = true;
      boolean robustLog = false;
      //robustLog = true;
      int seport = 1429;
      int li = args.length - 1;
      Integer logPort = null;
      //logPort = 1425;
      for (int i=0;i<args.length;i++)
      {
         String s = args[i];
         if (s.equals("-h") || s.equals("--h") || s.equals("-?") || s.equals("--?") || s.equals("?") || s.equals("h") || s.equals("/?") || s.equals("/h"))
         {
            showUsageAndDie("");
         }
         if (s.equals("--robustlog"))
         {
            robustLog = true;
         }
         else if (s.equals("--nolog"))
         {
            log = null;
         }
         else if (s.equals("--nostdcopy"))
         {
            doPrints = false;
         }
         else if (i < li)
         {
            if (s.equals("--sensorport"))
            {
               seport = Integer.parseInt(args[++i]);
               if (seport < 1)
               {
                  showUsageAndDie(seport + " is not a valid sensor server port\n");
               }
            }
            else if (s.equals("--targetport"))
            {
               targetPort = Integer.parseInt(args[++i]);
               if (targetPort < 1)
               {
                  showUsageAndDie(targetPort + " is not a valid subscriber server port\n");
               }
            }
            else if (s.equals("--logport"))
            {
               logPort = Integer.parseInt(args[++i]);
               if (logPort < 1)
               {
                  showUsageAndDie(logPort + " is not a valid log server port\n");
               }
            }
            else if (s.equals("--target"))
            {
               target = args[++i];
            }
            else
            {
               showUsageAndDie("The option \"" + s + "\" is not recognized\n");
            }
         }
         else
         {
            showUsageAndDie("The option \"" + s + "\" is not recognized\n");
         }
      }
      
      
      BaseStation m = new BaseStation(log, doPrints, robustLog, logPort, seport, target, targetPort);
      m.run();
      
   }
   
   private static void showUsageAndDie(String err)
   {
   		System.err.println("\n" + err);
   		System.out.println("USAGE: java BaseStation\n\t--robustlog\n\t--nolog\n\t--nostdcopy\n\t--sensorport\t<#>\n\t--logport\t<#>\n\t--targetport\t<#>\n\t--target\t<IP|HOSTNAME>\n");
   		System.exit(1);
   }
   
}
