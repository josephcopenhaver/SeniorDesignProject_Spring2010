/*
 * Easier to use semaphore
 * with thread queueing!
 *
 */

class Sema extends java.util.concurrent.Semaphore {
   
   public static final long serialVersionUID = 1L;
   
   public Sema() {
      
      super(1, true);
      
   }
   
   public void lock() {
      
      try {
         
         acquire();
         
      }
      catch(Exception e) {
         
         System.err.println("\tERR: Failed to lock a Semaphore");
         
         e.printStackTrace();
         
         System.exit(1);
         
      }
      
   }
   
   public void unlock() {
      
      release();
      
   }
   
}
