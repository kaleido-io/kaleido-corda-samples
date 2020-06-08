package io.kaleido.samples;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kaleido.samples.flow.IOUFlow;
import io.kaleido.samples.state.IOUState;
import net.corda.core.CordaRuntimeException;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;

public class IOUClient {
    private static final Logger logger = LoggerFactory.getLogger(IOUClient.class);
    public boolean issueIoU(String borrowerAccount, String lenderAccount, final int value, CordaRPCOps rpcOps) {
        logger.info("Initiating the IoU flow...");
        CordaFuture<SignedTransaction> future;
        try {
            FlowHandle<SignedTransaction> flowHandle = null;
            try {
                flowHandle = rpcOps.startFlowDynamic(IOUFlow.Initiator.class, value, lenderAccount, borrowerAccount);
            } catch (final CordaRuntimeException cre) {
                logger.error("Failed to start the flow", cre);
            }

            logger.info("Started flow, handle: {}", flowHandle.toString());
            future = flowHandle.getReturnValue();
        } catch (final Exception e) {
            logger.error("Failed", e);
            return false;
        }

        if (future != null) {
            try {
                final SignedTransaction result = future.get();
                logger.info("Signed tx: {}", result);
                final String msg = result.getTx().getOutputStates().get(0).toString();
                logger.info(msg);
            } catch(final Exception e) {
                logger.error("Failed to get transaction state", e);
                return false;
            }
        }

        return true;
    }

    public void query(final String txId, CordaRPCOps rpcOps) {
        try {
            Vault.Page<IOUState> result = rpcOps.vaultQuery(IOUState.class);
            final List<StateAndRef<IOUState>> states = result.getStates();
            logger.info("Number of IOU transactions returned from query: {}", states.size());
            for (final StateAndRef<IOUState> state : states) {
                final TransactionState<IOUState> s = state.getState();
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

    private static void printStateDetails(final TransactionState<IOUState> s) {
        logger.info("\tNotary: {}", s.getNotary());
        logger.info("\tValue: {}", s.getData().getValue());
        logger.info("\tIoU lender: {}", s.getData().getLender());
        logger.info("\tIoU borrower: {}", s.getData().getBorrower());
    }
}