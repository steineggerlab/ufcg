package envs.toolkit;

import java.util.Calendar;
import java.text.SimpleDateFormat;

public class TimeKeeper {
	private static long iniTime;

	public TimeKeeper(){
		iniTime = System.nanoTime();
	}

	public int timePass(){
		return (int) ((System.nanoTime() - iniTime) / 1000000);
	}
	
	private String convert(long time) {
		if(time < 1000) return "< 1s"; // Millisecond
		time /= 1000;
		
		String tstr = "";
		tstr = time % 60 + "s " + tstr; // Second
		if(time < 60) return tstr;
		time /= 60;
		
		tstr = time % 60 + "m " + tstr; // Minute
		if(time < 60) return tstr;
		time /= 60;
		
		tstr = time % 24 + "h " + tstr; // Hour
		if(time < 24) return tstr;
		time /= 24;
		
		tstr = time % 30 + "d " + tstr; // Day
		if(time < 30) return tstr;
		time /= 30;
		
		tstr = time % 12 + "m " + tstr; // Month
		if(time < 12) return tstr;
		time /= 12;
		
		return time + "y " + tstr; // Year
	}
	public static String format(long time) {
		return (new TimeKeeper()).convert(time);
	}

	public String eta(int proc, int tot){
		if(proc == 0) return ANSIHandler.wrapper("inf", 'K');
		return ANSIHandler.wrapper(convert((long) timePass() * (tot - proc) / proc), 'K');
	}
	// public String elap() {return convert(timePass());}
	
	private static final String[] MONTHS = {
			"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
	};
	private static final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd HH:mm:ss");
	private static final SimpleDateFormat SDF_EXT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	public static String timeStamp() {
		String sdf = SDF.format(Calendar.getInstance().getTime());
		return MONTHS[Integer.parseInt(sdf.substring(0, 2)) - 1] + " " + sdf.substring(3);
	}
	public static String timeStampExtended() {return SDF_EXT.format(Calendar.getInstance().getTime());}
}
