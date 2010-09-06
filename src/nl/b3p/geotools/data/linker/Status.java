/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Erik van de Pol
 */
public class Status {

    public static final String FEATURES_START = "read.features.start";
    public static final String FEATURES_END = "read.features.end";

    private String errorReport = "";
    private int errorCount = 0;
    private int totalFeatureCount = 0;
    private int processedFeatures = 0;
    private int featureStart = 0;
    private int featureEnd = -1;
    private int totalFeatureSize = 0;
    private boolean interrupted = false;

    private Map<String, List<Integer>> nonFatalErrorMap = null;
    
    private static final Log log = LogFactory.getLog(Status.class);

    private static final String DEFAULT_NEW_LINE = "\n";
    private static final int DEFAULT_MAX_FEATURE_NUMBER_PER_EXCEPTION = 3;

    protected Properties batch;
    protected nl.b3p.datastorelinker.entity.Process process;


    public Status(Properties batch) {
        if (batch.containsKey(FEATURES_START))
            featureStart = ActionFactory.toInteger(batch.getProperty(FEATURES_START));
        if (batch.containsKey(FEATURES_END))
            featureEnd = ActionFactory.toInteger(batch.getProperty(FEATURES_END));
        init();
    }

    public Status(nl.b3p.datastorelinker.entity.Process process) {
        if (process.getFeaturesStart() != null)
            featureStart = process.getFeaturesStart();
        if (process.getFeaturesEnd() != null)
            featureEnd = process.getFeaturesEnd();
        init();
    }

    private void init() {
        nonFatalErrorMap = new HashMap<String, List<Integer>>();
    }

    public synchronized void addNonFatalError(String errorString, Integer featureNumber) {
        incrementErrorCount();
        log.warn("[" + featureNumber + "] " + errorString);
        if (!nonFatalErrorMap.containsKey(errorString)) {
            nonFatalErrorMap.put(errorString, new ArrayList<Integer>());
        }
        nonFatalErrorMap.get(errorString).add(featureNumber);
    }

    public synchronized Map<String, List<Integer>> getNonFatalErrors() {
        return nonFatalErrorMap;
    }

    public synchronized String getNonFatalErrorReport(String newLineString, int maxFeatureNumbersPerException) {
        if (newLineString == null)
            newLineString = DEFAULT_NEW_LINE;
        if (maxFeatureNumbersPerException < 1) {
            maxFeatureNumbersPerException = DEFAULT_MAX_FEATURE_NUMBER_PER_EXCEPTION;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getFinishedMessage());
        sb.append(newLineString);
        /*if (nonFatalErrorMap.entrySet().isEmpty()) {
            sb.append("");
        } else {
            sb.append("");*/
            for (Map.Entry<String, List<Integer>> entry : nonFatalErrorMap.entrySet()) {
                sb.append(entry.getKey());
                sb.append(newLineString);
                sb.append("Deze fout geldt voor de features: ");
                int i = 0;
                for (Integer featureNumber : entry.getValue()) {
                    if (maxFeatureNumbersPerException > 0 && i >= maxFeatureNumbersPerException) {
                        sb.append(" en ");
                        sb.append(entry.getValue().size() - i);
                        sb.append(" andere features.");
                        break;
                    } else {
                        sb.append(featureNumber);
                        sb.append(", ");
                    }
                    i++;
                }
                if (entry.getValue().size() > 0 && i <= maxFeatureNumbersPerException - 1) {
                    sb.delete(sb.length() - 2, sb.length()); // remove the trailing ", " if present
                }
                sb.append(newLineString);
            }
        //}
        return sb.toString();
    }

    public synchronized String getTruncatedErrorReport() {
        return (errorReport.length() > 500 ? errorReport.substring(0, 500) + "... (see log)" : errorReport);
    }

    public synchronized String getErrorReport() {
        return errorReport;
    }

    public synchronized void setErrorReport(String errorReport) {
        this.errorReport = errorReport;
    }

    public synchronized int getErrorCount() {
        return errorCount;
    }

    public synchronized void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public synchronized void incrementErrorCount() {
        errorCount++;
    }

    public synchronized String getFinishedMessage() {
        return getFinishedMessage(DEFAULT_NEW_LINE);
    }

    public synchronized String getFinishedMessage(String newLineString) {
        if (getProcessedFeatures() == 0 && getErrorCount() == 0) {
            return "No features processed, was this intended?";
        } else if (getProcessedFeatures() == getTotalFeatureCount() && getErrorCount() == 0) {
            return "All " + getProcessedFeatures() + " features processed";
        } else if (getProcessedFeatures() == getTotalFeatureCount()) {
            return getProcessedFeatures() + " features processed, but " + getErrorCount() + " errors.";
        } else {
            return getProcessedFeatures() + " of " + getTotalFeatureCount() + " features processed." + newLineString + "Using parameters:" + newLineString + "Start:  " + getFeatureStart() + newLineString + "End:    " + getFeatureEnd() + newLineString + "Errors: " + getErrorCount();
        }
    }

    /**
     *
     * @return Total number of features of all feature sources in this process
     * that have been considered a candidate to be processed at this moment
     * in the DataStoreLinkers execution.
     */
    public synchronized int getTotalFeatureCount() {
        return totalFeatureCount;
    }

    public synchronized void setTotalFeatureCount(int totalFeatureCount) {
        this.totalFeatureCount = totalFeatureCount;
    }

    public synchronized void incrementTotalFeatureCount() {
        totalFeatureCount++;
    }

    /**
     *
     * @return Total number of features of all feature sources in this process
     * that have been succesfully processed at this moment
     * in the DataStoreLinkers execution.
     * @see getTotalFeatureCount()
     */
    public synchronized int getProcessedFeatures() {
        return processedFeatures;
    }

    public synchronized void setProcessedFeatures(int processedFeatures) {
        this.processedFeatures = processedFeatures;
    }

    public synchronized void incrementProcessedFeatures() {
        processedFeatures++;
    }

    public synchronized int getFeatureStart() {
        return featureStart;
    }

    public synchronized void setFeatureStart(int featureStart) {
        this.featureStart = featureStart;
    }

    public synchronized int getFeatureEnd() {
        return featureEnd;
    }

    public synchronized void setFeatureEnd(int featureEnd) {
        this.featureEnd = featureEnd;
    }

    /**
     *
     * @return Total number of features in all feature sources in this process.
     */
    public synchronized int getTotalFeatureSize() {
        return totalFeatureSize;
    }

    public synchronized void setTotalFeatureSize(int totalFeatureSize) {
        this.totalFeatureSize = totalFeatureSize;
    }

    public synchronized void incrementTotalFeatureSize() {
        totalFeatureSize++;
    }

    public synchronized boolean isInterrupted() {
        return interrupted;
    }

    public synchronized void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
