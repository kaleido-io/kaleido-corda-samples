package io.kaleido.samples;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
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
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;

public class IOUClient {
    private static final Logger logger = LoggerFactory.getLogger(IOUClient.class);
    public AbstractParty getPartyFromAcctName(String acctName, CordaRPCOps rpcOps) {
        AbstractParty party = null;
        logger.info("Initiating the AccountInfo flow...");
        CordaFuture<List<? extends StateAndRef<? extends AccountInfo>>> future = null;
        try {
            FlowHandle<List<? extends StateAndRef<? extends AccountInfo>>> flowHandle = null;
            try {
                flowHandle =  rpcOps.startFlowDynamic(AccountInfoByName.class, acctName);
            } catch (final CordaRuntimeException cre) {
                logger.error("Failed to start the flow", cre);
            }

            logger.info("Started flow, handle: {}", flowHandle.toString());
            future = flowHandle.getReturnValue();
        } catch (final Exception e) {
            logger.error("Failed", e);
        }
        AccountInfo target=null;
        if (future != null) {
            try {
                final List<? extends StateAndRef<? extends AccountInfo>> result = future.get();
                List<? extends AccountInfo> accountInfos = result.stream().map(acct -> acct.getState().getData()).collect(Collectors.toList());

                if (accountInfos.size() < 1) {
                    throw new IllegalArgumentException("Failed to find a account info that matches acctName");
                } else {
                    logger.info("Found {} accounts in the network matching id {}", accountInfos.size(), acctName);
                    logger.info("Pick from the following accounts:");
                    final ArrayList<AccountInfo> selections = new ArrayList<AccountInfo>();
                    int idx = 0;
                    final Iterator<? extends AccountInfo> itr = accountInfos.iterator();
                    while (itr.hasNext()) {
                        final AccountInfo b = itr.next();
                        logger.info("\t{}: {}", idx++, b.toString());
                        selections.add(b);
                    }
                    final Scanner in = new Scanner(System.in);
                    final String s = in.nextLine();
                    final int index = Integer.parseInt(s);
                    logger.info("Your selection is {}", selections.get(index));
                    target = selections.get(index);
                }
            } catch(final Exception e) {
                logger.error("Failed to pick an account", e);
            }
            // invoke flow to get key for given account
            return getPartyFromAccountInfo(target, rpcOps);
        }
        return party;
    }
    public AbstractParty getPartyFromAccountInfo(AccountInfo accountInfo, CordaRPCOps rpcOps) {
        AbstractParty party = null;
        logger.info("Initiating the RequestKeyForAccount flow...");
        CordaFuture<AnonymousParty> future = null;
        try {
            FlowHandle<AnonymousParty> flowHandle = null;
            try {
                flowHandle =  rpcOps.startFlowDynamic(RequestKeyForAccount.class, accountInfo);
            } catch (final CordaRuntimeException cre) {
                logger.error("Failed to start the flow", cre);
            }
            logger.info("Started flow, handle: {}", flowHandle.toString());
            future = flowHandle.getReturnValue();
        } catch (final Exception e) {
            logger.error("Failed", e);
        }
        if(future != null) {
            try {
                party = future.get();
                logger.info("Key: {}", party.getOwningKey());
            } catch(final Exception e) {
                logger.error("Failed to get Party for account info", e);
            }
        }
        return party;
    }

    public AbstractParty getPartyFromId(String id, CordaRPCOps rpcOps) {
        if (id == null) {
            logger.info(
                    "No id is provided, will prompt selection form all available parties");
            id = "";
        }
        final Set<Party> parties = rpcOps.partiesFromName(id, false);
        AbstractParty party = null;
        if (parties.size() < 1) {
            throw new IllegalArgumentException("Failed to find a network party that matches id");
        } else {
            logger.info("Found {} parties in the network matching id {}", parties.size(), id);
            if (id == "") {
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
                party = selections.get(index);
            } else {
                party = parties.iterator().next();
            }
        }
        logger.info("Party: {}", party.toString());
        return party;
    }

    public boolean issueIoU(AbstractParty lender, AbstractParty borrower, final int value, CordaRPCOps rpcOps) {
        logger.info("Initiating the IoU flow...");
        CordaFuture<SignedTransaction> future;
        try {
            FlowHandle<SignedTransaction> flowHandle = null;
            try {
                flowHandle = rpcOps.startFlowDynamic(IOUFlow.Initiator.class, value, lender, borrower);
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