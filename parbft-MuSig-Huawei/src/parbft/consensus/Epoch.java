package parbft.consensus;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import parbft.consensus.messages.ConsensusMessage;

import parbft.reconfiguration.ServerViewController;
import parbft.reconfiguration.views.View;
import parbft.tom.core.messages.TOMMessage;


/**
 * This class stands for a consensus epoch
 */
public class Epoch implements Serializable {

    private static final long serialVersionUID = -2891450035863688295L;
    private final transient Consensus consensus; // Consensus where the epoch belongs to

    private final int timestamp; // Epochs's timestamp
    private final int me; // Process ID
    private boolean alreadyRemoved = false; // indicates if this epoch was removed from its consensus
    public byte[] propValue = null; // proposed value
    public TOMMessage[] deserializedPropValue = null; //utility var
    public byte[] propValueHash = null; // proposed value hash
    public HashSet<ConsensusMessage> proof; // proof from other processes

    private View lastView = null;

    private ServerViewController controller;

    /**
     * Creates a new instance of Epoch for acceptors
     * @param controller
     * @param parent Consensus to which this epoch belongs
     * @param timestamp Timestamp of the epoch
     */
    public Epoch(ServerViewController controller, Consensus parent, int timestamp) {
        this.consensus = parent;
        this.timestamp = timestamp;
        this.controller = controller;
        this.proof = new HashSet<>();
        //ExecutionManager manager = consensus.getManager();

        this.lastView = controller.getCurrentView();
        this.me = controller.getStaticConf().getProcessId();

        //int[] acceptors = manager.getAcceptors();
        int n = controller.getCurrentViewN(controller.getStaticConf().getGroup(controller.getStaticConf().getProcessId()));
    }
            
    /**
     * Set this epoch as removed from its consensus instance
     */
    public void setRemoved() {
        this.alreadyRemoved = true;
    }

    /**
     * Informs if this epoch was removed from its consensus instance
     * @return True if it is removed, false otherwise
     */
    public boolean isRemoved() {
        return this.alreadyRemoved;
    }


    public void addToProof(ConsensusMessage pm) {
        proof.add(pm);
    }
    
    public Set<ConsensusMessage> getProof() {
        return proof;
    }
    /**
     * Retrieves the duration for the timeout
     * @return Duration for the timeout
     */
    /*public long getTimeout() {
        return this.timeout;
    }*/

    /**
     * Retrieves this epoch's timestamp
     * @return This epoch's timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Retrieves this epoch's consensus
     * @return This epoch's consensus
     */
    public Consensus getConsensus() {
        return consensus;
    }


    /*************************** DEBUG METHODS *******************************/



    @Override
    public boolean equals(Object o) {
        return this == o;
    }
    
    /**
     * Clear all epoch info.
     */
    public void clear() {

        int n = controller.getCurrentViewN(controller.getStaticConf().getGroup(controller.getStaticConf().getProcessId()));
        
        this.proof = new HashSet<ConsensusMessage>();
    }
}
