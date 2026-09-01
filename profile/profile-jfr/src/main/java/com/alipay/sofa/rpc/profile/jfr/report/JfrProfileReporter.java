/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.profile.jfr.report;

import com.alipay.sofa.rpc.profile.jfr.JfrEventNames;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 读取一个或多个JFR文件，按Profile ID关联客户端和服务端RPC调用记录。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
public final class JfrProfileReporter {

    private static final int     DEFAULT_TOP   = 20;

    private static final Phase[] CLIENT_PHASES = {
                                               new Phase("first streaming response", "firstResponseTime"),
                                               new Phase("router", "routerTime"),
                                               new Phase("connection creation", "connectionTime"),
                                               new Phase("client filters", "clientFilterTime"),
                                               new Phase("load balancer", "loadBalancerTime"),
                                               new Phase("request serialization", "requestSerializationTime"),
                                               new Phase("response deserialization", "responseDeserializationTime") };

    private static final Phase[] SERVER_PHASES = {
                                               new Phase("request deserialization", "requestDeserializationTime"),
                                               new Phase("response serialization", "responseSerializationTime"),
                                               new Phase("business thread-pool wait", "threadPoolWaitTime"),
                                               new Phase("business invocation", "businessTime"),
                                               new Phase("server filters", "serverFilterTime"),
                                               new Phase("server network wait", "serverNetworkWaitTime") };

    private JfrProfileReporter() {
    }

    /**
     * 命令行入口。
     *
     * @param args {@code [--top N] recording.jfr [provider.jfr ...]}
     * @throws IOException JFR文件读取失败
     */
    public static void main(String[] args) throws IOException {
        Arguments arguments = parseArguments(args);
        if (arguments == null) {
            printUsage();
            return;
        }
        System.out.print(createReport(arguments.files, arguments.top));
    }

