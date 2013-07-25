/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import nl.b3p.geotools.data.linker.util.LocalizationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Erik van de Pol
 */
public class Status {

    public static final String FEATURES_START = "read.features.start";
    public static final String FEATURES_END = "read.features.end";

    private final static ResourceBundle resources = LocalizationUtil.getResources();

    private String errorReport = "";
    private int nonFatalErrorCount = 0;
	private int writeErrorCount = 0;
    private int visitedFeatures = 0;
    private int processedFeatures = 0;
    private int featureStart = 0;
    private int featureEnd = -1;
    private int totalFeatureSize = 0;
    private boolean interrupted = false;

    private Map<String, List<String>> nonFatalErrorMap = null;
    private Map<String, List<String>> writeErrorMap = null;
    
    private static final Log log = LogFactory.getLog(Status.class);

    private static final String DEFAULT_NEW_LINE = "\n";
    private static final int DEFAULT_MAX_FEATURE_NUMBER_PER_EXCEPTION = 3;

    protected Properties batch;
    protected nl.b3p.datastorelinker.entity.Process process;

    private String processName = null;


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
        processName = process.getName();
    }

    private void init() {
        nonFatalErrorMap = new HashMap<String, List<String>>();
		writeErrorMap = new HashMap<String, List<String>>();
    }

    public synchronized void addNonFatalError(String errorString, String featureKey) {
        incrementNonFatalErrorCount();
        //log.warn("[" + featureNumber + "] " + errorString);
        if (!nonFatalErrorMap.containsKey(errorString)) {
            nonFatalErrorMap.put(errorString, new ArrayList<String>());
        }
        nonFatalErrorMap.get(errorString).add(featureKey);
    }

    public synchronized Map<String, List<String>> getNonFatalErrors() {
        return nonFatalErrorMap;
    }

    public synchronized void addWriteError(String errorString, String featureKey) {
        incrementWriteErrorCount();
        //log.warn("[" + featureNumber + "] " + errorString);
        if (!writeErrorMap.containsKey(errorString)) {
            writeErrorMap.put(errorString, new ArrayList<String>());
        }
        writeErrorMap.get(errorString).add(featureKey);
    }

    public synchronized Map<String, List<String>> getWriteErrorMap() {
        return writeErrorMap;
    }

	public synchronized String getNonFatalErrorReport(String newLineString, int maxFeatureNumbersPerException) {
        if (newLineString == null) {
            newLineString = DEFAULT_NEW_LINE;
        }
        if (maxFeatureNumbersPerException < 1) {
            maxFeatureNumbersPerException = DEFAULT_MAX_FEATURE_NUMBER_PER_EXCEPTION;
        }

        StringBuilder sb = new StringBuilder();
        if (processName != null) {
            sb.append(MessageFormat.format(resources.getString("report.processFinished"), processName));
            sb.append(newLineString);
            sb.append(newLineString);
        }
        sb.append(getFinishedMessage());
        sb.append(newLineString);
        for (Map.Entry<String, List<String>> entry : nonFatalErrorMap.entrySet()) {
            sb.append(entry.getKey());
            sb.append(newLineString);
            sb.append(resources.getString("report.errorAppliesTo"));
            int i = 0;
            for (String featureKey : entry.getValue()) {
                if (maxFeatureNumbersPerException > 0 && i >= maxFeatureNumbersPerException) {
                    sb.delete(sb.length() - 1, sb.length()); // remove the trailing "," if present
                    sb.append(" ");
                    sb.append(resources.getString("report.and"));
                    sb.append(" ");
                    sb.append(entry.getValue().size() - i);
                    sb.append(" ");
                    sb.append(resources.getString("report.others"));
                    sb.append(".");
                    break;
                } else {
                    sb.append(" ");
                    sb.append(featureKey);
                    sb.append(",");
                }
                i++;
            }
            if (entry.getValue().size() > 0 && i <= maxFeatureNumbersPerException - 1) {
                sb.delete(sb.length() - 1, sb.length()); // remove the trailing "," if present
            }
            sb.append(newLineString);
        }

        sb.append(newLineString);
        for (Map.Entry<String, List<String>> entry : writeErrorMap.entrySet()) {
            sb.append(entry.getKey());
            sb.append(newLineString);
            sb.append(resources.getString("report.errorAppliesTo"));
            int i = 0;
            for (String featureKey : entry.getValue()) {
                if (maxFeatureNumbersPerException > 0 && i >= maxFeatureNumbersPerException) {
                    sb.delete(sb.length() - 1, sb.length()); // remove the trailing "," if present
                    sb.append(" ");
                    sb.append(resources.getString("report.and"));
                    sb.append(" ");
                    sb.append(entry.getValue().size() - i);
                    sb.append(" ");
                    sb.append(resources.getString("report.others"));
                    sb.append(".");
                    break;
                } else {
                    sb.append(" ");
                    sb.append(featureKey);
                    sb.append(",");
                }
                i++;
            }
            if (entry.getValue().size() > 0 && i <= maxFeatureNumbersPerException - 1) {
                sb.delete(sb.length() - 1, sb.length()); // remove the trailing "," if present
            }
            sb.append(newLineString);
        }

        return sb.toString();
    }

    /**
     * Use getNonFatalErrorReport instead.
     * @return
     * @deprecated
     */
    @Deprecated
    public synchronized String getTruncatedErrorReport() {
        return (errorReport.length() > 500 ? errorReport.substring(0, 500) + "... (see log)" : errorReport);
    }

    public synchronized String getErrorReport() {
        return errorReport;
    }

    public synchronized void setErrorReport(String errorReport) {
        this.errorReport = errorReport;
    }

    public synchronized int getNonFatalErrorCount() {
        return nonFatalErrorCount;
    }

    public synchronized void setNonFatalErrorCount(int nonFatalErrorCount) {
        this.nonFatalErrorCount = nonFatalErrorCount;
    }

    public synchronized void incrementNonFatalErrorCount() {
        nonFatalErrorCount++;
    }

    public synchronized int getWriteErrorCount() {
        return writeErrorCount;
    }

    public synchronized void setWriteErrorCount(int writeErrorCount) {
        this.writeErrorCount = writeErrorCount;
    }

    public synchronized void incrementWriteErrorCount() {
        writeErrorCount++;
    }

    public synchronized int getErrorCount() {
        return writeErrorCount + nonFatalErrorCount;
    }

    private String getFinishedMessage() {
        if (getProcessedFeatures() == 0 && getErrorCount() == 0) {
            return resources.getString("report.nothingProcessed");
        } else if (getProcessedFeatures() == getVisitedFeatures() && getErrorCount() == 0) {
            return MessageFormat.format(resources.getString("report.allProcessed"), getProcessedFeatures());
        } else {
            return MessageFormat.format(resources.getString("report.processedWithErrors"), getProcessedFeatures(), getVisitedFeatures(), getWriteErrorCount());
            //+ newLineString + "Using parameters:" + newLineString + "Start:  " + getFeatureStart() + newLineString + "End:    " + getFeatureEnd() + newLineString + "Errors: " + getErrorCount();
        }
    }

    /**
     *
     * @return Total number of features of all feature sources in this process
     * that have been considered a candidate to be processed at this moment
     * in the DataStoreLinkers execution.
     */
    public synchronized int getVisitedFeatures() {
        return visitedFeatures;
    }

    public synchronized void setVisitedFeatures(int visitedFeatures) {
        this.visitedFeatures = visitedFeatures;
    }

    public synchronized void incrementVisitedFeatures() {
        visitedFeatures++;
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
