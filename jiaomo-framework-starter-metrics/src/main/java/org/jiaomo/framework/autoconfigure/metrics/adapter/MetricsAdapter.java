/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jiaomo.framework.autoconfigure.metrics.adapter;

import com.codahale.metrics.*;
import org.jiaomo.framework.autoconfigure.metrics.reporter.MetricReporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import javax.annotation.PostConstruct;

/**
 * @author 唐燕云 tangyanyun
 * @email tangyanyun@sina.com
 * @date 2023-08-26
 */

@Slf4j
public class MetricsAdapter {
    @Getter
    private static final MetricRegistry metricRegistry = new MetricRegistry();

    @Getter
    private static volatile boolean metricsEnabled = Boolean.parseBoolean(System.getProperty("metrics.enabled"));
    private static String reportOutputPath;
    private static String reportOutputFile;
    private static long reportDelayMills;
    private static long reportPeriodMills;

    @Value("${metrics.enabled:false}")
    public void setMetricsEnabled(boolean enabled) {
        metricsEnabled = enabled;
    }
    @Value("${metrics.report.output-path:.}")
    public void setReportOutputPath(String path) {
        reportOutputPath = path;
    }
    @Value("${metrics.report.output-file:metrics.log}")
    public void setReportOutputFile(String file) {
        reportOutputFile = file;
    }
    @Value("${metrics.report.delay-mills:120000}")
    public void setReportDelayMills(long delayMills) {
        reportDelayMills = delayMills;
    }
    @Value("${metrics.report.period-mills:120000}")
    public void setReportPeriodMills(long periodMills) {
        reportPeriodMills = periodMills;
    }

    @PostConstruct
    public void init() {
        if (isMetricsEnabled()) {
            new java.util.Timer().schedule(new java.util.TimerTask() {
                public void run() {
                    try {
                        MetricsAdapter.report();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, reportDelayMills, reportPeriodMills);
        }
    }

    public static Meter meter(String name) {
        return getMetricRegistry().meter(name);
    }
    public static void mark(String name) {
        if (isMetricsEnabled())
            getMetricRegistry().meter(name).mark();
    }

    public static Histogram histogram(String name) {
        return getMetricRegistry().histogram(name);
    }
    public static void update(String name,int value) {
        if (isMetricsEnabled())
            getMetricRegistry().histogram(name).update(value);
    }
    public static void update(String name,long value) {
        if (isMetricsEnabled())
            getMetricRegistry().histogram(name).update(value);
    }

    public static Counter counter(String name) {
        return getMetricRegistry().counter(name);
    }

    public static Timer timer(String name) {
        return getMetricRegistry().timer(name);
    }
    public static Timer.Context time(String name) {
        return isMetricsEnabled() ? getMetricRegistry().timer(name).time() : null;
    }
    public static Timer.Context time(Timer timer) {
        return timer == null ? null : timer.time();
    }
    public static long stop(Timer.Context timerContext) {
        return timerContext == null ? 0L : timerContext.stop();
    }

    public static void report(OutputStream outputStream) {
        if (isMetricsEnabled()) {
            try (final PrintStream out = new PrintStream(outputStream)) {
                MetricReporter.forRegistry(getMetricRegistry())
                        .outputTo(out)
                        .build()
                        .report();
            }
        }
    }
    public static void report() throws IOException {
        if (isMetricsEnabled())
            report(new FileOutputStream(obtainOutputFile(reportOutputPath,reportOutputFile),true));
    }

    private static File obtainOutputFile(final String path, final String baseFileName) throws IOException {
        makeDir(path);
        final String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
        final String fileName = baseFileName + "." + now;
        final File file = Paths.get(path, fileName).toFile();
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Fail to create file: " + file);
        }
        return file;
    }

    private static void makeDir(final String path) throws IOException {
        final File dir = Paths.get(path).toFile().getAbsoluteFile();
        if (dir.exists()) {
            assert dir.isDirectory() : String.format("[%s] is not directory.", path);
        } else {
            FileUtils.forceMkdir(dir);
        }
    }
}
