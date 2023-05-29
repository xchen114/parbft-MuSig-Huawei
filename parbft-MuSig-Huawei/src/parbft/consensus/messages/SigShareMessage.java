package parbft.consensus.messages;

import parbft.communication.SystemMessage;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;

public class SigShareMessage extends SystemMessage {
    private BigInteger r;
    private BigInteger s;
    private byte[] Q = null;
    private byte[] pk = null;
    public SigShareMessage() {
    }
    public SigShareMessage(BigInteger r, BigInteger s, ECPoint q, ECPoint pk) {
        this.r = r;
        this.s = s;
        this.Q = q.getEncoded(false);
        this.pk = pk.getEncoded(false);
    }

    public BigInteger getR() {
        return r;
    }

    public void setR(BigInteger r) {
        this.r = r;
    }

    public BigInteger getS() {
        return s;
    }

    public void setS(BigInteger s) {
        this.s = s;
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
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(r);
        out.writeObject(s);
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
        r = (BigInteger) in.readObject();
        s = (BigInteger) in.readObject();
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
