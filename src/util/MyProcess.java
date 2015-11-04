package util;

/**
*
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
/**
* @author nlao
*
*/
public class MyProcess {
   public ProcessBuilder pb;

   public Process proc;
   public BufferedWriter out;
   public BufferedReader in;
   public BufferedReader err;
   public String cmd;
   
   public void printErr(){
       String s ;
       System.out.println("remote error message from: "    +cmd);
       //        +pb.toString());
     try{
    	
         while ((s = err.readLine()) != null) {
         System.out.println(s);
         err.close();
       }
     }catch(Exception ex){
       ex.printStackTrace();
     }           
   }
   public boolean write(String str){
   try{
       out.write(str,0,str.length());
       //out.newLine();
       out.flush();
   }catch(Exception ex){
       printErr();
     ex.printStackTrace();
     return false;
   }       
   return true;
   }
   
 
   public String readLine(){   
   try{	  
	  return in.readLine().trim();
     }catch(Exception ex){
//       printErr();
//       ex.printStackTrace();
       return "program ends";
     }           
   }
 
   
//   public String readLine() throws Exception{   	  
//		  return in.readLine().trim();
//   }
   
   public String dir=null;
   
   public void start(String ... args){
       //cmd = new VectorS(args).join(" ") ;
	   cmd = args[0];
	   for(int i = 1;i<args.length;i++){
		   cmd+=" "+args[i];
	   }
      System.out.println(cmd);
       try {
           pb = new ProcessBuilder(args);   
           if (dir != null)
               pb.directory(new File(dir));
           //pb.redirectErrorStream(true);
           proc = pb.start();
           out = new BufferedWriter(new OutputStreamWriter(
                   proc.getOutputStream(), "UTF8"));
           in = new BufferedReader(new InputStreamReader(
                   proc.getInputStream()));
           err = new BufferedReader(new InputStreamReader(
                   proc.getErrorStream()));
           //proc.getErrorStream().close();
       }
       catch (Exception ex) {
           ex.printStackTrace();
           System.exit(-1);
       } 
       //printErr();
   }   
 public void destroy(){
     try{
       out.close();
       in.close();
       err.close();
       proc.destroy();
       }
       catch (Exception ex) {
           ex.printStackTrace();
           System.exit(-1);
       }       
 }
 public String pushPop(String str){
   write(str);
   return readLine();
 }
}


class StreamGobbler extends Thread
{
    InputStream is; //Process.getInputStream() or Process.getErrorStream()
    String type;
    OutputStream os; //where you want to redirect the output of the external process
    
    StreamGobbler(InputStream is, String type)
    {
        this(is, type, null);
    }

    StreamGobbler(InputStream is, String type, OutputStream redirect)
    {
        this.is = is;
        this.type = type;
        this.os = redirect;
    }
    
    public void run()
    {
        try
        {
            PrintWriter pw = null;
            if (os != null)
                pw = new PrintWriter(os);
                
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
            {
                if (pw != null)
                    pw.println(line);
                System.out.println(type + ">" + line);    
            }
            if (pw != null)
                pw.flush();
        } catch (IOException ioe)
            {
            ioe.printStackTrace();  
            }
    }
}


