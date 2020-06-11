/*
 * Copyright 2020 Kaleido
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.kaleido.samples.contract;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.stream.Collectors;

import io.kaleido.samples.state.IOUState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [IOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [IOU].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
public class IOUContract implements Contract {
    public static final String ID = "io.kaleido.samples.contract.IOUContract";

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands.Create> command = requireSingleCommand(tx.getCommands(), Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            require.using("No inputs should be consumed when issuing an IOU.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final IOUState out = tx.outputsOfType(IOUState.class).get(0);
            require.using("The lender and the borrower cannot be the same entity.",
                    out.getLender() != out.getBorrower());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

            // IOU-specific constraints.
            require.using("The IOU's value must be non-negative.",
                    out.getValue() > 0);

            return null;
        });
    }

    /**
     * This contract only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}