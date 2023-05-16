package envs.toolkit;

import pipeline.ExceptionHandler;

import java.io.File;
import java.io.IOException;

public class Checkpoint {
    final String ckpFile;
    public Checkpoint(String ckpFile) {
        this.ckpFile = ckpFile;
    }

    private String module = null;
    public void setModule(String module) {
        this.module = module;
    }

    public int read() {
        try {
            if (!(new File(ckpFile)).exists()) {
                return -1;
            }
            FileStream ckpStream = new FileStream(ckpFile, 'r');
            int ret = Integer.parseInt(ckpStream.readLine().split(" ")[1]);
            ckpStream.close();
            return ret;
        } catch (IOException ioe) {
            Prompt.debug("Failed to read checkpoint file : " + ANSIHandler.wrapper(ckpFile, 'B'));
            return -1;
        } catch (NumberFormatException nfe) {
            Prompt.debug("Failed to read checkpoint integer from : " + ANSIHandler.wrapper(ckpFile, 'B'));
            return -1;
        } catch (Exception e) {
            ExceptionHandler.handle(e);
            return -1;
        }
    }

    public void log(String module, int state) {
        try {
            File ckp = new File(ckpFile);
            if(ckp.exists() && !ckp.delete()) {
                ExceptionHandler.pass("Failed to delete checkpoint file : " + ckpFile);
                ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
            }
            FileStream ckpStream = new FileStream(ckpFile, 'w');
            ckpStream.println(String.format("%s %d", module, state));
            ckpStream.close();
        } catch (java.io.IOException e) {
            ExceptionHandler.handle(e);
        }
    }
    public void log(int state) {
        if (this.module == null) {
            ExceptionHandler.pass("Checkpoint module not set.");
            ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
        } else log(this.module, state);
    }
}
