package de.Ryeera.Thready;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class DragoLogger {
	
	private File logFile;
	private PrintWriter log;
	
	public DragoLogger() {
		this.logFile = null;
		this.log = null;
	}
	
	public DragoLogger(File logFile) throws FileNotFoundException {
		this.logFile = logFile;
		log = new PrintWriter(this.logFile);
	}
	
	public void logStackTrace(Exception e) {
		log("EXCEPTION", e.toString());
		for(StackTraceElement s : e.getStackTrace()) {
			log("STACKTRACE", "at " + s.getClassName() + "." + s.getMethodName() + ":" + s.getLineNumber());
		}
	}
	
	public void log(String level, String text){
		System.out.println("[" + DragoUtils.formatTime(System.currentTimeMillis(), "dd.MM.yy HH:mm:ss") + "] [" + level + "] " + text);
		if (log != null) {
			log.println("[" + DragoUtils.formatTime(System.currentTimeMillis(), "dd.MM.yy HH:mm:ss") + "] [" + level + "] " + text);
			log.flush();
		}
	}
}
