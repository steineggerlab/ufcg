package module;

import envs.config.GenericConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
import tree.tools.LabelReplacer;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PruneModule {
    private static List<String> parseArgument(String[] args) throws ParseException {
        /* option argument setup */
        Options opts = new Options();

        opts.addOption("h", "help",		false,	"helper route");
        opts.addOption("i", "input",	true,	"input file");
        opts.addOption("g", "gene",		true,	"gene to replace");
        opts.addOption("l", "leaf",		true,	"leaf format");

        opts.addOption(null, "notime", false, "no timestamp with prompt");
        opts.addOption(null, "nocolor", false, "disable ANSI escapes");
        opts.addOption("v", "verbose", false, "verbosity");
        opts.addOption(null, "developer", false, "developer tool");

        /* parse argument with CommandLineParser */
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

        /* apply general options */
        assert cmd != null;
        if(cmd.hasOption("v"))		 GenericConfig.VERB = true;
        if(cmd.hasOption("notime"))  GenericConfig.TSTAMP = false;
        if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
        if(cmd.hasOption("developer")) {
            GenericConfig.DEV = true;
            GenericConfig.VERB = true;
            GenericConfig.TSTAMP = true;
        }

        /* prepare arguments compatible to tree module */
        List<String> argList = new ArrayList<>();
        argList.add("replace");

        if(cmd.hasOption("h")) printPruneHelp();
        else {
            if(cmd.hasOption("i")) {
                argList.add(cmd.getOptionValue("i"));
            } else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
            if(cmd.hasOption("g")) {
                argList.add(cmd.getOptionValue("g"));
            } else ExceptionHandler.handle(ExceptionHandler.NO_GENE_NAME);
            if(cmd.hasOption("l")) {
                String buf = cmd.getOptionValue("l");
                for(String ele : buf.split(",")) argList.add("-" + ele);
            }
        }

        return argList;
    }

    private static void validate(List<String> argList) {
        final String[] validatedLeaf = {"-uid", "-acc", "-label", "-taxon", "-strain", "-type", "-taxonomy"};
        List<String> validatedLeafOptionList = Arrays.asList(validatedLeaf);

        for(String arg : argList) {
            if(arg.startsWith("-")&& !validatedLeafOptionList.contains(arg)) {
                ExceptionHandler.pass(arg);
                ExceptionHandler.handle(ExceptionHandler.INVALID_LEAF_FORMAT);
            }
        }
    }

    public static void run(String[] args) {
        List<String> argList = null;
        try {
            argList = parseArgument(args);
        } catch(ParseException e) {
            ExceptionHandler.handle(e);
        }

        try {
            assert Objects.requireNonNull(argList).size() > 2;
            validate(argList);

            String trmFileName = argList.get(1);
            File trmFile = new File(trmFileName);
            if (!trmFile.exists()) {
                ExceptionHandler.pass(trmFile);
                ExceptionHandler.handle(ExceptionHandler.INVALID_FILE);
            }
            if (!new File(System.getProperty("user.dir")).canWrite()) {
                ExceptionHandler.pass("Cannot write a file in the current working directory.");
                ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
            }

            String gene = argList.get(2);
            String content = new String (Files.readAllBytes(Paths.get(trmFileName)));
            JSONObject jsonObject = new JSONObject(content);
            if(!jsonObject.keySet().contains(gene)) {
                ExceptionHandler.pass("No " + gene + " tree in the .trm file.");
                ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
            }
            if(jsonObject.get(gene)==null||jsonObject.get(gene).equals("")) {
                ExceptionHandler.pass("No " + gene + " tree in the .trm file.");
                ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
            }

            String nwk = (String) jsonObject.get(gene);
            if (nwk == null) {
                ExceptionHandler.pass("The " + gene + " tree doesn't exist.");
                ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
            }

            HashMap<String, String> replaceMap = new HashMap<>();
            HashMap<String, Integer> checkLabelName = new HashMap<>();
            JSONArray labelLists = (JSONArray) jsonObject.get("list");
            for (int i = 0; i < labelLists.length(); i++) {
                JSONArray labelList = (JSONArray) labelLists.get(i);

                String uid = (String) labelList.get(0);
                String label = (String) labelList.get(1);
                String acc = (String) labelList.get(2);
                String taxon_name = (String) labelList.get(3);
                String strain_name = (String) labelList.get(4);
                String type = (String) labelList.get(5);
                String taxonomy = (String) labelList.get(6);

                String replacedLabel = "";
                if (argList.contains("-uid")) { replacedLabel = replacedLabel + "|" + uid;}
                if (argList.contains("-acc")) { replacedLabel = replacedLabel + "|" + acc;}
                if (argList.contains("-label")) { replacedLabel = replacedLabel + "|" + label;}
                if (argList.contains("-taxon")) { replacedLabel = replacedLabel + "|" + taxon_name;}
                if (argList.contains("-taxonomy")) { replacedLabel = replacedLabel + "|" + taxonomy;}
                if (argList.contains("-strain")) { replacedLabel = replacedLabel + "|" + strain_name;}
                if (argList.contains("-type")) {
                    if (type.equals("true")) {
                        replacedLabel = replacedLabel + "|type" ;
                    }
                }
                if (replacedLabel.startsWith("|")) {
                    replacedLabel = replacedLabel.substring(1);
                }

                if (checkLabelName.containsKey(replacedLabel)) {
                    checkLabelName.put(replacedLabel, checkLabelName.get(replacedLabel) + 1);
                } else {
                    checkLabelName.put(replacedLabel, 1);
                }
                if (checkLabelName.get(replacedLabel) != 1) {
                    replacedLabel = replacedLabel + "_" + checkLabelName.get(replacedLabel);
                }

                replaceMap.put(uid, replacedLabel);

            }

            String treeFileName = "replaced." + gene + ".nwk";
            FileWriter treeFW = new FileWriter(treeFileName);
            treeFW.append(nwk);
            treeFW.flush();
            treeFW.close();

            LabelReplacer.replace_name(treeFileName, treeFileName, replaceMap);
            Prompt.print("The tree file '" + treeFileName + "' with replaced labels was written.");
        } catch(Exception e) {
            ExceptionHandler.handle(e);
        }

    }

    private static void printPruneHelp() {
        System.out.println(ANSIHandler.wrapper(" UFCG - prune", 'G'));
        System.out.println(ANSIHandler.wrapper(" Fix UFCG tree labels or get a single gene tree", 'g'));
        System.out.println();

        System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ufcg prune -i <INPUT> -g <GENE> -l <LABEL>");
        System.out.println();

        System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
        System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
        System.out.println(ANSIHandler.wrapper(" -i STR         Input .trm file provided by tree module ", 'x'));
        System.out.println(ANSIHandler.wrapper(" -g STR         Gene name - \"UFCG\" for a UFCG tree, proper gene name for a single gene tree ", 'x'));
        System.out.println(ANSIHandler.wrapper(" -l STR         Tree label format, comma-separated string containing one or more of the following keywords: ", 'x'));
        System.out.println(ANSIHandler.wrapper("                [uid, acc, label, taxon, strain, type, taxonomy] ", 'x'));
        System.out.println();

        UFCGMainPipeline.printGeneral();

        System.exit(0);
    }
}
