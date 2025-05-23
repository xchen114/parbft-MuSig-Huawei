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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import parbft.communication.client.CommunicationSystemServerSide;
import parbft.communication.client.CommunicationSystemServerSideFactory;
import parbft.communication.client.RequestReceiver;
import parbft.communication.server.ServersCommunicationLayer;
import parbft.consensus.roles.Backup;
import parbft.consensus.roles.Primary;
import parbft.reconfiguration.ServerViewController;
import parbft.tom.ServiceReplica;
import parbft.tom.core.TOMLayer;
import parbft.tom.core.messages.TOMMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alysson
 */
public class ServerCommunicationSystem extends Thread {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean doWork = true;
    public final long MESSAGE_WAIT_TIME = 100;
    private LinkedBlockingQueue<SystemMessage> inQueue = null;//new LinkedBlockingQueue<SystemMessage>(IN_QUEUE_SIZE);
    protected MessageHandler messageHandler;
    private ServersCommunicationLayer serversConn;
    private CommunicationSystemServerSide clientsConn;
    private ServerViewController controller;

    /**
     * Creates a new instance of ServerCommunicationSystem
     */
    public ServerCommunicationSystem(ServerViewController controller, ServiceReplica replica) throws Exception {
        super("Server CS");

        this.controller = controller;
        
        messageHandler = new MessageHandler();

        inQueue = new LinkedBlockingQueue<SystemMessage>(controller.getStaticConf().getInQueueSize());

        //create a new conf, with updated port number for servers
        //TOMConfiguration serversConf = new TOMConfiguration(conf.getProcessId(),
        //      Configuration.getHomeDir(), "hosts.config");

        //serversConf.increasePortNumber();

        serversConn = new ServersCommunicationLayer(controller, inQueue, replica);

        //******* EDUARDO BEGIN **************//
       // if (manager.isInCurrentView() || manager.isInInitView()) {
            clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
       // }
        //******* EDUARDO END **************//
        //start();
    }

    //******* EDUARDO BEGIN **************//
    public void joinViewReceived() {
        serversConn.joinViewReceived();
    }

    public void updateServersConnections() {
        this.serversConn.updateConnections();
        if (clientsConn == null) {
            clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
        }

    }

    //******* EDUARDO END **************//
    public void setBackup(Backup backup) {
        messageHandler.setBackup(backup);
    }
    public void setPrimary(Primary primary) {
        messageHandler.setPrimary(primary);
    }
    public void setTOMLayer(TOMLayer tomLayer) {
        messageHandler.setTOMLayer(tomLayer);
    }

    public void setRequestReceiver(RequestReceiver requestReceiver) {
        if (clientsConn == null) {
            clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
        }
        clientsConn.setRequestReceiver(requestReceiver);
    }

    /**
     * Thread method responsible for receiving messages sent by other servers.
     */
    @Override
    public void run() {
        
        long count = 0;
        while (doWork) {
            try {
                if (count % 1000 == 0 && count > 0) {
                    logger.debug("After " + count + " messages, inQueue size=" + inQueue.size());
                }

                SystemMessage sm = inQueue.poll(MESSAGE_WAIT_TIME, TimeUnit.MILLISECONDS);

                if (sm != null) {
                    logger.debug("<-------receiving---------- " + sm);
                    messageHandler.processData(sm);
                    count++;
                } else {                
                    messageHandler.verifyPending();               
                }
            } catch (InterruptedException e) {
                
                logger.error("Error processing message",e);
            }
        }
        logger.info("ServerCommunicationSystem stopped.");

    }

    /**
     * Send a message to target processes. If the message is an instance of 
     * TOMMessage, it is sent to the clients, otherwise it is set to the
     * servers.
     *
     * @param targets the target receivers of the message
     * @param sm the message to be sent
     */
    public void send(int[] targets, SystemMessage sm) {
        if (sm instanceof TOMMessage) {
            clientsConn.send(targets, (TOMMessage) sm, false);
        } else {
            logger.debug("--------sending----------> " + sm);
            serversConn.send(targets, sm, true);
        }
    }
    public void sendToLeader(int targets, SystemMessage sm) {
        logger.debug("----sendingToOtherLeader----> " + sm);
        serversConn.sendingToOtherLeader(targets, sm, true);
    }

    public ServersCommunicationLayer getServersConn() {
        return serversConn;
    }
    
    public CommunicationSystemServerSide getClientsConn() {
        return clientsConn;
    }
    
    @Override
    public String toString() {
        return serversConn.toString();
    }
    
    public void shutdown() {
        
        logger.info("Shutting down communication layer");
        
        this.doWork = false;        
        clientsConn.shutdown();
        serversConn.shutdown();
    }
}
