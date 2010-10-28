/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import javax.activation.UnsupportedDataTypeException;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import nl.b3p.datastorelinker.util.MarshalUtils;
import nl.b3p.datastorelinker.util.Util;
import org.apache.log4j.PropertyConfigurator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.util.logging.Logging;
import org.xml.sax.SAXException;

/**
 * Start process for one or more .properties of .xml files
 * @author Gertjan Al, B3Partners
 */
public class Client {

    private static final String version = "1.1";
    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
    private static final Log log = LogFactory.getLog(DataStoreLinker.class);
    static final Logging logging = Logging.ALL;
    private static HashMap<String, Boolean> projectProperties = new HashMap();
    private static final String TO_XML = "toXml";
    private static final String PRINT_STATS = "printStats";
    private static final String NO_MAIL = "noMail";

    static {
        projectProperties.put(PRINT_STATS, false);
        projectProperties.put(TO_XML, false);
        projectProperties.put(NO_MAIL, false);
    }

    /**
     * @param args the command line arguments
     * @throws Exception for log4j
     */
    public static void main(String[] args) throws Exception {
        // Logger
        Class c = Client.class;
        URL log4j_url = c.getResource("log4j.properties");
        if (log4j_url == null) {
            throw new IOException("Unable to locate log4j.properties in package " + Client.class.getPackage().toString());
        }
        //logger for geotools
        try {
            logging.setLoggerFactory("org.geotools.util.logging.Log4JLoggerFactory");
        } catch (ClassNotFoundException log4jException) {
            log.error("error: ",log4jException);
        }

        Properties p = new Properties();
        p.load(log4j_url.openStream());
        PropertyConfigurator.configure(p);
        log.info("logging configured!");

        parseProjectProperties(args);
        Properties batch = new Properties();
        nl.b3p.datastorelinker.entity.Process process = null;
        String tempFile = ""; // tempVar for error report

        if (getProjectProperty(PRINT_STATS)) {
            System.out.println();
        }


        try {
            if (args.length == 0) {
                System.out.println("No arguments specified; enter .properties and / or .xml file(s)");
            }

            for (int i = 0; i < args.length; i++) {
                tempFile = args[i];
                String reportMessage = "";


                if (args[i].toLowerCase().endsWith(".xml") || args[i].toLowerCase().endsWith(".properties")) {
                    File file = new File(args[i]);

                    if (file.exists() && file.isFile()) {
                        String info = "Reading \"" + file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("\\") + 1) + "\"\n";
                        Date startTime = Calendar.getInstance().getTime();

                        info += "Started at " + sdf.format(startTime) + "\n";
                        reportMessage += info;

                        log.info(info);
                        if (getProjectProperty(PRINT_STATS)) {
                            System.out.println(info);
                        }

                        if (getProjectProperty(TO_XML)) {
                            saveAsXML(file);
                        }

                        // Open filestream
                        InputStream input = new FileInputStream(args[i]);

                        if (input != null) {
                            batch = new Properties();

                            process = null;

                            if (args[i].toLowerCase().endsWith(".xml")) {
                                //batch.loadFromXML(input);
                                //if (batch.size() == 1 && batch.containsKey("process")) {
                                    // we are dealing with new version xml here
                                    process = (nl.b3p.datastorelinker.entity.Process)
                                        MarshalUtils.unmarshalProcess(input, MarshalUtils.getDslSchema());
                                //}
                            } else if (args[i].toLowerCase().endsWith(".properties")) {
                                batch.load(input);
                            } else {
                                throw new UnsupportedDataTypeException("File \"" + args[i] + "\" not supported; Use .properties or .xml");
                            }

                            DataStoreLinker dsl = null;
                            if (process != null)
                                dsl = new DataStoreLinker(process);
                            else
                                dsl = new DataStoreLinker(batch);
                            
                            dsl.process();
                            info = dsl.getStatus().getNonFatalErrorReport("\n", 3);//.getFinishedMessage();

                            Date endTime = Calendar.getInstance().getTime();
                            info += giveTimeInfo(startTime, endTime);

                            log.info(info);
                            reportMessage += "\n" + info + "\n";

                            if (getProjectProperty(PRINT_STATS)) {
                                System.out.println(" " + info + "\n");
                            }

                            if (!getProjectProperty(NO_MAIL)) {
                                try {
                                    if (process != null) {
                                        DataStoreLinkerMail.mail(batch, reportMessage,
                                            "Batchreport for '" + file.getAbsoluteFile().getName() + "' - finished at " + sdf.format(endTime)
                                        );
                                    } else {
                                        DataStoreLinkerMail.mail(batch,
                                                "Batchreport for '" + file.getAbsoluteFile().getName() + "' - finished at " + sdf.format(endTime),
                                                reportMessage);
                                    }
                                } catch (Exception ex) {
                                }
                            }

                        } else {
                            throw new Exception("Filestream \"" + args[i] + "\" could not be opened");
                        }

                    } else {
                        throw new Exception("File \"" + file.toURL().toString() + " could not be found");
                    }
                }
            }


        } catch (Exception ex) {
            log.error("",ex);
            // Log error
            String exception;
            if (ex.getMessage() == null) {
                exception = " > Geen info. Foutmelding ontstaan buiten DataStoreLinker.";
            } else {
                exception = ex.getMessage() + "\n" + (ex.getCause() != null ? "\n" + ex.getCause().toString() : "") + "\n";
            }

            log.error(exception);

            if (!getProjectProperty(NO_MAIL)) {
                String message = "Er is een fout opgetreden bij de verwerking van batchBestand " + tempFile + "\n\n" +
                        "Voor meer informatie over de ontstane fout verwijzen wij u naar het logbestand:\n" +
                        p.getProperty("logFilePath") + p.getProperty("logFile") + "\n\n\n" +
                        "Foutmelding:\n" + exception;

                if (process != null)
                    DataStoreLinkerMail.mail(process, message);
                else
                    DataStoreLinkerMail.mail(batch, message);
            }
            throw ex;
        }
    }

    private static String giveTimeInfo(Date startTime, Date endTime) {
        Date total = new Date(endTime.getTime() - startTime.getTime());

        double seconds = (Integer.parseInt(Long.toString((endTime.getTime() - startTime.getTime()))) / 10) / 100.0;
        String milliseconds = "0";
        if (Double.toString(seconds).contains(".")) {
            String value = Double.toString(seconds);
            milliseconds = value.substring(value.indexOf("."));
        }

        int minutes = total.getMinutes();
        int hours = total.getHours() - 1; // total returns hours = 1, when zero?
        //int hours = endTime.getHours() - startTime.getHours();// problem when starttime is < 24:00 & endtime > 0:00

        return "\n\nBatch succesful (total time:" + (hours == 0 ? "" : " " + hours + " hour" + (hours == 1 ? "" : "s") + " ,") + (minutes == 0 ? "" : " " + minutes + " minute" + (minutes == 1 ? "" : "s") + " ,") + " " + total.getSeconds() + milliseconds + " seconds)";
    }

    public static void saveAsXML(File propertiesFile) throws FileNotFoundException, IOException {
        InputStream input = new FileInputStream(propertiesFile);

        // Don't use replace(), .properties might be uppercase
        String fileToString = propertiesFile.getAbsolutePath();
        if (fileToString.toLowerCase().endsWith(".properties")) {

            String outString = fileToString.substring(0, fileToString.length() - ".properties".length()) + ".xml";
            File outputFile = new File(outString);
            OutputStream output = new FileOutputStream(outputFile);

            Properties properties = new Properties();
            properties.load(input);

            properties.storeToXML(output, "XML Generated with DataStoreLinker v" + version);

        } else {
            throw new IOException("File \"" + propertiesFile.toURL().toString() + "\" is not a propertiesFile");
        }
    }

    private static void parseProjectProperties(String[] args) {
        // TODO make property-value combination possible
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && args[i].length() > 1) {
                String arg = args[i].substring(1);
                projectProperties.put(arg, true);
            }
        }
    }

    private static boolean getProjectProperty(String property) {
        if (projectProperties.containsKey(property)) {
            return projectProperties.get(property);
        } else {
            return false;
        }
    }

}
