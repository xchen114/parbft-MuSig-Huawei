/**
 Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package parbft.communication;

import parbft.consensus.messages.*;
import parbft.consensus.roles.Backup;
import parbft.consensus.roles.Primary;
import parbft.statemanagement.SMMessage;
import parbft.tom.core.TOMLayer;
import parbft.tom.core.messages.TOMMessage;
import parbft.tom.core.messages.ForwardedMessage;
import parbft.tom.leaderchange.LCMessage;
import parbft.tom.util.TOMUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author edualchieri
 */
public class MessageHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Backup backup;
    private Primary primary;
    private TOMLayer tomLayer;
    private Mac mac;
    private ReentrantLock lock = new ReentrantLock();

    public MessageHandler() {
        try {
            this.mac = TOMUtil.getMacFactory();
        } catch (NoSuchAlgorithmException /*| NoSuchPaddingException*/ ex) {
            logger.error("Failed to create MAC engine",ex);
        }
    }
    public void setBackup(Backup backup) {
        this.backup = backup;
    }
    public void setPrimary(Primary primary){
        this.primary = primary;
    }

    public void setTOMLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    @SuppressWarnings("unchecked")
    protected void processData(SystemMessage sm) {
        if (sm instanceof ConsensusMessage) {
            int myId = tomLayer.controller.getStaticConf().getProcessId();
            ConsensusMessage consMsg = (ConsensusMessage) sm;
            if (tomLayer.controller.getStaticConf().getUseMACs() == 0 || consMsg.authenticated || consMsg.getSender() == myId) backup.deliver(consMsg);
            else if (consMsg.getType() == MessageFactory.PREPARE && consMsg.getProof() != null) {
                //We are going to verify the MAC vector at the algorithm level
                HashMap<Integer, byte[]> macVector = (HashMap<Integer, byte[]>) consMsg.getProof();

                byte[] recvMAC = macVector.get(myId);

                ConsensusMessage cm = new ConsensusMessage(MessageFactory.PREPARE,consMsg.getNumber(),
                        consMsg.getEpoch(), consMsg.getSender(), consMsg.getValue());

                ByteArrayOutputStream bOut = new ByteArrayOutputStream(248);
                try {
                    new ObjectOutputStream(bOut).writeObject(cm);
                } catch (IOException ex) {
                    logger.error("Failed to serialize consensus message",ex);
                }

                byte[] data = bOut.toByteArray();

                //byte[] hash = tomLayer.computeHash(data);

                byte[] myMAC = null;

                /*byte[] k = tomLayer.getCommunication().getServersConn().getSecretKey(paxosMsg.getSender()).getEncoded();
                SecretKeySpec key = new SecretKeySpec(new String(k).substring(0, 8).getBytes(), "DES");*/

                SecretKey key = tomLayer.getCommunication().getServersConn().getSecretKey(consMsg.getSender());
                try {
                    this.mac.init(key);
                    myMAC = this.mac.doFinal(data);
                } catch (/*IllegalBlockSizeException | BadPaddingException |*/ InvalidKeyException ex) {
                    logger.error("Failed to generate MAC",ex);
                }

                if (recvMAC != null && myMAC != null && Arrays.equals(recvMAC, myMAC))
                    backup.deliver(consMsg);
                else {
                    logger.warn("Invalid MAC from " + sm.getSender());
                }
            } else {
                logger.warn("Discarding unauthenticated message from " + sm.getSender());
            }

        }else if(sm instanceof SigShareMessage){
            primary.deliver(sm);
        }else if(sm instanceof PrePrepareMessage){
            primary.deliver(sm);
        }else if(sm instanceof PrepareStepOneMessage){
            PrepareStepOneMessage prepareStepOneMessage = (PrepareStepOneMessage)sm;
            backup.deliver(prepareStepOneMessage);
        }else if(sm instanceof VerificationMessage){
            lock.lock();
            VerificationMessage verificationMessage = (VerificationMessage)sm;
            if(!verificationMessage.isLeaderMessage())
                primary.deliver(verificationMessage);
            else
                backup.deliver(verificationMessage);
            lock.unlock();
        }else if(sm instanceof VerificationCommitMessage){
            lock.lock();
            VerificationCommitMessage verificationCommitMessage = (VerificationCommitMessage)sm;
            primary.deliver(verificationCommitMessage);
            lock.unlock();
        }else if(sm instanceof CommitMessage){
            lock.lock();
            CommitMessage commitMessage = (CommitMessage) sm;
            primary.deliver(commitMessage);
            lock.unlock();
        }else{
            if (tomLayer.controller.getStaticConf().getUseMACs() == 0 || sm.authenticated) {
                /*** This is Joao's code, related to leader change */
                if (sm instanceof LCMessage) {
                    LCMessage lcMsg = (LCMessage) sm;
                    String type = null;
                    switch(lcMsg.getType()) {
                        case TOMUtil.STOP:
                            type = "STOP";
                            break;
                        case TOMUtil.STOPDATA:
                            type = "STOPDATA";
                            break;
                        case TOMUtil.SYNC:
                            type = "SYNC";
                            break;
                        default:
                            type = "LOCAL";
                            break;
                    }

                    if (lcMsg.getReg() != -1 && lcMsg.getSender() != -1)
                        logger.info("Received leader change message of type {} for regency {} from replica {}", type, lcMsg.getReg(), lcMsg.getSender());
                    else logger.debug("Received leader change message from myself");
                    if (lcMsg.TRIGGER_LC_LOCALLY) tomLayer.requestsTimer.run_lc_protocol();
                    else tomLayer.getSynchronizer().deliverTimeoutRequest(lcMsg);
                    /**************************************************************/

                } else if (sm instanceof ForwardedMessage) {
                    TOMMessage request = ((ForwardedMessage) sm).getRequest();
                    tomLayer.requestReceived(request);

                    /** This is Joao's code, to handle state transfer */
                } else if (sm instanceof SMMessage) {
                    SMMessage smsg = (SMMessage) sm;
                    // System.out.println("(MessageHandler.processData) SM_MSG received: type " + smsg.getType() + ", regency " + smsg.getRegency() + ", (replica " + smsg.getSender() + ")");
                    switch(smsg.getType()) {
                        case TOMUtil.SM_REQUEST:
                            tomLayer.getStateManager().SMRequestDeliver(smsg, tomLayer.controller.getStaticConf().isBFT());
                            break;
                        case TOMUtil.SM_REPLY:
                            tomLayer.getStateManager().SMReplyDeliver(smsg, tomLayer.controller.getStaticConf().isBFT());
                            break;
                        case TOMUtil.SM_ASK_INITIAL:
                            tomLayer.getStateManager().currentConsensusIdAsked(smsg.getSender());
                            break;
                        case TOMUtil.SM_REPLY_INITIAL:
                            tomLayer.getStateManager().currentConsensusIdReceived(smsg);
                            break;
                        default:
                            tomLayer.getStateManager().stateTimeout();
                            break;
                    }
                    /******************************************************************/
                }else {
                    logger.warn("UNKNOWN MESSAGE TYPE: " + sm);
                }
            } else {
                logger.warn("Discarding unauthenticated message from " + sm.getSender());
            }
        }
    }

    protected void verifyPending() {
        tomLayer.processOutOfContext();
    }
}
