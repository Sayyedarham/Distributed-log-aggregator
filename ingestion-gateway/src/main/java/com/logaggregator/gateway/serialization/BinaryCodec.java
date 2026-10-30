package com.logaggregator.gateway.serialization;

import com.logaggregator.gateway.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

public class BinaryCodec {

    public static final byte MSG_METRIC_PAYLOAD = 1;
    public static final byte MSG_METRIC_BATCH = 2;
    public static final byte MSG_SERVER_ACK = 3;
    public static final byte MSG_AGENT_REGISTRATION = 4;
    public static final byte MSG_REGISTRATION_RESPONSE = 5;

    public static byte getMessageType(byte[] data) {
        if (data == null || data.length < 1) return 0;
        return data[0];
    }

    public static AgentRegistration decodeRegistration(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte msgType = buf.get();
        if (msgType != MSG_AGENT_REGISTRATION) {
            throw new IllegalArgumentException("Expected MSG_AGENT_REGISTRATION (4), got " + msgType);
        }

        AgentInfo info = decodeAgentInfo(buf);
        long regTime = buf.getLong();
        int reqMetricCount = buf.getInt();
        List<String> requested = new ArrayList<>(reqMetricCount);
        for (int i = 0; i < reqMetricCount; i++) {
            requested.add(readString(buf));
        }
        int intervalMs = buf.getInt();

        AgentRegistration reg = new AgentRegistration(info, regTime, requested, intervalMs);
        return reg;
    }

    public static byte[] encodeRegistrationResponse(RegistrationResponse resp) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.put(MSG_REGISTRATION_RESPONSE);
        buf.put((byte) (resp.isAccepted() ? 1 : 0));
        writeString(buf, resp.getAssignedAgentId());
        buf.putInt(resp.getCollectionIntervalMs());

        List<String> metrics = resp.getCollectMetrics();
        buf.putInt(metrics != null ? metrics.size() : 0);
        if (metrics != null) {
            for (String m : metrics) {
                writeString(buf, m);
            }
        }

