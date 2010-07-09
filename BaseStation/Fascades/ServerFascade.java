import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.IOException;

public class ServerFascade
{
    public static void main(String[] args)
    {

    	if (args.length != 1)
    	{
    		System.out.println("USAGE: <this_program> PORT\n");
    		System.exit(1);
    	}
    	
        Socket s = null;
        BufferedWriter out = null;
        BufferedReader in = null;
        BufferedReader stdIn = null;
        ServerSocket ss = null;
        //String host = args[0];
        int port = Integer.parseInt(args[0]);
        if (port <= 0)
        {
        	System.err.println("Port " + port + " is an invalid port");
        	System.exit(1);
        }
        
        //System.out.println("Attempting to connect to " + host + " on port " + port + " ...");
        
        try
        {
            //s = new Socket(host, port);
            System.out.println("Creating Server Socket on port " + port);
            ss = new ServerSocket(port, 0);
            //new ServerSocket()
            System.out.println("Waiting for connection request");
            s = ss.accept();
            System.out.println("Setting up Outbound stream");
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            System.out.println("Setting up Inbound stream");
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        }
        catch (IOException e)
        {
            System.err.println("An IO Exception event was raised.");
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