    /**
     * 生成文本报告。
     *
     * @param files JFR文件
     * @param top   最多展示的调用数
     * @return 文本报告
     * @throws IOException JFR文件读取失败
     */
    public static String createReport(List<Path> files, int top) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one JFR file is required");
        }
        if (top <= 0) {
            throw new IllegalArgumentException("top must be greater than zero");
        }

        validateFiles(files);

        CandidateSelector selector = new CandidateSelector(top);
        long eventCount = 0L;
        for (Path file : files) {
            eventCount += selectCandidates(file, selector);
        }

        Set<String> selectedProfileIds = selector.profileIds();
        Map<String, InvocationGroup> groups = new LinkedHashMap<String, InvocationGroup>();
        for (Path file : files) {
            readSelected(file, selectedProfileIds, groups);
        }
        List<InvocationGroup> sorted = new ArrayList<InvocationGroup>(groups.values());
        for (InvocationGroup group : sorted) {
            group.sortEvents();
        }
        Collections.sort(sorted, new Comparator<InvocationGroup>() {
            @Override
            public int compare(InvocationGroup left, InvocationGroup right) {
                int duration = Long.compare(right.maxDuration(), left.maxDuration());
                return duration == 0 ? left.profileId.compareTo(right.profileId) : duration;
            }
        });
        return render(files.size(), eventCount, sorted);
    }

    private static void validateFiles(List<Path> files) throws IOException {
        for (Path file : files) {
            if (file == null || !Files.isRegularFile(file)) {
                throw new IOException("JFR file does not exist: " + file);
            }
        }
    }

    private static long selectCandidates(Path file, CandidateSelector selector) throws IOException {
        long eventCount = 0L;
        RecordingFile recording = new RecordingFile(file);
        try {
            while (recording.hasMoreEvents()) {
                RecordedEvent event = recording.readEvent();
                String eventName = event.getEventType().getName();
                if (!isRpcEvent(eventName)) {
                    continue;
                }
                selector.offer(profileId(event, eventName), durationNanos(event));
                eventCount++;
            }
        } finally {
            recording.close();
        }
        return eventCount;
    }

    private static void readSelected(Path file, Set<String> selectedProfileIds,
                                     Map<String, InvocationGroup> groups) throws IOException {
        if (selectedProfileIds.isEmpty()) {
            return;
        }
        RecordingFile recording = new RecordingFile(file);
        try {
            while (recording.hasMoreEvents()) {
                RecordedEvent event = recording.readEvent();
                String eventName = event.getEventType().getName();
                if (!isRpcEvent(eventName)) {
                    continue;
                }
                String profileId = profileId(event, eventName);
                if (!selectedProfileIds.contains(profileId)) {
                    continue;
                }
                boolean client = JfrEventNames.CLIENT_INVOCATION.equals(eventName);
                ProfileEvent profileEvent = ProfileEvent.from(event, client ? CLIENT_PHASES : SERVER_PHASES);
                InvocationGroup group = groups.get(profileId);
                if (group == null) {
                    group = new InvocationGroup(profileId);
                    groups.put(profileId, group);
                }
                if (client) {
                    group.clients.add(profileEvent);
                } else {
                    group.servers.add(profileEvent);
                }
            }
        } finally {
            recording.close();
        }
    }

    private static boolean isRpcEvent(String eventName) {
        return JfrEventNames.CLIENT_INVOCATION.equals(eventName) ||
            JfrEventNames.SERVER_INVOCATION.equals(eventName);
    }

    private static String profileId(RecordedEvent event, String eventName) {
        String profileId = string(event, "profileId");
        return profileId.isEmpty() ? eventName + "@" + event.getStartTime() : profileId;
    }

    private static String render(int fileCount, long eventCount, List<InvocationGroup> groups) {
        StringBuilder report = new StringBuilder(4096);
        report.append("SOFA RPC JFR Profile Report\n");
        report.append("JFR files: ").append(fileCount)
            .append(", RPC events scanned: ").append(eventCount)
            .append(", invocation groups: ").append(groups.size()).append(" selected")
            .append(", correlated client/server: ").append(correlatedCount(groups)).append("\n");
        if (groups.isEmpty()) {
            report.append("No SOFA RPC profile events found.\n");
            return report.toString();
        }

        for (int i = 0; i < groups.size(); i++) {
            InvocationGroup group = groups.get(i);
            ProfileEvent sample = group.sample();
            report.append('\n').append('#').append(i + 1).append(" profileId=").append(group.profileId).append('\n');
            report.append("  service=").append(sample.service)
                .append('#').append(sample.method).append('\n');
            appendSide(report, "client", group.clients);
            appendSide(report, "server", group.servers);
            appendBottleneck(report, group);
        }
        return report.toString();
    }

    private static int correlatedCount(List<InvocationGroup> groups) {
        int count = 0;
        for (InvocationGroup group : groups) {
            if (!group.clients.isEmpty() && !group.servers.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void appendSide(StringBuilder report, String side, List<ProfileEvent> events) {
        if (events.isEmpty()) {
            report.append("  ").append(side).append(": not recorded\n");
            return;
        }
        for (int i = 0; i < events.size(); i++) {
            ProfileEvent event = events.get(i);
            long duration = event.duration;
            report.append("  ").append(side);
            if (events.size() > 1) {
                report.append('[').append(i + 1).append(']');
            }
            report.append(": ").append(formatDuration(duration))
                .append(' ').append(event.result)
                .append("  ").append(event.sourceApp)
                .append(" -> ").append(event.targetApp);
            if (!event.protocol.isEmpty() || !event.invokeType.isEmpty()) {
                report.append("  [").append(event.protocol).append('/').append(event.invokeType).append(']');
            }
            report.append('\n');
            report.append("    time=").append(event.startTime)
                .append(", address=").append(event.localAddress)
                .append(" -> ").append(event.remoteAddress).append('\n');
            if (!event.errorClass.isEmpty()) {
                report.append("    error=").append(event.errorClass);
                if (event.errorCode >= 0) {
                    report.append(", sofaErrorCode=").append(event.errorCode);
                }
                report.append('\n');
            }
            appendDataSize(report, event);
            for (PhaseValue phase : event.phases) {
                report.append("    ").append(pad(phase.label, 29))
                    .append(' ').append(bar(phase.duration, duration))
                    .append(' ').append(formatDuration(phase.duration))
                    .append(' ').append(formatPercent(phase.duration, duration)).append('\n');
            }
        }
    }

    private static void appendBottleneck(StringBuilder report, InvocationGroup group) {
        Bottleneck bottleneck = null;
        for (ProfileEvent event : group.clients) {
            bottleneck = max(bottleneck, "client", event);
        }
        for (ProfileEvent event : group.servers) {
            bottleneck = max(bottleneck, "server", event);
        }
        if (bottleneck != null) {
            report.append("  largest recorded phase: ").append(bottleneck.side).append('.')
                .append(bottleneck.label).append(" = ").append(formatDuration(bottleneck.duration)).append('\n');
        }
    }

    private static Bottleneck max(Bottleneck current, String side, ProfileEvent event) {
        for (PhaseValue phase : event.phases) {
            if (current == null || phase.duration > current.duration) {
                current = new Bottleneck(side, phase.label, phase.duration);
            }
        }
        return current;
    }

    private static void appendDataSize(StringBuilder report, ProfileEvent event) {
        if (event.invokeCount <= 1 && event.requestSize < 0 && event.responseSize < 0) {
            return;
        }
        report.append("    data=");
        if (event.invokeCount > 1) {
            report.append("invokeCount:").append(event.invokeCount).append(' ');
        }
        if (event.requestSize >= 0) {
            report.append("request:").append(event.requestSize).append(" B ");
        }
        if (event.responseSize >= 0) {
            report.append("response:").append(event.responseSize).append(" B");
        }
        report.append('\n');
    }

    private static String string(RecordedEvent event, String field) {
        if (event == null || !event.hasField(field)) {
            return "";
        }
        Object value = event.getValue(field);
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(RecordedEvent event, String field) {
        if (!event.hasField(field)) {
            return -1L;
        }
        Object value = event.getValue(field);
        return value instanceof Number ? ((Number) value).longValue() : -1L;
    }

    private static long durationNanos(RecordedEvent event) {
        Duration duration = event.getDuration();
        return duration == null ? 0L : duration.toNanos();
    }

    private static String formatDuration(long nanos) {
        if (nanos >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.3f s", nanos / 1_000_000_000D);
        }
        if (nanos >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.3f ms", nanos / 1_000_000D);
        }
        if (nanos >= 1_000L) {
            return String.format(Locale.ROOT, "%.3f us", nanos / 1_000D);
        }
        return nanos + " ns";
    }

    private static String formatPercent(long value, long total) {
        if (total <= 0) {
            return "(n/a)";
        }
        return String.format(Locale.ROOT, "(%5.1f%%)", value * 100D / total);
    }

    private static String bar(long value, long total) {
        int width = 12;
        int filled = total <= 0 ? 0 : (int) Math.round(Math.min(1D, value / (double) total) * width);
        StringBuilder bar = new StringBuilder(width + 2);
        bar.append('[');
        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        return bar.append(']').toString();
    }

    private static String pad(String value, int width) {
        StringBuilder padded = new StringBuilder(value);
        while (padded.length() < width) {
            padded.append(' ');
        }
        return padded.toString();
    }

    private static Arguments parseArguments(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        int top = DEFAULT_TOP;
        List<Path> files = new ArrayList<Path>();
        for (int i = 0; i < args.length; i++) {
            if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                return null;
            }
            if ("--top".equals(args[i])) {
                if (++i >= args.length) {
                    return null;
                }
                try {
                    top = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                files.add(Paths.get(args[i]));
            }
        }
        return files.isEmpty() || top <= 0 ? null : new Arguments(files, top);
    }

    private static void printUsage() {
        System.out.println("Usage: JfrProfileReporter [--top N] recording.jfr [provider.jfr ...]");
    }

    /**
     * 第一遍扫描只保留耗时最大的Top-N个Profile ID，避免报告器按录制时长无限占用内存。
     */
    private static final class CandidateSelector {

        private final int                    limit;

        private final Map<String, Candidate> candidates = new HashMap<String, Candidate>();

        private final TreeSet<Candidate>     ordered    = new TreeSet<Candidate>();

        private CandidateSelector(int limit) {
            this.limit = limit;
        }

        private void offer(String profileId, long duration) {
            Candidate existing = candidates.get(profileId);
            if (existing != null) {
                if (duration > existing.duration) {
                    ordered.remove(existing);
                    existing.duration = duration;
                    ordered.add(existing);
                }
                return;
            }

            Candidate candidate = new Candidate(profileId, duration);
            if (candidates.size() < limit) {
                add(candidate);
                return;
            }

            Candidate worst = ordered.first();
            if (candidate.compareTo(worst) > 0) {
                ordered.remove(worst);
                candidates.remove(worst.profileId);
                add(candidate);
            }
        }

        private void add(Candidate candidate) {
            candidates.put(candidate.profileId, candidate);
            ordered.add(candidate);
        }

        private Set<String> profileIds() {
            return new HashSet<String>(candidates.keySet());
        }
    }

    /**
     * 耗时越大越优先；耗时相同时使用Profile ID保证选择结果稳定。
     */
    private static final class Candidate implements Comparable<Candidate> {

        private final String profileId;

        private long         duration;

        private Candidate(String profileId, long duration) {
            this.profileId = profileId;
            this.duration = duration;
        }

        @Override
        public int compareTo(Candidate other) {
            int durationOrder = Long.compare(duration, other.duration);
            if (durationOrder != 0) {
                return durationOrder;
            }
            // TreeSet.first()表示最差候选；同耗时时字典序更大的ID优先被淘汰。
            return other.profileId.compareTo(profileId);
        }
    }

    private static final class Arguments {

        private final List<Path> files;

        private final int        top;

        private Arguments(List<Path> files, int top) {
            this.files = files;
            this.top = top;
        }
    }

    private static final class Phase {

        private final String label;

        private final String field;

        private Phase(String label, String field) {
            this.label = label;
            this.field = field;
        }
    }

    private static final class PhaseValue {

        private final String label;

        private final long   duration;

        private PhaseValue(String label, long duration) {
            this.label = label;
            this.duration = duration;
        }
    }

    /**
     * RecordedEvent的部分JDK 8实现会惰性解析字符串常量池。因此必须在RecordingFile关闭前
     * 复制所有字段，同时也避免长时间保留JFR解析器的内部对象。
     */
    private static final class ProfileEvent {

        private final String           service;

        private final String           method;

        private final String           sourceApp;

        private final String           targetApp;

        private final String           protocol;

        private final String           invokeType;

        private final String           localAddress;

        private final String           remoteAddress;

        private final String           result;

        private final String           errorClass;

        private final Instant          startTime;

        private final long             duration;

        private final long             errorCode;

        private final long             invokeCount;

        private final long             requestSize;

        private final long             responseSize;

        private final List<PhaseValue> phases;

        private ProfileEvent(RecordedEvent event, Phase[] phaseDefinitions) {
            service = string(event, "service");
            method = string(event, "method");
            sourceApp = string(event, "sourceApp");
            targetApp = string(event, "targetApp");
            protocol = string(event, "protocol");
            invokeType = string(event, "invokeType");
            localAddress = string(event, "localAddress");
            remoteAddress = string(event, "remoteAddress");
            result = string(event, "result");
            errorClass = string(event, "errorClass");
            startTime = event.getStartTime();
            duration = durationNanos(event);
            errorCode = number(event, "errorCode");
            invokeCount = number(event, "invokeCount");
            requestSize = number(event, "requestSize");
            responseSize = number(event, "responseSize");
            phases = new ArrayList<PhaseValue>(phaseDefinitions.length);
            for (Phase phase : phaseDefinitions) {
                long phaseDuration = number(event, phase.field);
                if (phaseDuration >= 0) {
                    phases.add(new PhaseValue(phase.label, phaseDuration));
                }
            }
        }

        private static ProfileEvent from(RecordedEvent event, Phase[] phaseDefinitions) {
            return new ProfileEvent(event, phaseDefinitions);
        }
    }

    private static final class Bottleneck {

        private final String side;

        private final String label;

        private final long   duration;

        private Bottleneck(String side, String label, long duration) {
            this.side = side;
            this.label = label;
            this.duration = duration;
        }
    }

    private static final class InvocationGroup {

        private final String             profileId;

        private final List<ProfileEvent> clients = new ArrayList<ProfileEvent>();

        private final List<ProfileEvent> servers = new ArrayList<ProfileEvent>();

        private InvocationGroup(String profileId) {
            this.profileId = profileId;
        }

        private ProfileEvent sample() {
            return clients.isEmpty() ? servers.get(0) : clients.get(0);
        }

        private long maxDuration() {
            long max = 0L;
            for (ProfileEvent event : clients) {
                max = Math.max(max, event.duration);
            }
            for (ProfileEvent event : servers) {
                max = Math.max(max, event.duration);
            }
            return max;
        }

        private void sortEvents() {
            Comparator<ProfileEvent> comparator = new Comparator<ProfileEvent>() {
                @Override
                public int compare(ProfileEvent left, ProfileEvent right) {
                    return left.startTime.compareTo(right.startTime);
                }
            };
            Collections.sort(clients, comparator);
            Collections.sort(servers, comparator);
        }
    }
}
