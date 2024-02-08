package cz.gov.data.ms;

import cz.gov.data.ms.action.DownloadSharepointDirectory;
import cz.gov.data.ms.action.DownloadSharepointList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;

public class EntryPoint {

    private static final Logger LOG = LoggerFactory.getLogger(EntryPoint.class);

    protected AzureAuthentication authentication;

    protected String siteIdentifier;

    public static void main(String[] args) {
        (new EntryPoint()).execute(args);
    }

    public void execute(String[] args) {
        if (args.length < 1) {
            System.out.println("You must specify a command: download-list, download-directory.");
        }
        String command = args[0];
        if ("download-list".equals(command)) {
            runDownloadList(Arrays.copyOfRange(args, 1, args.length));
        } else if ("download-directory".equals(command)) {
            runDownloadDirectory(Arrays.copyOfRange(args, 1, args.length));
        } else {
            System.out.println("Unknown command.");
        }
    }

    public void runDownloadList(String[] args) {
        Options options = new Options();
        addCommonOptions(options);
        options.addRequiredOption(null, "list", true, "List identifier.");
        options.addRequiredOption(null, "output", true, "Output file.");
        options.addRequiredOption(null, "base", true, "Base URL.");
        //
        CommandLine commandLine = parseCommandLine(options, args);
        loadCommonOptions(commandLine);
        //
        String list = commandLine.getOptionValue("list");
        Path output = Path.of(commandLine.getOptionValue("output"));
        String baseUrl = commandLine.getOptionValue("base");
        //
        try {
            DownloadSharepointList.downloadContent(
                    authentication, siteIdentifier, list, baseUrl, output);
        } catch (Throwable t) {
            LOG.error("Failed to download SharePoint list.", t);
        }
    }

    protected void addCommonOptions(Options options) {
        options.addOption(
                null, "application", true, "Application identification.");
        options.addOption(
                null, "tenant", true, "Tenant identification.");
        options.addOption(
                null, "secret", true, "Application secret.");
        //
        options.addRequiredOption(
                null, "site", true, "Site identifier.");
    }

    protected CommandLine parseCommandLine(Options options, String[] args) {
        try {
            return (new DefaultParser()).parse(options, args);
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            (new HelpFormatter()).printHelp("Usage:", options);
            System.exit(0);
        }
        return null;
    }

    protected void loadCommonOptions(CommandLine commandLine) {
        String application = getOption(
                commandLine, "application", "MS_APPLICATION");
        String tenant = getOption(
                commandLine, "tenant", "MS_TENANT");
        authentication = new AzureAuthentication(application, tenant);

        String secret = getOption(
                commandLine, "secret", "MS_SECRET");
        authentication.authenticateAsApplication(secret);

        siteIdentifier = commandLine.getOptionValue("site");
    }

    protected String getOption(
            CommandLine commandLine, String argument, String environment) {
        String result = commandLine.getOptionValue(argument);
        if (result == null) {
            result = System.getenv(environment);
        }
        return result;
    }

    public void runDownloadDirectory(String[] args) {
        Options options = new Options();
        addCommonOptions(options);
        options.addRequiredOption(null, "drive", true, "Drive name.");
        options.addRequiredOption(null, "directory", true, "Directory name.");
        options.addRequiredOption(null, "output", true, "Output directory.");
        //
        CommandLine commandLine = parseCommandLine(options, args);
        loadCommonOptions(commandLine);
        //
        String drive = commandLine.getOptionValue("drive");
        String directory = commandLine.getOptionValue("directory");
        Path output = Path.of(commandLine.getOptionValue("output"));
        //
        try {
            DownloadSharepointDirectory.downloadContent(
                    authentication, siteIdentifier, drive, directory, output);
        } catch (Throwable t) {
            LOG.error("Failed to download SharePoint directory.", t);
        }
    }

}
