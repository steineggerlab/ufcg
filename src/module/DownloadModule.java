package module;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import envs.config.PathConfig;
import envs.toolkit.Shell;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import envs.config.GenericConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;

public class DownloadModule {
    private static final String[] targets = {"full", "minimum", "config", "core", "busco", "sample"};
    private final String[] args;
    private String target, dir = PathConfig.JarPath;
    private boolean dirGiven = false;

    public DownloadModule(String[] args) {
        this.args = args;
        switch(parseArguments()){
            case 0: break;
            case -1: printManual(); break;
            case -2: checkStatus(); break;
            default: ExceptionHandler.handle(ExceptionHandler.UNEXPECTED_ERROR);
        }
    }

    private int parseArguments() {
        Options opts = new Options();
        opts.addOption("h", "help", false, "helper route");
        opts.addOption(null, "notime", false, "no timestamp with prompt");
        opts.addOption(null, "nocolor", false, "disable ANSI escapes");
        opts.addOption("v", "verbose", false, "verbosity");
        opts.addOption(null, "developer", false, "developer tool");
        opts.addOption(null, "test", false, "for test");

        opts.addOption("t", "target", true, "download target");
        opts.addOption("d", "dir", true, "download directory");
        opts.addOption("c", "check", false, "check files");

        CommandLineParser clp = new DefaultParser();
        CommandLine cmd = null;
        try{ cmd = clp.parse(opts, args); }
        catch(UnrecognizedOptionException uoe) {
            ExceptionHandler.pass(uoe.getOption());
            ExceptionHandler.handle(ExceptionHandler.UNKNOWN_OPTION);
        }
        catch(MissingArgumentException mae) {
            ExceptionHandler.pass(mae.getOption().getOpt() != null ?
                    mae.getOption().getOpt() :
                    mae.getOption().getLongOpt());
            ExceptionHandler.handle(ExceptionHandler.MISSING_ARGUMENT);
        }
        catch(ParseException pe) {
            ExceptionHandler.handle(pe);
        }

        assert cmd != null;
        if(cmd.hasOption("developer")) {
            GenericConfig.DEV = true;
            GenericConfig.VERB = true;
            GenericConfig.TSTAMP = true;
        }
        if(cmd.hasOption("test")) GenericConfig.TEST = true;

        /* parse user friendly options; return ID and finish routine if necessary */
        if(cmd.hasOption("v"))       GenericConfig.VERB = true;
        if(cmd.hasOption("notime"))  GenericConfig.TSTAMP = false;
        if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
        if(cmd.hasOption("h"))       return -1;

        Prompt.debug(ANSIHandler.wrapper("Developer mode activated.", 'Y'));
        Prompt.talk("Verbose option check.");
        if(GenericConfig.TSTAMP) Prompt.talk("Timestamp printing option check.");

        /* parse arguments */
        if(cmd.hasOption("d")){
            dirGiven = true;
            dir = cmd.getOptionValue("d");
            if(!dir.endsWith("/")) dir += "/";
            if(new File(dir).exists()) {
                if(!new File(dir).isDirectory()) {
                    ExceptionHandler.pass(dir);
                    ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
                    return 1;
                }
            }
            else {
                if(!new File(dir).mkdirs()) {
                    ExceptionHandler.pass(dir);
                    ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
                    return 1;
                }
            }
        }
        if(cmd.hasOption("c")) return -2;
        if(cmd.hasOption("t")) target = cmd.getOptionValue("t");
        else {
            ExceptionHandler.handle(ExceptionHandler.NO_TARGET);
            return 1;
        }
        if(!Arrays.asList(targets).contains(target)){
            ExceptionHandler.pass(target);
            ExceptionHandler.handle(ExceptionHandler.INVALID_TARGET);
            return 1;
        }
        return 0;
    }

