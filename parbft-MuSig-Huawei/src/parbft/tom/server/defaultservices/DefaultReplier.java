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
package parbft.tom.server.defaultservices;

import parbft.tom.MessageContext;
import parbft.tom.ReplicaContext;
import parbft.tom.core.messages.TOMMessage;
import parbft.tom.server.Replier;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.LoggerFactory;

/**
 *
 * @author miguel
 */
public class DefaultReplier implements Replier{
    
    private Lock replyLock = new ReentrantLock();
    private Condition contextSetted = replyLock.newCondition();
    private ReplicaContext rc;
    
    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {
        
        
        while (rc == null) {
            
            try {

                this.replyLock.lock();

                this.contextSetted.await();
         
                this.replyLock.unlock();

            } catch (InterruptedException ex) {
                LoggerFactory.getLogger(this.getClass()).error("Interruption while waiting/aquiring condition", ex);
            }
        }
        
        rc.getServerCommunicationSystem().send(new int[]{request.getSender()}, request.reply);
        
    }

    @Override
    public void setReplicaContext(ReplicaContext rc) {
        
        this.replyLock.lock();
        
        this.rc = rc;
        
        this.contextSetted.signalAll();
        
        this.replyLock.unlock();
    }
    
}
