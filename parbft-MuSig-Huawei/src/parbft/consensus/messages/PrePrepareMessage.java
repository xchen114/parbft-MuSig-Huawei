package parbft.consensus.messages;

import parbft.communication.SystemMessage;
import org.bouncycastle.math.ec.ECPoint;
import parbft.tom.util.ECschnorrSig;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PrePrepareMessage extends SystemMessage {
    private byte[] Q = null;
    private byte[] pk = null;
    public PrePrepareMessage(final ECPoint Q, final ECPoint pk) {
        this.pk = pk.getEncoded(false);
        this.Q = Q.getEncoded(false);
    }

    public PrePrepareMessage() {
    }

    public byte[] getQ() {
        return Q;
    }

    public void setQ(byte[] q) {
        Q = q;
    }

    public byte[] getPk() {
        return pk;
    }

    public void setPk(byte[] pk) {
        this.pk = pk;
    }

    @Override
    public String toString() {
        return "PrePrepareMessage{" +
                "Q=" + ECschnorrSig.toHexString(Q) +
                ", pk=" + ECschnorrSig.toHexString(pk) +
                ", sender=" + sender +
                '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        if(Q == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(Q.length);
            out.write(Q);
        }
        if(pk == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(pk.length);
            out.write(pk);
        }
    }
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int toRead = in.readInt();
        if(toRead != -1) {
            Q = new byte[toRead];
            do{
                toRead -= in.read(Q, Q.length-toRead, toRead);
            } while(toRead > 0);
        }
        toRead = in.readInt();
        if(toRead != -1) {
            pk = new byte[toRead];
            do{
                toRead -= in.read(pk, pk.length-toRead, toRead);
            } while(toRead > 0);
        }
    }
}