        buf.flip();
        byte[] out = new byte[buf.remaining()];
        buf.get(out);
        return out;
    }

    public static byte[] encodeServerAck(ServerAck ack) {
        ByteBuffer buf = ByteBuffer.allocate(512);
        buf.put(MSG_SERVER_ACK);
        writeString(buf, ack.getBatchId());
        buf.put((byte) (ack.isSuccess() ? 1 : 0));
        writeString(buf, ack.getErrorMessage());
        buf.putLong(ack.getServerTimestamp());
        buf.putInt(ack.getNextIntervalMs());

        buf.flip();
        byte[] out = new byte[buf.remaining()];
        buf.get(out);
        return out;
    }

    public static MetricBatch decodeBatch(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte msgType = buf.get();
        if (msgType != MSG_METRIC_BATCH) {
            throw new IllegalArgumentException("Expected MSG_METRIC_BATCH (2), got " + msgType);
        }

        String batchId = readString(buf);
        long timestamp = buf.getLong();
        int payloadCount = buf.getInt();
        boolean compressed = buf.get() != 0;
        int bodyLength = buf.getInt();

        byte[] bodyBytes = new byte[bodyLength];
        buf.get(bodyBytes);

        if (compressed) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);
                InflaterInputStream iis = new InflaterInputStream(bais);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] temp = new byte[4096];
                int read;
                while ((read = iis.read(temp)) != -1) {
                    baos.write(temp, 0, read);
                }
                bodyBytes = baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Decompression failed for batch " + batchId, e);
            }
        }

        ByteBuffer bodyBuf = ByteBuffer.wrap(bodyBytes);
        List<MetricPayload> payloads = new ArrayList<>(payloadCount);
        String agentId = "unknown";

        while (bodyBuf.hasRemaining()) {
            int pLen = bodyBuf.getInt();
            byte[] pBytes = new byte[pLen];
            bodyBuf.get(pBytes);

            MetricPayload payload = decodePayload(pBytes);
            if (payload != null) {
                payloads.add(payload);
                if (payload.getAgentInfo() != null) {
                    agentId = payload.getAgentInfo().getAgentId();
                }
            }
        }

        return new MetricBatch(batchId, agentId, timestamp, payloads);
    }

    public static MetricPayload decodePayload(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte msgType = buf.get();
        if (msgType != MSG_METRIC_PAYLOAD) {
            throw new IllegalArgumentException("Expected MSG_METRIC_PAYLOAD (1), got " + msgType);
        }

        AgentInfo agentInfo = decodeAgentInfo(buf);
        long timestamp = buf.getLong();
        long seqNum = buf.getLong();
        int priority = buf.getInt();
        boolean isRecovery = buf.get() != 0;

        MetricPayload payload = new MetricPayload();
        payload.setAgentInfo(agentInfo);
        payload.setTimestamp(timestamp);
        payload.setSequenceNumber(seqNum);
        payload.setPriority(priority);

        boolean hasCpu = buf.get() != 0;
        if (hasCpu) {
            CpuMetrics cpu = new CpuMetrics();
            cpu.setUserPercent(buf.getDouble());
            cpu.setSystemPercent(buf.getDouble());
            cpu.setIdlePercent(buf.getDouble());
            cpu.setIowaitPercent(buf.getDouble());
            cpu.setStealPercent(buf.getDouble());
            cpu.setCoreCount(buf.getInt());
            cpu.setLoadAvg1m(buf.getDouble());
            cpu.setLoadAvg5m(buf.getDouble());
            cpu.setLoadAvg15m(buf.getDouble());

            int perCoreCount = buf.getInt();
            List<CpuMetrics.PerCoreUsage> perCore = new ArrayList<>(perCoreCount);
            for (int i = 0; i < perCoreCount; i++) {
                int coreId = buf.getInt();
                double usage = buf.getDouble();
                perCore.add(new CpuMetrics.PerCoreUsage(coreId, usage));
            }
            cpu.setPerCoreUsage(perCore);
            payload.setCpuMetrics(cpu);
        }

        boolean hasMem = buf.get() != 0;
        if (hasMem) {
            MemoryMetrics mem = new MemoryMetrics();
            mem.setTotalBytes(buf.getLong());
            mem.setAvailableBytes(buf.getLong());
            mem.setUsedBytes(buf.getLong());
            mem.setUsedPercent(buf.getDouble());
            mem.setSwapTotalBytes(buf.getLong());
            mem.setSwapUsedBytes(buf.getLong());
            mem.setBuffersBytes(buf.getLong());
            mem.setCachedBytes(buf.getLong());
            payload.setMemoryMetrics(mem);
        }

        boolean hasDisk = buf.get() != 0;
        if (hasDisk) {
            DiskMetrics disk = new DiskMetrics();
            int devCount = buf.getInt();
            List<DiskMetrics.DiskDevice> devices = new ArrayList<>(devCount);
            for (int i = 0; i < devCount; i++) {
                DiskMetrics.DiskDevice dev = new DiskMetrics.DiskDevice();
                dev.setDeviceName(readString(buf));
                dev.setReadsCompleted(buf.getLong());
                dev.setWritesCompleted(buf.getLong());
                dev.setReadBytes(buf.getLong());
                dev.setWriteBytes(buf.getLong());
                dev.setReadRateBps(buf.getDouble());
                dev.setWriteRateBps(buf.getDouble());
                dev.setUtilizationPercent(buf.getDouble());
                dev.setTotalBytes(buf.getLong());
                dev.setUsedBytes(buf.getLong());
                dev.setUsedPercent(buf.getDouble());
                devices.add(dev);
            }
            disk.setDevices(devices);
            payload.setDiskMetrics(disk);
        }

        boolean hasNet = buf.get() != 0;
        if (hasNet) {
            NetworkMetrics net = new NetworkMetrics();
            int ifaceCount = buf.getInt();
            List<NetworkMetrics.NetworkInterface> ifaces = new ArrayList<>(ifaceCount);
            for (int i = 0; i < ifaceCount; i++) {
                NetworkMetrics.NetworkInterface iface = new NetworkMetrics.NetworkInterface();
                iface.setName(readString(buf));
                iface.setBytesSent(buf.getLong());
                iface.setBytesRecv(buf.getLong());
                iface.setPacketsSent(buf.getLong());
                iface.setPacketsRecv(buf.getLong());
                iface.setErrorsIn(buf.getLong());
                iface.setErrorsOut(buf.getLong());
                iface.setSendRateBps(buf.getDouble());
                iface.setRecvRateBps(buf.getDouble());
                ifaces.add(iface);
            }
            net.setInterfaces(ifaces);
            payload.setNetworkMetrics(net);
        }

        int logCount = buf.getInt();
        List<LogEntry> logs = new ArrayList<>(logCount);
        for (int i = 0; i < logCount; i++) {
            LogEntry log = new LogEntry();
            log.setSource(readString(buf));
            log.setLevel(readString(buf));
            log.setMessage(readString(buf));
            log.setTimestamp(buf.getLong());
            int tagCount = buf.getInt();
            Map<String, String> tags = new HashMap<>();
            for (int t = 0; t < tagCount; t++) {
                tags.put(readString(buf), readString(buf));
            }
            log.setTags(tags);
            logs.add(log);
        }
        payload.setLogEntries(logs);

        return payload;
    }

    private static AgentInfo decodeAgentInfo(ByteBuffer buf) {
        AgentInfo info = new AgentInfo();
        info.setAgentId(readString(buf));
        info.setHostname(readString(buf));
        info.setIpAddress(readString(buf));
        info.setOsType(readString(buf));
        info.setOsVersion(readString(buf));
        info.setBootTime(buf.getLong());
        info.setAgentVersion(readString(buf));
        return info;
    }

    private static String readString(ByteBuffer buf) {
        int len = buf.getInt();
        if (len < 0) return null;
        if (len == 0) return "";
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteBuffer buf, String str) {
        if (str == null) {
            buf.putInt(-1);
            return;
        }
        byte[] b = str.getBytes(StandardCharsets.UTF_8);
        buf.putInt(b.length);
        buf.put(b);
    }
}

