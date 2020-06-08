package io.kaleido.samples;
import java.util.*;

import io.kaleido.samples.flow.NewKeyForAccount;
import io.kaleido.samples.flow.ShareAccountTo;
import net.corda.core.identity.PartyAndCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kaleido.samples.flow.CreateNewAccount;
import net.corda.core.CordaRuntimeException;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;

public class AccountUtils {
    private static final Logger logger = LoggerFactory.getLogger(AccountUtils.class);
    public Party getPartyFromId(String shareToId, CordaRPCOps rpcOps) {
        if (shareToId == null) {
            logger.info(
                    "To share account info with other party, You can specify a unique search string with --share-to, or select from the discovered list of parties below.");
            shareToId = "";
        }
        final Set<Party> parties = rpcOps.partiesFromName(shareToId, false);
        Party shareTo = null;
        if (parties.size() < 1) {
            throw new IllegalArgumentException("Failed to find a network party that matches id");
        } else {
            logger.info("Found {} parties in the network matching the shareTo id {}", parties.size(), shareToId);
            if (shareToId == "") {
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
                shareTo = selections.get(index);
            } else {
                shareTo = parties.iterator().next();
            }
        }
        logger.info("Borrower party: {}", shareTo.toString());
        return shareTo;
    }
    public boolean createAccount(String acctName, CordaRPCOps rpcOps){
        logger.info("Initiating create new account flow...");
        CordaFuture<String> future;
        try {
            FlowHandle<String> flowHandle = null;
            try {
                flowHandle = rpcOps.startFlowDynamic(CreateNewAccount.class, acctName);
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
                final String result = future.get();
                logger.info("Account created: {}", result);
            } catch(final Exception e) {
                logger.error("Failed to get transaction state", e);
                return false;
            }
        }

        return true;
    }

    public boolean createNewKeyForAccount(UUID accountId, CordaRPCOps rpcOps) {
        logger.info("Initiating create new key for account flow...");
        CordaFuture<PartyAndCertificate> future;
        try {
            FlowHandle<PartyAndCertificate> flowHandle = null;
            try {
                flowHandle = rpcOps.startFlowDynamic(NewKeyForAccount.class, accountId);
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
                final PartyAndCertificate result = future.get();
                logger.info("Key generated for the account: {}", result.getOwningKey());
            } catch(final Exception e) {
                logger.error("Failed to get transaction state", e);
                return false;
            }
        }

        return true;
    }

    public boolean shareAccountTo(String accName, Party shareTo, CordaRPCOps rpcOps) {
        logger.info("Initiating share accountInfo flow...");
        CordaFuture<String> future;
        try {
            FlowHandle<String> flowHandle = null;
            try {
                flowHandle = rpcOps.startFlowDynamic(ShareAccountTo.class, accName, shareTo);
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
                final String result = future.get();
                logger.info("Shared account info: {} {}",accName, result);
            } catch(final Exception e) {
                logger.error("Failed to get transaction state", e);
                return false;
            }
        }

        return true;
    }
}
