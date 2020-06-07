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

 package io.kaleido;

import java.util.concurrent.Callable;

import com.timgroup.statsd.StatsDClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kaleido.samples.IOUClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;

public class Worker implements Callable<Result> {
    private static final Logger logger = LoggerFactory.getLogger(KaleidoClient.class);

    private String borrowerAcctName;
    private String lenderAcctName;
    private int value;
    private int iterations;
    private IOUClient client;
    private CordaRPCConnection connection;
    private int workerId;
    private StatsDClient statsd;

    public Worker(String borrowerAcctName, String lenderAcctName, int value, int loops, IOUClient client, CordaRPCConnection connection, int workerId, StatsDClient statsd) {
        this.iterations = loops;
        this.connection = connection;
        this.client = client;
        this.borrowerAcctName = borrowerAcctName;
        this.lenderAcctName = lenderAcctName;
        this.value = value;
        this.workerId = workerId;
        this.statsd = statsd;
    }

    @Override
    public Result call() throws Exception  {
        Result res = new Result();
        int i = 0;
        CordaRPCOps rpcOps = connection.getProxy();
        // iteration 0 means going indefinitely
        while (iterations <= 0 || i < iterations) {
            logger.info("Sending IoU issuing transaction #{} (worker #{})", i+1, workerId);
            // the issue transaction flows is made synchronous, the get() call
            // blocks on the future that only resolves after the tx has been finalized
            submitStats("tx.sub");
            boolean success = client.issueIoU(borrowerAcctName, lenderAcctName, value, rpcOps);
            if (success) {
                logger.info("IoU issuing transaction #{} (worker #{}) successfully finalized", i+1, workerId);
                submitStats("tx.sent");;
                res.addSuccess();
            } else {
                logger.error("IoU issuing transaction #{} (worker #{}) failed", i+1, workerId);
                submitStats("tx.fail");
                res.addFailure();
            }
            i++;
        }

        return res;
    }

    private void submitStats(String stat) {
        if (statsd != null) {
            statsd.incrementCounter(Utils.makeTelegrafMetricString(stat, workerId));
        }
    }
}