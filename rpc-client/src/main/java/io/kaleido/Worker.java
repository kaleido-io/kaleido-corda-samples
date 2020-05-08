package io.kaleido;

import java.util.concurrent.Callable;

import com.timgroup.statsd.StatsDClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kaleido.samples.IOUClient;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;

public class Worker implements Callable<Result> {
    private static final Logger logger = LoggerFactory.getLogger(KaleidoClient.class);

    private Party borrower;
    private int value;
    private int iterations;
    private CordaRPCOps rpcOps;
    private int workerId;
    private StatsDClient statsd;

    public Worker(Party borrower, int value, int loops, CordaRPCOps rpcOps, int workerId, StatsDClient statsd) {
        this.iterations = loops;
        this.rpcOps = rpcOps;
        this.borrower = borrower;
        this.value = value;
        this.workerId = workerId;
        this.statsd = statsd;
    }

    @Override
    public Result call() throws Exception  {
        Result res = new Result();
        int i = 0;
        // iteration 0 means going indefinitely
        while (iterations <= 0 || i < iterations) {
            logger.info("Sending IoU issuing transaction #{} (worker #{})", i+1, workerId);
            IOUClient rpc = new IOUClient(rpcOps);
            // the issue transaction flows is made synchronous, the get() call
            // blocks on the future that only resolves after the tx has been finalized
            statsd.incrementCounter(Utils.makeTelegrafMetricString("tx.sub", workerId));
            boolean success = rpc.issueIoU(borrower, value);
            if (success) {
                logger.info("IoU issuing transaction #{} (worker #{}) successfully finalized", i+1, workerId);
                statsd.incrementCounter(Utils.makeTelegrafMetricString("tx.sent", workerId));
                res.addSuccess();
            } else {
                logger.error("IoU issuing transaction #{} (worker #{}) failed", i+1, workerId);
                statsd.incrementCounter(Utils.makeTelegrafMetricString("tx.fail", workerId));
                res.addFailure();
            }
            i++;
        }

        return res;
    }
}