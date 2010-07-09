import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.IOException;

public class EchoClient
{
    public static void main(String[] args)
    {

    	if (args.length != 2)
    	{
    		System.out.println("USAGE: <this_program> HOST_NAME PORT\n");
    		System.exit(1);
    	}
    	
        Socket s = null;
        BufferedWriter out = null;
        BufferedReader in = null;
        BufferedReader stdIn = null;
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        if (port <= 0)
        {
        	System.err.println("Port " + port + " is an invalid port");
        	System.exit(1);
        }
        
        System.out.println("Attempting to connect to " + host + " on port " + port + " ...");
        
        try
        {
            s = new Socket(host, port);
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        }
        catch (UnknownHostException e)
        {
            System.err.println("Failed to identify host: " + host);
            System.exit(1);
        }
        catch (IOException e)
        {
            System.err.println("Failed to secure the IO streams to host: " + host);
            System.exit(1);
        }
        
		try
		{
   			stdIn = new BufferedReader(new InputStreamReader(System.in));
			new ListenThread(in);
			String userInput;
			System.out.println("Connected!");
			while ((userInput = stdIn.readLine()) != null)
			{
				out.write(userInput + "\n");
				out.flush();
			}
			
			out.close();
			in.close();
			stdIn.close();
			s.close();
		}
		catch (Exception e)
		{
   			e.printStackTrace();
			try{out.close();}catch(Exception e2){}
			try{in.close();}catch(Exception e2){}
			try{stdIn.close();}catch(Exception e2){}
			try{s.close();}catch(Exception e2){}
   			System.err.println("Something went BANG in the writer thread, closing program.");
   			System.exit(1);
		}

    }
    
}
