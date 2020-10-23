package com.foo.examples.client.samples;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foo.examples.flows.MyFlowInitiator;
import com.foo.examples.contracts.MyState1;
import net.corda.core.CordaRuntimeException;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;

public class FooClient {
    private static final Logger logger = LoggerFactory.getLogger(FooClient.class);

    public Party getBorrowerParty(String borrowerId, CordaRPCOps rpcOps) {
        if (borrowerId == null) {
            logger.info(
                    "To issue new IoU, a borrower is needed. You can specify a unique search string with -borrower-id (using the borrower node Id would be a good idea), or select from the discovered list of parties below.");
            borrowerId = "";
        }
        final Set<Party> parties = rpcOps.partiesFromName(borrowerId, false);
        Party borrower = null;
        if (parties.size() < 1) {
            throw new IllegalArgumentException("Failed to find a network party that matches borrower");
        } else {
            logger.info("Found {} parties in the network matching the borrower id {}", parties.size(), borrowerId);
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
        logger.info("Borrower party: {}", borrower.toString());
        return borrower;
    }

    public boolean startMyflow(Party cpty, final Integer value,  CordaRPCOps rpcOps) {
        logger.info("Initiating the IoU flow...");
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
}