package parbft.consensus.messages;

import parbft.communication.SystemMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class VerificationCommitMessage extends SystemMessage {
    private int processId;

    public VerificationCommitMessage() {
    }

    public VerificationCommitMessage(int processId) {
        this.processId = processId;
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(processId);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        processId = in.readInt();
    }
}
