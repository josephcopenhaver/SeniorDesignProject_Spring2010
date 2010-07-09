import java.io.BufferedReader;

class ListenThread extends Thread
{
   
   private BufferedReader in = null;
   
   public ListenThread(BufferedReader in)
   {
      this.in = in;
      start();
   }
   
   public void run()
   {
      String buff;
      try
      {
         while ((buff = in.readLine()) != null)
         {
            System.out.println(">>" + buff);
         }
      } catch(Exception e)
      {
         e.printStackTrace();
      }
      try{in.close();}catch(Exception e){}
      System.err.println("Input Stream went BANG!, closing program");
      System.exit(1);
   }
   
}