    private boolean internetConnection(){
        String[] raw = Shell.exec(String.format("ping -c 1 -W %d ufcg.steineggerlab.workers.dev", GenericConfig.OS.equals("Linux") ? 1 : 1000), true, 0);
        if(raw == null || raw.length < 2) return false;
        return raw[raw.length - 2].contains(GenericConfig.OS.equals("Linux") ? "1 received" : "1 packets received");
    }
    private void printManual() {
        System.out.println(ANSIHandler.wrapper(" UFCG - download", 'G'));
        System.out.println(ANSIHandler.wrapper(" List or download resources", 'g'));
        System.out.println();

        System.out.println(ANSIHandler.wrapper("\n USAGE :", 'Y') + " ufcg download -t <TARGET> [...]");
        System.out.println();

        System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
        System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
        System.out.println(ANSIHandler.wrapper(" -t ", 'x') + ANSIHandler.wrapper("full        ", 'x') + ANSIHandler.wrapper("Full download", 'x'));
        System.out.println(ANSIHandler.wrapper("    minimum     ", 'x') + ANSIHandler.wrapper("Minimum download (config, core)", 'x'));
        System.out.println(ANSIHandler.wrapper("    config      ", 'x') + ANSIHandler.wrapper("Download config files", 'x'));
        System.out.println(ANSIHandler.wrapper("    core        ", 'x') + ANSIHandler.wrapper("Download core gene database", 'x'));
        System.out.println(ANSIHandler.wrapper("    busco       ", 'x') + ANSIHandler.wrapper("Download BUSCO odb_fungi_v10 database", 'x'));
        System.out.println(ANSIHandler.wrapper("    sample      ", 'x') + ANSIHandler.wrapper("Download sample files", 'x'));
        System.out.println();

        System.out.println(ANSIHandler.wrapper("\n Configurations", 'y'));
        System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
        System.out.println(ANSIHandler.wrapper(" -d STR         Download directory [auto]", 'x'));
        System.out.println(ANSIHandler.wrapper(" -c             Check download status", 'x'));
        System.out.println();

        UFCGMainPipeline.printGeneral();

        System.exit(0);
    }
    private void checkStatus(){
        boolean ping = internetConnection();
        if(PathConfig.EnvironmentPathSet && !dirGiven) dir = PathConfig.EnvironmentPath;
        String sampleDir = dir;
        if(!dirGiven) sampleDir = PathConfig.CurrPath;

        System.out.println(ANSIHandler.wrapper(" System status", 'Y'));
        System.out.println(ANSIHandler.wrapper(" OS       : " + GenericConfig.OS, 'x'));
        System.out.println(ANSIHandler.wrapper(" Path     : ", 'x') + ANSIHandler.wrapper(dir, 'y'));
        System.out.println(ANSIHandler.wrapper(" Internet : ", 'x') + (ping ? ANSIHandler.wrapper("OK", 'G') : ANSIHandler.wrapper("NO", 'R')));
        if(!ping) System.exit(0);

        System.out.println(ANSIHandler.wrapper("\n Download status", 'Y'));
        System.out.println(ANSIHandler.wrapper(" Target     Status", 'c'));
        System.out.println(ANSIHandler.wrapper(" config     ", 'x') + (new File(dir + "config/ppx.cfg").exists() ?
                ANSIHandler.wrapper("OK", 'G') : ANSIHandler.wrapper("NO", 'R')));
        System.out.println(ANSIHandler.wrapper(" core       ", 'x') + (new File(dir + "config/seq/pro/ACT1.fa").exists() ?
                ANSIHandler.wrapper("OK", 'G') : ANSIHandler.wrapper("NO", 'R')));
        System.out.println(ANSIHandler.wrapper(" busco      ", 'x') + (new File(dir + "config/seq/busco/100957at4751.fa").exists() ?
                ANSIHandler.wrapper("OK", 'G') : ANSIHandler.wrapper("NO", 'R')));
        System.out.println(ANSIHandler.wrapper(" sample     ", 'x') + (new File(sampleDir + "sample/meta_full.tsv").exists() ?
                ANSIHandler.wrapper("OK", 'G') : ANSIHandler.wrapper("NO", 'R')));
        System.out.println();

        System.exit(0);
    }
    private void download() throws IOException {
        boolean ping = internetConnection();
        if(PathConfig.EnvironmentPathSet && !dirGiven) dir = PathConfig.EnvironmentPath;

        if(!ping) {
            ExceptionHandler.handle(ExceptionHandler.NO_INTERNET);
            return;
        }

        switch (target) {
            case "full": downloadConfig(); downloadCore(); downloadBusco(); downloadSample(); break;
            case "minimum": downloadConfig(); downloadCore(); break;
            case "config": downloadConfig(); break;
            case "core": downloadCore(); break;
            case "busco": downloadBusco(); break;
            case "sample": downloadSample(); break;
        }
    }

