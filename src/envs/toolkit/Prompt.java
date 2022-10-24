package envs.toolkit;

import envs.config.GenericConfig;

public class Prompt {
	public static boolean SUPPRESS = false;
	
	private static String buildMessage(String head, String message, char color) {
		int hlen = head.length();
		StringBuilder headBuilder = new StringBuilder(head);
		for(int i = 0; i < GenericConfig.HLEN - hlen; i++) headBuilder.append(" ");
		head = headBuilder.toString();
		String header = ANSIHandler.wrapper(GenericConfig.TSTAMP ?
				String.format("[%s] %s |:", TimeKeeper.timeStamp(), head) :
				String.format(" %s |:", head), color);
		return String.format("%s  %s", header, message);
	}
	
	// universal standard for prompt line print
	public static void print_univ(String head, String message, char color) {
		if(!GenericConfig.TEST) if(!SUPPRESS) System.out.println(buildMessage(head, message, color));
	}
	
	public static void print(String head, String message){
		print_univ(head, message, 'C');
	}
	public static void print(String message){
		print(GenericConfig.PHEAD, message);
	}
	public static void warn(String message){
		print_univ("WARN", message, 'Y');
	}
	public static void talk(String head, String message) {
		if(GenericConfig.VERB) print_univ(head, message, 'c');
	}
	public static void talk(String message) {
		talk(GenericConfig.PHEAD, message);
	}
	public static void debug(String head, String message) {
		if(GenericConfig.DEV) print_univ(head, message, 'G');
	}
	public static void debug(String message) { 
		debug("DEBUG", message);
	}
	public static void test(String head, String message) {
		if(GenericConfig.TEST) if(!SUPPRESS) System.out.println(buildMessage(head, message, 'W'));
	}
	public static void test(String message) { 
		test("TEST", message);
	}
	
	
	// print with no new-line character
	public static void print_nnc(String head, String message){
		if(!GenericConfig.TEST) if(!SUPPRESS) System.out.print(buildMessage(head, message, 'C'));
	}
	public static void print_nnc(String message){ print_nnc(GenericConfig.PHEAD, message); }
	public static void dynamicHeader(String message) { if(!GenericConfig.VERB) print_nnc(message);}
	public static void dynamic(String message) { if(!GenericConfig.VERB) if(!SUPPRESS) System.out.print(message);}
	/*
	public static void erase(String msg, int sub) {
		for(int x = 0; x < msg.length() - sub; x++) System.out.print("\b");
		for(int x = 0; x < msg.length() - sub; x++) System.out.print(" ");
		System.out.flush();
		for(int x = 0; x < msg.length() - sub; x++) System.out.print("\b");
	}
	public static void erase(String msg) {erase(msg, 0);}
	*/
}
