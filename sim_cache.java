import java.io.IOException;

public class sim_cache {
	public static void main(String[] args) throws IOException {

		if (args.length >= 8) { //we need number of arguments equal to 8
			processCacheConfiguration(args); // Sample Input: 32 1024 1 0 0 0 0 gcc_trace.txt
		} else {
			System.out.println("Please enter only 8 arguments in order");
		}
	}
	private static void processCacheConfiguration(String[] args) throws IOException{
		//Getting parameters as input from command line arguments
		int bs=parse(args[0]); //block Size
		int l1s=parse(args[1]); //l1 Size
		int l1a=parse(args[2]); //l1 Associativity
		int l2s=parse(args[3]); //l2 Size
		int l2a=parse(args[4]); //l2 Associativity
		int rp=parse(args[5]); //Replacement Policy
		int ip=parse(args[6]); //Inclusion Property
		String trace=args[7]; //trace file

		//Creating an instance of CachingManager class with the parsed command-line arguments.
		 new CachingManager(bs, l1s, l1a, l2s, l2a, rp, ip, trace);
	}
	private static int parse(String arg){
		return Integer.parseInt(arg);
	}
}


