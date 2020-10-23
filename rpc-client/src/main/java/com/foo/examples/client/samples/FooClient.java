package com.foo.examples.client.samples;

import java.util.*;

import net.corda.core.node.services.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foo.examples.flows.MyFlowInitiator;
import com.foo.examples.contracts.MyState1;
import net.corda.core.CordaRuntimeException;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;

public class FooClient {
    private static final Logger logger = LoggerFactory.getLogger(FooClient.class);

    public Party getCounterParty(String borrowerId, CordaRPCOps rpcOps) {
        if (borrowerId == null) {
            logger.info(
                    "To start MyInitiator, a counterparty is needed. You can specify a unique search string with -borrower-id (using the borrower node Id would be a good idea), or select from the discovered list of parties below.");
            borrowerId = "";
        }
        final Set<Party> parties = rpcOps.partiesFromName(borrowerId, false);
        Party borrower = null;
        if (parties.size() < 1) {
            throw new IllegalArgumentException("Failed to find a network party that matches counterparty");
        } else {
            logger.info("Found {} parties in the network matching the counterparty id {}", parties.size(), borrowerId);
            if (borrowerId == "") {
                logger.info("Pick from the following parties:");
                final ArrayList<Party> selections = new ArrayList<Party>();
                int idx = 0;
                final Iterator<Party> itr = parties.iterator();
                while (itr.hasNext()) {
                    final Party b = itr.next();
                    logger.info("\t{}: {}", idx++, b.toString());
                    selections.add(b);
                }
                final Scanner in = new Scanner(System.in);
                final String s = in.nextLine();
                final int index = Integer.parseInt(s);
                logger.info("Your selection is {}", selections.get(index));
                borrower = selections.get(index);
            } else {
                borrower = parties.iterator().next();
            }
        }
        logger.info("counterparty party: {}", borrower.toString());
        return borrower;
    }

    public void query(final String txId, CordaRPCOps rpcOps) {
        try {
            Vault.Page<MyState1> result = rpcOps.vaultQuery(MyState1.class);
            final List<StateAndRef<MyState1>> states = result.getStates();
            logger.info("Number of MyFlowInitiator transactions returned from query: {}", states.size());
            for (final StateAndRef<MyState1> state : states) {
                final TransactionState<MyState1> s = state.getState();
                if (txId == null) {
                    logger.info("Id: {}", state.getRef().getTxhash());
                    printStateDetails(s);
                } else if (state.getRef().getTxhash().toString().equals(txId)) {
                    logger.info("Found transaction by hash: {}", txId);
                    printStateDetails(s);
                }
            }
        } catch (final Exception e) {
            logger.error("Failed", e);
        }
    }

    public boolean startMyflow(Party cpty, final Integer value,  CordaRPCOps rpcOps) {
        logger.info("Initiating the MyInitiatorFlow...");
        CordaFuture<Void> future;
        try {
            FlowHandle<Void> flowHandle = null;
            try {
                flowHandle = rpcOps.startFlowDynamic(MyFlowInitiator.class, value, cpty);
            } catch (final CordaRuntimeException cre) {
                logger.error("Failed to start the flow", cre);
            }

            logger.info("Started flow, handle: {}", flowHandle.toString());
        } catch (final Exception e) {
            logger.error("Failed", e);
            return false;
        }
        return true;
    }

    private static void printStateDetails(final TransactionState<MyState1> s) {
        logger.info("\tNotary: {}", s.getNotary());
        logger.info("\tValue: {}", s.getData().getValue());
        logger.info("\tSender: {}", s.getData().getSender());
        logger.info("\tReceiver: {}", s.getData().getReceiver());
        logger.info("\tMsg: {}", s.getData().getMsg());
    }
}