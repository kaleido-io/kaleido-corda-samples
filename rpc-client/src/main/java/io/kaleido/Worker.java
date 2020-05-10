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

    private Party borrower;
    private int value;
    private int iterations;
    private IOUClient client;
    private CordaRPCConnection connection;
    private int workerId;
    private StatsDClient statsd;

    public Worker(Party borrower, int value, int loops, IOUClient client, CordaRPCConnection connection, int workerId, StatsDClient statsd) {
        this.iterations = loops;
        this.connection = connection;
        this.client = client;
        this.borrower = borrower;
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
            boolean success = client.issueIoU(borrower, value, rpcOps);
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