    private void downloadConfig(){
        // download payload
        Prompt.print("Downloading config package on " + ANSIHandler.wrapper(dir + "config.tar.gz", 'y') + " ...");
        Shell.exec(String.format("wget -q -O %sconfig.tar.gz https://ufcg.steineggerlab.workers.dev/payload/config.tar.gz", dir));

        // check download
        if(!new File(dir + "config.tar.gz").exists()){
            ExceptionHandler.pass(dir + "config.tar.gz");
            ExceptionHandler.handle(ExceptionHandler.DOWNLOAD_FAILED);
            return;
        }

        // extract payload
        Prompt.print("Extracting config package ...");
        Shell.exec(String.format("tar -C %s -xzf %sconfig.tar.gz", dir, dir));

        // check extraction
        if(!new File(dir + "config/ppx.cfg").exists()){
            ExceptionHandler.pass(dir + "config.tar.gz");
            ExceptionHandler.handle(ExceptionHandler.EXTRACTION_FAILED);
            return;
        }

        // delete payload
        Prompt.print("Deleting config package ...");
        if(!(new File(dir + "config.tar.gz")).delete()){
            Prompt.warn("Failed to delete config package on " + ANSIHandler.wrapper(dir + "config.tar.gz", 'y'));
        }

        Prompt.print("Download success : target " + ANSIHandler.wrapper("config", 'G'));
    }
    private void downloadCore(){
        // download payload
        Prompt.print("Downloading core package on " + ANSIHandler.wrapper(dir + "core.tar.gz", 'y') + " ...");
        Shell.exec(String.format("wget -q -O %score.tar.gz https://ufcg.steineggerlab.workers.dev/payload/core.tar.gz", dir));

        // check download
        if(!new File(dir + "core.tar.gz").exists()){
            ExceptionHandler.pass(dir + "core.tar.gz");
            ExceptionHandler.handle(ExceptionHandler.DOWNLOAD_FAILED);
            return;
        }

        // extract payload
        Prompt.print("Extracting core package ...");
        Shell.exec(String.format("tar -C %s -xzf %score.tar.gz", dir, dir));

        // check extraction
        if(!new File(dir + "config/seq/pro/ACT1.fa").exists()){
            ExceptionHandler.pass(dir + "core.tar.gz");
            ExceptionHandler.handle(ExceptionHandler.EXTRACTION_FAILED);
            return;
        }

        // delete payload
        Prompt.print("Deleting core package ...");
        if(!(new File(dir + "core.tar.gz")).delete()){
            Prompt.warn("Failed to delete core package on " + ANSIHandler.wrapper(dir + "core.tar.gz", 'y'));
        }

        Prompt.print("Download success : target " + ANSIHandler.wrapper("core", 'G'));
    }
    private void downloadBusco(){
        // download payload
        Prompt.print("Downloading busco package on " + ANSIHandler.wrapper(dir + "busco.tar.gz", 'y') + " ...");
        Shell.exec(String.format("wget -q -O %sbusco.tar.gz https://ufcg.steineggerlab.workers.dev/payload/busco.tar.gz", dir));

        // check download
        if(!new File(dir + "busco.tar.gz").exists()){
            ExceptionHandler.pass(dir + "busco.tar.gz");
            ExceptionHandler.handle(ExceptionHandler.DOWNLOAD_FAILED);
            return;
        }

        // extract payload
        Prompt.print("Extracting busco package ...");
        Shell.exec(String.format("tar -C %s -xzf %sbusco.tar.gz", dir, dir));

        // check extraction
        if(!new File(dir + "config/seq/busco/100957at4751.fa").exists()){
            ExceptionHandler.pass(dir + "busco.tar.gz");
            ExceptionHandler.handle(ExceptionHandler.EXTRACTION_FAILED);
            return;
        }

        // delete payload
        Prompt.print("Deleting busco package ...");
        if(!(new File(dir + "busco.tar.gz")).delete()){
            Prompt.warn("Failed to delete busco package on " + ANSIHandler.wrapper(dir + "busco.tar.gz", 'y'));
        }

        Prompt.print("Download success : target " + ANSIHandler.wrapper("busco", 'G'));
    }
    private void downloadSample(){
        if(!dirGiven) {
            dir = PathConfig.CurrPath;
        }

        // download payload
        Prompt.print("Downloading sample package on " + ANSIHandler.wrapper(dir + "sample.tar.gz", 'y') + " ...");
        Shell.exec(String.format("wget -q -O %ssample.tar.gz https://ufcg.steineggerlab.workers.dev/payload/sample.tar.gz", dir));

        // check download
        if(!new File(dir + "sample.tar.gz").exists()){
            ExceptionHandler.pass(dir + "sample.tar.gz");
            ExceptionHandler.handle(ExceptionHandler.DOWNLOAD_FAILED);
            return;
        }

        // extract payload
        Prompt.print("Extracting sample package ...");
        Shell.exec(String.format("tar -C %s -xzf %ssample.tar.gz", dir, dir));

        // check extraction
        if(!new File(dir + "sample/meta_full.tsv").exists()){
            ExceptionHandler.pass(dir + "sample.tar.gz");
            ExceptionHandler.handle(ExceptionHandler.EXTRACTION_FAILED);
            return;
        }

        // delete payload
        Prompt.print("Deleting sample package ...");
        if(!(new File(dir + "sample.tar.gz")).delete()){
            Prompt.warn("Failed to delete sample package on " + ANSIHandler.wrapper(dir + "sample.tar.gz", 'y'));
        }

        Prompt.print("Download success : target " + ANSIHandler.wrapper("sample", 'G'));
    }

    public static void run(String[] args){
        DownloadModule dm = new DownloadModule(args);
        try{
            dm.download();
        } catch (IOException e) {
            ExceptionHandler.handle(e);
        }
    }
}
