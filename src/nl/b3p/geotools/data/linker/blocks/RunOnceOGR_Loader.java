package nl.b3p.geotools.data.linker.blocks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.data.jdbc.JDBCDataStore;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public class RunOnceOGR_Loader extends RunOnce {

    public static final String TEMP_TABLE = "_temp_ogr";
    private static String FWTOOLS_DIR;
    private String file_in;
    private Map db_out;
    private String srs;
    private boolean skipFailures;

    public RunOnceOGR_Loader(String fwtools_dir, String file_in, Map db_tmp, String srs, boolean skipFailures) {
        FWTOOLS_DIR = fwtools_dir;
        this.file_in = file_in;
        this.db_out = db_tmp;
        this.srs = srs;
        this.skipFailures = skipFailures;
    }

    protected void exec() throws Exception {

        ArrayList<String> commandList = new ArrayList();
        commandList.add(FWTOOLS_DIR + "bin/ogr2ogr");
        commandList.add("-f");
        commandList.add("PostgreSQL");
        commandList.add("-a_srs");
        commandList.add(srs);
        commandList.add(mapToDBString(db_out));
        commandList.add(file_in);
        commandList.add("-nln");
        commandList.add(TEMP_TABLE);
        commandList.add("-overwrite");

        if (skipFailures) {
            commandList.add("-skipfailures");
        }

        String[] commands = commandList.toArray(new String[commandList.size()]);

        ProcessBuilder pb = new ProcessBuilder(commands);
        DataStoreLinker.setEnvironment(pb.environment(), "ogr.env.");
        /*
        pb.environment().put("PYTHONPATH", FWTOOLS_DIR + "pymod");

        // TODO directory check instead of os check
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
        // linux
        pb.environment().put("LD_LIBRARY_PATH", FWTOOLS_DIR + "lib");
        pb.environment().put("GDAL_DATA", FWTOOLS_DIR + "share/epsg_csv");
        pb.environment().put("PROJ_LIB", FWTOOLS_DIR + "lib");

        } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
        // windows
        pb.environment().put("PROJ_LIB", FWTOOLS_DIR + "proj_lib");
        pb.environment().put("GEOTIFF_CSV", FWTOOLS_DIR + "data");
        pb.environment().put("GDAL_DATA", FWTOOLS_DIR + "data");
        pb.environment().put("GDAL_DRIVER_PATH", FWTOOLS_DIR + "gdal_plugins");

        } else {
        log.info("Unknown operating system " + System.getProperty("os.name") + "; using linux settings");
        // linux
        pb.environment().put("LD_LIBRARY_PATH", FWTOOLS_DIR + "lib");
        pb.environment().put("GDAL_DATA", FWTOOLS_DIR + "share/epsg_csv");
        pb.environment().put("PROJ_LIB", FWTOOLS_DIR + "lib");
        }
         */
        Process child = pb.start();

        BufferedReader errorReader = new BufferedReader(new InputStreamReader(child.getErrorStream()));
        String errorLine;
        String errorText = "";
        while ((errorLine = errorReader.readLine()) != null) {
            errorText += errorLine + "\n";
        }

        if (!errorText.equals("")) {
            log.error(errorText);
        }

        int result = child.waitFor();
        if (result != 0) {
            if (errorText.equals("")) {
                throw new IOException("Loading file '" + file_in + "'failed. No error available");
            } else {
                throw new IOException("Loading file '" + file_in + "'failed. " + errorText);
            }
        }
    }

    private String mapToDBString(Map params) {
        String connect = "PG: ";

        Iterator iter = params.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String value = (String) params.get(key);
            connect += key + "=" + value + " ";
        }

        return connect.substring(0, connect.length() - 1);
    }

    public static void close(DataStore dataStore2Read) throws Exception {
        // Drop temptable
        JDBCDataStore database = (JDBCDataStore) dataStore2Read;
        Connection con = database.getConnection(Transaction.AUTO_COMMIT);
        con.setAutoCommit(true);

        // TODO make this function work with all databases
        PreparedStatement ps = con.prepareStatement("DROP TABLE \"" + TEMP_TABLE + "\"; DELETE FROM \"geometry_columns\" WHERE f_table_name = '" + TEMP_TABLE + "'");
        ps.execute();

        con.close();
    }

    public String toString() {
        return "Preload '" + file_in + "' to the PostGIS database " + db_out.toString() + " for further use";
    }

    public String getDescription_NL() {
        return "";
    }

    public static String[][] getConstructors() {
        return null;
    }
}
