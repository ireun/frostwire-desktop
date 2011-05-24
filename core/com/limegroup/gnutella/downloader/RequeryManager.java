package com.limegroup.gnutella.downloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.FrostWireUtils;

/**
 *  A manager for controlling how requeries are sent in downloads.
 *  This abstract class has specific functionality that differs for
 *  Basic & PRO.
 *  
 *  The manager keeps track of what queries have been sent out,
 *  when queries can begin, and how long queries should wait for results.
 */
class RequeryManager  {

    private static final Log LOG = LogFactory.getLog(RequeryManager.class);
    
    /** The types of requeries that can be currently active. */
    public static enum QueryType { DHT, GNUTELLA };
    
    /**
     * The time to wait between requeries, in milliseconds.  This time can
     * safely be quite small because it is overridden by the global limit in
     * DownloadManager.  Package-access and non-final for testing.
     * 
     * @see com.limegroup.gnutella.DownloadManager#TIME_BETWEEN_GNUTELLA_REQUERIES 
     */
    static long TIME_BETWEEN_REQUERIES = 5L * 60L * 1000L;  //5 minutes
    
    private final RequeryListener requeryListener;
    
    private final DownloadManager downloadManager;
    
    /** The type of the last query this sent out. */
    private volatile QueryType lastQueryType;
    
    /** The number of DHT queries already done for this downloader. */
    private volatile int numDHTQueries;
    
    /** The time the last query of either type was sent out. */
    private volatile long lastQuerySent;
    
    /** True if a gnutella query has been sent. */
    protected volatile boolean sentGnutellaQuery;
    
    /** True if requerying has been activated by the user. */
    protected volatile boolean activated;
    
    /** 
     * a dht lookup to Shutdownable if we get shut down 
     * not null if a DHT query is currently out (and not finished, success or failure)
     */
    private volatile Shutdownable dhtQuery;
    
    private final ConnectionServices connectionServices;
    
    RequeryManager(RequeryListener requeryListener, 
            DownloadManager manager,
            ConnectionServices connectionServices) {
        this.requeryListener = requeryListener;
        this.downloadManager = manager;
        this.connectionServices = connectionServices;
    }
    
    /** Returns true if we're currently waiting for any kinds of results. */
    boolean isWaitingForResults() {
        if(lastQueryType == null)
            return false;
        
        switch(lastQueryType) {
        case DHT: return dhtQuery != null && getTimeLeftInQuery() > 0;
        case GNUTELLA: return getTimeLeftInQuery() > 0;
        }
        
        return false;
    }
    
    /** Returns the kind of last requery we sent. */
    QueryType getLastQueryType() {
        return lastQueryType;
    }
    
    /** Returns how much time, in milliseconds, is left in the current query. */
    long getTimeLeftInQuery() {
        return TIME_BETWEEN_REQUERIES - (System.currentTimeMillis() - lastQuerySent);
    }
        
    /** Sends a requery, if allowed. */
    void sendQuery() {
        if(canSendQueryNow()) {
            if(!sentGnutellaQuery)
                sendGnutellaQuery();
            else
                LOG.debug("Can send a query now, but not sending it?!");
        } else {
            LOG.debug("Tried to send query, but cannot do it now.");
        }
    }
        
    /** True if a requery can immediately be performed or can be triggered from a user action. */
    boolean canSendQueryAfterActivate() {
        return !sentGnutellaQuery;
    }
    
    /** Returns true if a query can be sent right now. */
    boolean canSendQueryNow() {
//        // PRO users can always send the DHT query, but only Gnutella after activate.
//        if(FrostWireUtils.isPro())
//            return (activated && canSendQueryAfterActivate());
//        else
//            return activated && canSendQueryAfterActivate();
        return false;
    }
    
    /** Allows activated queries to begin. */
    void activate() {
        activated = true;
    }
    
   /** Removes all event listeners, cancels ongoing searches and cleans up references. */
    void cleanUp() {
        Shutdownable f = dhtQuery;
        dhtQuery = null;
        if (f != null)
            f.shutdown();
    }
    
    public void handleAltLocSearchDone(boolean success){
        dhtQuery = null;
        // This changes the state to GAVE_UP regardless of success,
        // because even if this was a success (it found results),
        // it's possible the download isn't going to want to use
        // those results.
        requeryListener.lookupFinished(QueryType.DHT);
    }

    

    /** Sends a Gnutella Query */
    private void sendGnutellaQuery() {
        // If we don't have stable connections, wait until we do.
        if (hasStableConnections()) {
            QueryRequest qr = requeryListener.createQuery();
            if(qr != null) {
                downloadManager.sendQuery(qr);
                LOG.debug("Sent a gnutella requery!");
                sentGnutellaQuery = true;
                lastQueryType = QueryType.GNUTELLA;
                lastQuerySent = System.currentTimeMillis();
                requeryListener.lookupStarted(QueryType.GNUTELLA, TIME_BETWEEN_REQUERIES);
            } else {
                sentGnutellaQuery = true;
                requeryListener.lookupFinished(QueryType.GNUTELLA);
            }
        } else {
            LOG.debug("Tried to send a gnutella requery, but no stable connections.");
            requeryListener.lookupPending(QueryType.GNUTELLA, CONNECTING_WAIT_TIME);
        }
    }
    
    /**
     * How long we'll wait before attempting to download again after checking
     * for stable connections (and not seeing any)
     */
    private static final int CONNECTING_WAIT_TIME     = 750;
    private static final int MIN_NUM_CONNECTIONS      = 2;
    private static final int MIN_CONNECTION_MESSAGES  = 6;
    private static final int MIN_TOTAL_MESSAGES       = 45;
            static boolean   NO_DELAY                 = false; // For testing
    
    /**
     *  Determines if we have any stable connections to send a requery down.
     */
    private boolean hasStableConnections() {
        if ( NO_DELAY )
            return true;  // For Testing without network connection

        // TODO: Note that on a private network, these conditions might
        //       be too strict.
        
        // Wait till your connections are stable enough to get the minimum 
        // number of messages
        return connectionServices.countConnectionsWithNMessages(MIN_CONNECTION_MESSAGES) 
                    >= MIN_NUM_CONNECTIONS &&
                    connectionServices.getActiveConnectionMessages() >= MIN_TOTAL_MESSAGES;
    }
}
