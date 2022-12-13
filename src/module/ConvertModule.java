package module;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.json.JSONArray;
import org.json.JSONObject;

import envs.config.GenericConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import org.json.JSONTokener;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;

public class ConvertModule {
    private static final String[] entries = {"uid", "acc (accession)", "label", "taxon (taxon_name)", "strain (strain_name)", "type (isTypeStrain)", "taxonomy"};
    private final String[] args;
    private String input, output;
    private boolean nucl, allowMultiple = false;

    private final boolean[] format = {false, true, false, false, false, false, false};
    public ConvertModule(String[] args) {
        this.args = args;
        if(parseArguments() < 0) printManual();
    }

    private int parseArguments() {
        Options opts = new Options();
        opts.addOption("h", "help", false, "helper route");
        opts.addOption(null, "notime", false, "no timestamp with prompt");
        opts.addOption(null, "nocolor", false, "disable ANSI escapes");
        opts.addOption("v", "verbose", false, "verbosity");
        opts.addOption(null, "developer", false, "developer tool");
        opts.addOption(null, "test", false, "for test");

        opts.addOption("i", "input", true, "input file");
        opts.addOption("o", "output", true, "output file");
        opts.addOption("t", "type", true, "sequence type");
        opts.addOption("f", "force", false, "force overwrite");
        opts.addOption("c", "copy", false, "allow multiple copy");
        opts.addOption("l", "leaf", true, "header format");

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
        if(cmd.hasOption("i")){
            File f = new File(cmd.getOptionValue("i"));
            if(f.exists() && f.isFile()) input = f.getAbsolutePath();
            else {
                ExceptionHandler.pass(cmd.getOptionValue("i"));
                ExceptionHandler.handle(ExceptionHandler.INVALID_FILE);
            }
        } else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);

        if(cmd.hasOption("o")){
            File f = new File(cmd.getOptionValue("o"));
            if(f.exists()){
                if(f.isFile() && f.canWrite()){
                    if(cmd.hasOption("f")){
                        Prompt.warn("Overwriting file: " + f.getAbsolutePath());
                        output = f.getAbsolutePath();
                    }
                    else{
                        ExceptionHandler.pass(cmd.getOptionValue("o"));
                        ExceptionHandler.handle(ExceptionHandler.FILE_EXISTS);
                    }
                }
                else {
                    ExceptionHandler.pass(cmd.getOptionValue("o"));
                    ExceptionHandler.handle(ExceptionHandler.INVALID_FILE);
                }
            }
            else output = f.getAbsolutePath();
        } else ExceptionHandler.handle(ExceptionHandler.NO_OUTPUT);

        if(cmd.hasOption("t")){
            String type = cmd.getOptionValue("t");
            if(type.startsWith("nuc")) nucl = true;
            else if(type.startsWith("pro")) nucl = false;
            else {
                ExceptionHandler.pass(type);
                ExceptionHandler.handle(ExceptionHandler.INVALID_TYPE);
            }
        } else ExceptionHandler.handle(ExceptionHandler.NO_TYPE_OPTION);

        if(cmd.hasOption("l")){
            format[1] = false;
            for(String ele : cmd.getOptionValue("l").split(",")){
                switch(ele){
                    case "uid":      format[0] = true; break;
                    case "acc":      format[1] = true; break;
                    case "label":    format[2] = true; break;
                    case "taxon":    format[3] = true; break;
                    case "strain":   format[4] = true; break;
                    case "type":     format[5] = true; break;
                    case "taxonomy": format[6] = true; break;
                    default:
                        ExceptionHandler.pass(ele);
                        ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
                }
            }
        }

        allowMultiple = cmd.hasOption("c");
        return 0;
    }

    private static void printManual() {
        System.out.println(ANSIHandler.wrapper(" UFCG - convert", 'G'));
        System.out.println(ANSIHandler.wrapper(" Convert core gene profile into a FASTA file", 'g'));
        System.out.println();

        System.out.println(ANSIHandler.wrapper("\n USAGE :", 'Y') + " ufcg convert -i <PROFILE> -o <FASTA> [...]");
        System.out.println();

        System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
        System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
        System.out.println(ANSIHandler.wrapper(" -i STR         Input core gene profile (.ucg)", 'x'));
        System.out.println(ANSIHandler.wrapper(" -o STR         Output FASTA file", 'x'));
		System.out.println(ANSIHandler.wrapper(" -t STR         Sequence type [nuc, pro]", 'x'));
        System.out.println();

        System.out.println(ANSIHandler.wrapper("\n Configurations", 'y'));
        System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
        System.out.println(ANSIHandler.wrapper(" -l STR         FASTA header format, comma-separated string containing one or more of the following keywords: [acc]", 'x'));
        System.out.println(ANSIHandler.wrapper("                [uid, acc, label, taxon, strain, type, taxonomy] ", 'x'));
        System.out.println(ANSIHandler.wrapper(" -f             Force to overwrite the existing files [0]", 'x'));
        System.out.println(ANSIHandler.wrapper(" -c             Include multiple copied genes (tag with numerical suffix) [0]", 'x'));
        System.out.println();

        UFCGMainPipeline.printGeneral();

        System.exit(0);
    }

    private void run() throws IOException {
        JSONObject json = new JSONObject(new JSONTokener(Files.newInputStream(Paths.get(input))));
        String[] info = {
                json.getJSONObject("genome_info").get("uid").toString(),
                json.getJSONObject("genome_info").get("accession").toString(),
                json.getJSONObject("genome_info").get("label").toString(),
                json.getJSONObject("genome_info").get("taxon_name").toString(),
                json.getJSONObject("genome_info").get("strain_name").toString(),
                json.getJSONObject("genome_info").get("isTypeStrain").toString(),
                json.getJSONObject("genome_info").get("taxonomy").toString()
        };

        // check corresponding format
        for(int i = 0; i < format.length; i++){
            if(format[i] && info[i].equals("null")){
                ExceptionHandler.pass("Requested entry not found in the input profile : " + ANSIHandler.wrapper(entries[i], 'y'));
                ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
            }
        }

        // write to file
        FileStream f = new FileStream(output, 'o');
        JSONObject data = json.getJSONObject("data");
        for(String gene : ((String) json.getJSONObject("run_info").get("target_gene_set")).split(",")){
            if(data.has(gene)){
                JSONArray arr = data.getJSONArray(gene);
                if(arr.length() < 1 || (!allowMultiple && arr.length() > 1)) continue;
                for(int i = 0; i < arr.length(); i++){
                    StringBuilder header = new StringBuilder(">");
                    for(int j = 0; j < format.length; j++){
                        if(format[j]) header.append(info[j]).append("_");
                    }
                    header.append(gene);
                    if(arr.length() > 1) header.append("_").append(i + 1);
                    f.println(header.toString());
                    f.println(arr.getJSONObject(i).getString(nucl ? "dna" : "protein"));
                }
            }
        }
        f.close();
    }

    public static void run(String[] args){
        ConvertModule cm = new ConvertModule(args);
        try{
            cm.run();
        } catch (IOException e) {
            ExceptionHandler.handle(e);
        }
    }
}
