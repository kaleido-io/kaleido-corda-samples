package io.kaleido;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utils {
    private static Long pid;
    private static String servername;
    public static long getPid() {
        if (pid == null) {
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();

            // Get name representing the running Java virtual machine.
            // It returns something like 6460@AURORA. Where the value
            // before the @ symbol is the PID.
            String jvmName = bean.getName();
    
            // Extract the PID by splitting the string returned by the
            // bean.getName() method.
            pid = Long.valueOf(jvmName.split("@")[0]);
        }

        return pid;
    }

    public static String getServerName() {
        if (servername == null) {
            try {
                servername = InetAddress.getLocalHost().getHostName();
            } catch(UnknownHostException e) {
                servername = "localhost";
            }
        }

        return servername;
    }

    public static String makeTelegrafMetricString(String stat, int index) {
        return String.format("%s,server=%s,pid=%d,worker=%d", stat, Utils.getServerName(), Utils.getPid(), index);
    }
}