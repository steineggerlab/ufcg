package pipeline;

import envs.config.GenericConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;

public class ExceptionHandler {
	public static final int EXCEPTION				= 0xFF;
	public static final int UNKNOWN_OPTION			= 0x01;
	public static final int MISSING_ARGUMENT		= 0x02;
	public static final int NO_INPUT 				= 0x03;
	public static final int NO_OUTPUT				= 0x04;
	public static final int INVALID_FILE			= 0x05;
	public static final int INVALID_DIRECTORY		= 0x06;
	public static final int INVALID_BINARY			= 0x07;
	public static final int INVALID_VALUE			= 0x08;
	public static final int WRONG_CORE_FORMAT		= 0x09;
	public static final int INVALID_GENE_NAME		= 0x0A;
	public static final int DEPENDENCY_UNSOLVED		= 0x0B;
	public static final int CONFIG_PATH_UNDEFINED	= 0x0C;
	public static final int INVALID_PPX_CONFIG		= 0x0D;
	public static final int INVALID_PROFILE_PATH	= 0x0E;
	public static final int INVALID_META_HEADER		= 0x0F;
	public static final int INSUFFICIENT_METADATA	= 0x10;
	public static final int INVALID_METADATA		= 0x11;
	public static final int INTEGRITY_TEST_FAILED	= 0x12;
	public static final int METAINFO_CONFLICT		= 0x13;
	public static final int INVALID_METAINFO		= 0x14;
	public static final int UNKNOWN_MODULE			= 0x15;
	
	private static Object OBJ;
	public static void pass(Object obj) {OBJ = obj;}
	
	private static Exception E = null;
	private static void printStackTrace() {
		System.out.println(E.getClass().getCanonicalName() + ": " + E.getMessage());
		StackTraceElement[] stes = E.getStackTrace();
		for(StackTraceElement ste : stes) {
			Prompt.print("\tat " + ste.toString());
		}
	}
	public static void handle(int exception) {
		Prompt.print_nnc(ANSIHandler.wrapper("ERROR! ", 'r'));
		switch(exception) {
		case EXCEPTION:
			printStackTrace(); break;
		case UNKNOWN_OPTION:
			System.out.println("Unrecognized option given : " + OBJ.toString()); break;
		case MISSING_ARGUMENT:
			System.out.println("Option with missing argument exists : -" + OBJ.toString()); break;
		case NO_INPUT:
			System.out.println("No input file given."); break;
		case NO_OUTPUT:
			System.out.println("No output directory given."); break;
		case INVALID_FILE:
			System.out.println("Invalid file given : " + ANSIHandler.wrapper(OBJ.toString(), 'B')); break;
		case INVALID_DIRECTORY:
			System.out.println("Invalid directory given : " + ANSIHandler.wrapper(OBJ.toString(), 'B')); break;
		case INVALID_BINARY:
			System.out.println("Invalid binary executable file given : " + ANSIHandler.wrapper(OBJ.toString(), 'B')); break;
		case INVALID_VALUE:
			System.out.println("Invalid value given : " + ANSIHandler.wrapper(OBJ.toString(), 'B')); break;
		case WRONG_CORE_FORMAT:
			System.out.println("Custom core gene list is improperly given."); break;
		case INVALID_GENE_NAME:
			System.out.println("Invalid gene name given : " + ANSIHandler.wrapper(OBJ.toString(), 'B')); break;
		case DEPENDENCY_UNSOLVED:
			System.out.println("Following dependency binary remains unsolved : " + ANSIHandler.wrapper(OBJ.toString(), 'B')); break;
		case CONFIG_PATH_UNDEFINED:
			System.out.println("AUGUSTUS_CONFIG_PATH undefined or imporoperly defined."); break;
		case INVALID_PPX_CONFIG:
			System.out.println("AUGUSTUS-PPX config file is absent or improperly formatted."); break;
		case INVALID_PROFILE_PATH:
			System.out.println("Core gene profile directory is incomplete."); break;
		case INVALID_META_HEADER:
			System.out.println("Unknown metadata header given : " + ANSIHandler.wrapper(OBJ.toString(), 'B')); break;
		case INSUFFICIENT_METADATA:
			System.out.println("Insufficient data given. Following data must be included : " +  ANSIHandler.wrapper(OBJ.toString(), 'B')); break;
		case INVALID_METADATA:
			System.out.println("Metadata file is improperly formatted."); break;
		case INTEGRITY_TEST_FAILED:
			System.out.println("Bad file integrity : " + OBJ.toString()); break;
		case METAINFO_CONFLICT:
			System.out.println("--metainfo option cannot be given with the directory input or --metadata option."); break;
		case INVALID_METAINFO:
			System.out.println("Metadata information is improperly formatted."); break;
		case UNKNOWN_MODULE:
			System.out.println("Unknown module given : " + OBJ.toString()); break;
		}
		
		if(!GenericConfig.INTERACT || exception == EXCEPTION) {
			switch(exception) {
			case EXCEPTION: 		break;
			case INVALID_GENE_NAME:	Prompt.print("Use --core option to check out the valid genes.\n"); break;
			default: 				Prompt.print("Run with \"" + GenericConfig.getModuleName() + " -h\" option to see the user manual.\n");
			}
			System.exit(1);
		}
	}
	public static void handle(Exception e) {
		E = e;
		handle(EXCEPTION);
	}
}
