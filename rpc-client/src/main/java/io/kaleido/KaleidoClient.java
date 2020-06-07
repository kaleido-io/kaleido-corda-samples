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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kaleido.samples.IOUClient;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "kldc", mixinStandardHelpOptions = true, version = "1.0",
         description = "Kaleido Client for submitting transactions to Corda networks.")
public class KaleidoClient implements Callable<Integer> {
    @Parameters(index = "0", description = "issue, query")
    private String subcommand;

    @Option(names = {"-u", "--url"}, description = "URL of the target Corda node or the local Kaleido bridge endpoint")
    private String url;

    @Option(names = {"-n", "--username"}, description = "username for authentiation")
    private String username;

    @Option(names = {"-p", "--password"}, description = "password for authentiation")
    private String password;

    @Option(names = {"-b", "--borrower-account-name"}, description = "Name of the borrower account to issue the IoU to")
    private String borrowerAcctName;

    @Option(names = {"-b", "--lender-account-name"}, description = "Name of the lender account to issue the IoU to")
    private String lenderAcctName;

    @Option(names = {"-v", "--value"}, description = "Value of the issued IoU contract")
    private Integer value = 100;

    @Option(names = {"-w", "--workers"}, description = "Number of concurrent workers, default 1")
    private Integer workers = 1;

    @Option(names = {"-l", "--loops"}, description = "Loops each worker executes before exiting, default 1 (0=infinite)")
    private Integer loops = 1;
    
    @Option(names = {"-i", "--tx-id"}, description = "Transaction id of an existing transaction")
    private String txId;
    
    @Option(names = {"-m", "--metrics-server"}, description = "URL of the statsd metrics server, such as Telegraf, to submit metrics data to")
    private String metricsServer;

    private static final Logger logger = LoggerFactory.getLogger(KaleidoClient.class);
    private ExecutorService executor;

    @Override
    public Integer call() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(url);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress);
        final CordaRPCConnection connection = client.start(username, password);
        final IOUClient iouClient = new IOUClient();

        StatsDClient statsd = null;
        if (metricsServer != null) {
            statsd = new NonBlockingStatsDClient("corda", metricsServer, 8125);
        }

        // ready to kick off
        executor = Executors.newFixedThreadPool(workers);

        if (subcommand.equals("issue")) {
            // prepare with possibly prompting users for the borrower party
            IOUClient c = new IOUClient();
            List<Worker> tasks = new ArrayList<Worker>();
            for (int i=0; i<workers; i++) {
                Worker w = new Worker(borrowerAcctName, lenderAcctName, value, loops, iouClient, connection, i+1, statsd);
                tasks.add(w);
            }

            List<Future<Result>> results = null;
            long start = System.currentTimeMillis();
            try {
                results = executor.invokeAll(tasks);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            executor.shutdown();
         
            int totalSuccesses = 0;
            int totalFailures = 0;
            for (int i = 0; i < results.size(); i++) {
                Future<Result> future = results.get(i);
                try {
                    Result result = future.get();
                    totalSuccesses += result.getSuccesses();
                    totalFailures += result.getFailures();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            long elapsedTime = (end - start) / 1000;
            System.out.println("\n========Printing the results======");
            System.out.printf("\tTotal successes: %s\n", totalSuccesses);
            System.out.printf("\tTotal failures: %s\n", totalFailures);
            System.out.printf("\tElapsed time: %s seconds\n", elapsedTime);
            System.out.printf("\tTPS: %s\n", (totalSuccesses + totalFailures) / elapsedTime);
         } else if (subcommand.equals("query")) {
            Future<String> result = this.queryTx(connection.getProxy(), txId);
            result.get();
        }

        return 0;
    }

    private Future<String> queryTx(CordaRPCOps rpcOps, String txId) {
        return executor.submit(() -> {
            IOUClient rpc = new IOUClient();
            rpc.query(txId, rpcOps);
            return "success";
        });
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new KaleidoClient());
        final int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}