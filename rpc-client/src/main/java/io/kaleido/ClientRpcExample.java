package io.kaleido;

import io.kaleido.flow.ExampleFlow;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.CordaRuntimeException;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.List;
import io.kaleido.utils.CliArgs;
import io.kaleido.state.IOUState;

class ClientRpcExample {
    private static final Logger logger = LoggerFactory.getLogger(ClientRpcExample.class);

    public static void main(String[] args) {
        String USAGE = "Usage: rpcClient -url <node address> -username <username> -password <password>";
        CliArgs cliArgs = new CliArgs(args);

        String url = cliArgs.switchValue("-url");
        String username = cliArgs.switchValue("-username");
        String password = cliArgs.switchValue("-password");
        String borrowerId = cliArgs.switchValue("-borrower");
        if (url == null || username == null || password == null) {
            throw new IllegalArgumentException(USAGE);
        }

        Boolean newIoU = cliArgs.switchPresent("-newIoU");
        Boolean query = cliArgs.switchPresent("-query");
        if (!newIoU && !query) {
            throw new IllegalArgumentException("Must specify the action with '-newIoU' or '-query'");
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(url);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress);
        final CordaRPCConnection connection = client.start(username, password);
        final CordaRPCOps cordaRPCOperations = connection.getProxy();

        logger.info("Calling node for current time...");
        logger.info(cordaRPCOperations.currentNodeTime().toString());
        logger.info("Calling node for node info...");
        logger.info(cordaRPCOperations.nodeInfo().toString());
        logger.info("Calling node for notary information...");
        logger.info(cordaRPCOperations.notaryIdentities().toString());

        if (newIoU) {
            issueIoU(cordaRPCOperations, borrowerId);
        }
        if (query) {
            String txId = cliArgs.switchValue("-txId");
            if (txId == null) {
                throw new IllegalArgumentException("Must specify '-txId' to query");
            }
            query(txId, cordaRPCOperations);
        }

        connection.notifyServerAndClose();
    }

    private static void issueIoU(final CordaRPCOps cordaRPCOperations, String borrowerId) {
        logger.info("Initiating the IoU flow...");
        try {
            if (borrowerId == null) {
                throw new IllegalArgumentException("To issue new IoU, please provide a unique lookup string that can identity the borrower, with -borrower, using the borrower node Id would be a good idea");
            }
            final Set<Party> parties = cordaRPCOperations.partiesFromName(borrowerId, false);
            if (parties.size() < 1) {
                logger.error("Failed to find borrower party");
                throw new IllegalArgumentException();
            }
            logger.info("Size of parties: {}", parties.size());
            final Party borrower = parties.iterator().next();
            logger.info("Borrower party: {}", borrower.toString());
            FlowHandle<SignedTransaction> flowHandle = null;
            try {
                flowHandle = cordaRPCOperations.startFlowDynamic(ExampleFlow.Initiator.class, 100, borrower);
            } catch(CordaRuntimeException cre) {
                logger.error("Failed to start the flow", cre);
            }
            
            logger.info("Started flow, handle: {}", flowHandle.toString());
            CordaFuture<SignedTransaction> future = flowHandle.getReturnValue();
            final SignedTransaction result = future.get();
            logger.info("Signed tx: {}", result);
            final String msg = result.getTx().getOutputStates().get(0).toString();
            logger.info(msg);
        } catch (Exception e) {
            logger.error("Failed", e);
        }
    }

    private static void query(final String txId, final CordaRPCOps cordaRPCOperations) {
        try {
            Vault.Page<IOUState> result = cordaRPCOperations.vaultQuery(IOUState.class);
            List<StateAndRef<IOUState>> states = result.getStates();
            logger.info("Number of IOU states returned from query: {}", states.size());
            for (StateAndRef<IOUState> state : states) {
                if (state.getRef().getTxhash().toString().equals(txId)) {
                    logger.info("Found state by transaction hash:", state);
                    TransactionState<IOUState> s = state.getState();
                    logger.info("\tNotary: {}", s.getNotary());
                    logger.info("\tValue: {}", s.getData().getValue());
                    logger.info("\tIoU lender: {}", s.getData().getLender());
                    logger.info("\tIoU borrower: {}", s.getData().getBorrower());
                }
            }
        } catch(Exception e) {
            logger.error("Failed", e);
        }
    }

}