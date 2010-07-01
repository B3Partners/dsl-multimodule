/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import java.util.Properties;

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


    public Status(Properties batch) {
        if (batch.containsKey(FEATURES_START))
            featureStart = ActionFactory.toInteger(batch.getProperty(FEATURES_START));
        if (batch.containsKey(FEATURES_END))
            featureEnd = ActionFactory.toInteger(batch.getProperty(FEATURES_END));
    }

    public Status(nl.b3p.datastorelinker.entity.Process process) {
        if (process.getFeaturesStart() != null)
            featureStart = process.getFeaturesStart();
        if (process.getFeaturesEnd() != null)
            featureEnd = process.getFeaturesEnd();
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

    public synchronized boolean isInterrupted() {
        return interrupted;
    }

    public synchronized void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
