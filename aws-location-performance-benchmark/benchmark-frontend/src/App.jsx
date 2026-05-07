import { useEffect, useState } from "react";
import { BrowserRouter, NavLink, Route, Routes } from "react-router-dom";
import { fetchMetricComparisons, fetchRecentMeasurements } from "./api/metricsApi";
import ComparisonTable from "./components/ComparisonTable";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer,
  Legend,
  BarChart,
  Bar,
  ReferenceLine,
} from "recharts";
import "./App.css";

const METRIC_LABELS = {
  HTTP_TTFB: "HTTP TTFB",
  TCP_LATENCY: "TCP Latency",
  WEBSOCKET_RTT: "WebSocket RTT",
  THROUGHPUT_DOWN: "Download Duration",
  THROUGHPUT_UP: "Upload Duration",
  JITTER: "Jitter",
  PACKET_LOSS: "Probe Failure Rate",
  DNS_RESOLUTION: "DNS Resolution",
};

const METRIC_UNITS = {
  HTTP_TTFB: "ms",
  TCP_LATENCY: "ms",
  WEBSOCKET_RTT: "ms",
  THROUGHPUT_DOWN: "ms",
  THROUGHPUT_UP: "ms",
  JITTER: "ms",
  PACKET_LOSS: "%",
  DNS_RESOLUTION: "ms",
};

const OVERVIEW_METRICS = [
  "HTTP_TTFB",
  "TCP_LATENCY",
  "WEBSOCKET_RTT",
  "THROUGHPUT_DOWN",
  "THROUGHPUT_UP",
  "JITTER",
];

function getMetricLabel(metricType) {
  return METRIC_LABELS[metricType] || metricType;
}

function getMetricUnit(metricType) {
  return METRIC_UNITS[metricType] || "ms";
}

function formatValue(value, metricType) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "N/A";
  }

  const unit = getMetricUnit(metricType);

  if (metricType === "PACKET_LOSS") {
    return `${Number(value).toFixed(3)} ${unit}`;
  }

  if (metricType === "DNS_RESOLUTION") {
    return `${Number(value).toFixed(4)} ${unit}`;
  }

  return `${Number(value).toFixed(2)} ${unit}`;
}

function buildChartData(data, metricType) {
  const BUCKET_SIZE_MS = 5 * 60 * 1000;

  const metrics = data
    .filter((item) => item.metricType === metricType)
    .filter((item) => item.success)
    .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));

  const buckets = {};

  metrics.forEach((item) => {
    const timestampMs = new Date(item.timestamp).getTime();
    const bucketStartMs = Math.floor(timestampMs / BUCKET_SIZE_MS) * BUCKET_SIZE_MS;

    const bucketTime = new Date(bucketStartMs).toLocaleString("tr-TR", {
      day: "2-digit",
      month: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });

    if (!buckets[bucketStartMs]) {
      buckets[bucketStartMs] = {
        time: bucketTime,
        frankfurtValues: [],
        istanbulValues: [],
      };
    }

    if (item.targetRegion === "frankfurt") {
      buckets[bucketStartMs].frankfurtValues.push(item.valueMs);
    }

    if (item.targetRegion === "istanbul") {
      buckets[bucketStartMs].istanbulValues.push(item.valueMs);
    }
  });

  return Object.entries(buckets)
    .sort(([a], [b]) => Number(a) - Number(b))
    .map(([, bucket]) => ({
      time: bucket.time,
      frankfurt:
        bucket.frankfurtValues.length > 0
          ? bucket.frankfurtValues.reduce((sum, value) => sum + value, 0) /
            bucket.frankfurtValues.length
          : null,
      istanbul:
        bucket.istanbulValues.length > 0
          ? bucket.istanbulValues.reduce((sum, value) => sum + value, 0) /
            bucket.istanbulValues.length
          : null,
    }));
}

function buildDeltaChartData(chartData) {
  return chartData
    .filter((item) => item.frankfurt !== null && item.istanbul !== null)
    .map((item) => ({
      time: item.time,
      delta: item.istanbul - item.frankfurt,
    }));
}

function buildOverviewBarData(comparisons, valueType) {
  return comparisons
    .filter((item) => OVERVIEW_METRICS.includes(item.metricType))
    .map((item) => ({
      metric: getMetricLabel(item.metricType),
      frankfurt:
        valueType === "average"
          ? item.baselineAverageMs
          : item.baselineP95Ms,
      istanbul:
        valueType === "average"
          ? item.candidateAverageMs
          : item.candidateP95Ms,
      metricType: item.metricType,
    }));
}

function MetricLineChart({ title, description, data, metricType }) {
  const unit = getMetricUnit(metricType);

  return (
    <div className="chart-card">
      <div className="table-header">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>

      <div className="chart-wrapper">
        <ResponsiveContainer width="100%" height={360}>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis unit={` ${unit}`} />
            <Tooltip
              contentStyle={{
                backgroundColor: "#020617",
                border: "1px solid #1e293b",
                borderRadius: "12px",
                color: "#e5e7eb",
                boxShadow: "0 18px 40px rgba(0, 0, 0, 0.35)",
              }}
              labelStyle={{
                color: "#cbd5e1",
                fontWeight: 700,
                marginBottom: "6px",
              }}
              itemStyle={{
                fontWeight: 700,
              }}
              formatter={(value, name) => [
                formatValue(value, metricType),
                name,
              ]}
            />
            <Legend />
            <Line
              type="linear"
              dataKey="frankfurt"
              name="Frankfurt"
              stroke="#60a5fa"
              strokeWidth={3}
              dot={false}
              connectNulls={false}
            />
            <Line
              type="linear"
              dataKey="istanbul"
              name="Istanbul Local Zone"
              stroke="#22c55e"
              strokeWidth={3}
              dot={false}
              connectNulls={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

function DeltaLineChart({ title, description, data, metricType }) {
  const unit = getMetricUnit(metricType);

  return (
    <div className="chart-card">
      <div className="table-header">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>

      <div className="chart-wrapper">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis unit={` ${unit}`} />
            <ReferenceLine
              y={0}
              stroke="#94a3b8"
              strokeDasharray="4 4"
              label={{
                value: "Equal",
                position: "insideTopLeft",
                fill: "#94a3b8",
                fontSize: 12,
              }}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: "#020617",
                border: "1px solid #1e293b",
                borderRadius: "12px",
                color: "#e5e7eb",
                boxShadow: "0 18px 40px rgba(0, 0, 0, 0.35)",
              }}
              labelStyle={{
                color: "#cbd5e1",
                fontWeight: 700,
                marginBottom: "6px",
              }}
              itemStyle={{
                fontWeight: 700,
              }}
              formatter={(value) => {
                const numericValue = Number(value);
                const direction =
                  numericValue > 0
                    ? "Istanbul slower"
                    : numericValue < 0
                      ? "Istanbul faster"
                      : "Equal";

                return [
                  `${formatValue(numericValue, metricType)} (${direction})`,
                  "Istanbul - Frankfurt",
                ];
              }}
            />
            <Legend />
            <Line
              type="linear"
              dataKey="delta"
              name="Istanbul - Frankfurt"
              stroke="#f97316"
              strokeWidth={3}
              dot={false}
              connectNulls={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

function ComparisonBarChart({ title, description, data }) {
  return (
    <div className="chart-card">
      <div className="table-header">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>

      <div className="chart-wrapper">
        <ResponsiveContainer width="100%" height={360}>
          <BarChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="metric" />
            <YAxis unit=" ms" />
            <Tooltip
              contentStyle={{
                backgroundColor: "#020617",
                border: "1px solid #1e293b",
                borderRadius: "12px",
                color: "#e5e7eb",
                boxShadow: "0 18px 40px rgba(0, 0, 0, 0.35)",
              }}
              labelStyle={{
                color: "#cbd5e1",
                fontWeight: 700,
                marginBottom: "6px",
              }}
              formatter={(value, name, props) => [
                formatValue(value, props.payload.metricType),
                name,
              ]}
            />
            <Legend />
            <Bar dataKey="frankfurt" name="Frankfurt" fill="#60a5fa" />
            <Bar dataKey="istanbul" name="Istanbul Local Zone" fill="#22c55e" />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

function OverviewPage() {
  const [comparisons, setComparisons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const averageData = buildOverviewBarData(comparisons, "average");
  const p95Data = buildOverviewBarData(comparisons, "p95");

  useEffect(() => {
    fetchMetricComparisons()
      .then((data) => {
        setComparisons(data.filter((item) => item.metricType !== "TLS_HANDSHAKE"));
        setError(null);
      })
      .catch((err) => {
        setError(err.message);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">AWS latency benchmark</p>
        <h1>Overview</h1>
        <p className="page-description">
          A Turkey-based benchmark agent compares AWS Frankfurt with AWS Istanbul Local Zone across
          HTTP, TCP, WebSocket, transfer-duration, jitter, DNS, and reliability metrics.
        </p>
      </div>

      {loading && <div className="empty-state">Loading comparison data...</div>}
      {error && <div className="error-state">{error}</div>}

      {!loading && !error && (
        <>
          <ComparisonTable comparisons={comparisons} />

          <ComparisonBarChart
            title="Average latency and transfer-duration comparison"
            description="Lower values are better. This chart excludes DNS, probe failure rate, and TLS for clearer application-level comparison."
            data={averageData}
          />

          <ComparisonBarChart
            title="P95 latency and transfer-duration comparison"
            description="P95 highlights tail latency. Lower values indicate more consistent high-percentile performance."
            data={p95Data}
          />
        </>
      )}
    </section>
  );
}

function HttpLatencyPage() {
  const [chartData, setChartData] = useState([]);
  const deltaData = buildDeltaChartData(chartData);

  useEffect(() => {
    fetchRecentMeasurements()
      .then((data) => {
        setChartData(buildChartData(data, "HTTP_TTFB"));
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">HTTP latency</p>
        <h1>HTTP TTFB</h1>
        <p className="page-description">
          48-hour HTTP time-to-first-byte trend, grouped into 5-minute averages. Lower values are
          better.
        </p>
      </div>

      <MetricLineChart
        title="HTTP TTFB over time"
        description="Time-to-first-byte trend for Frankfurt and Istanbul Local Zone targets."
        data={chartData}
        metricType="HTTP_TTFB"
      />

      <DeltaLineChart
        title="HTTP TTFB latency gap over time"
        description="Istanbul minus Frankfurt. Values above zero mean Istanbul was slower; values below zero mean Istanbul was faster."
        data={deltaData}
        metricType="HTTP_TTFB"
      />
    </section>
  );
}

function TcpPage() {
  const [tcpData, setTcpData] = useState([]);
  const deltaData = buildDeltaChartData(tcpData);

  useEffect(() => {
    fetchRecentMeasurements()
      .then((data) => {
        setTcpData(buildChartData(data, "TCP_LATENCY"));
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">TCP latency</p>
        <h1>TCP Latency</h1>
        <p className="page-description">
          48-hour TCP connection latency trend, grouped into 5-minute averages. Lower values are
          better.
        </p>
      </div>

      <MetricLineChart
        title="TCP Latency over time"
        description="TCP connection latency trend for Frankfurt and Istanbul Local Zone targets."
        data={tcpData}
        metricType="TCP_LATENCY"
      />

      <DeltaLineChart
        title="TCP latency gap over time"
        description="Istanbul minus Frankfurt. Values above zero mean Istanbul was slower; values below zero mean Istanbul was faster."
        data={deltaData}
        metricType="TCP_LATENCY"
      />
    </section>
  );
}

function WebSocketPage() {
  const [webSocketData, setWebSocketData] = useState([]);
  const deltaData = buildDeltaChartData(webSocketData);

  useEffect(() => {
    fetchRecentMeasurements()
      .then((data) => {
        setWebSocketData(buildChartData(data, "WEBSOCKET_RTT"));
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">WebSocket</p>
        <h1>WebSocket RTT</h1>
        <p className="page-description">
          48-hour WebSocket round-trip-time trend, grouped into 5-minute averages. Lower values are
          better for real-time applications.
        </p>
      </div>

      <MetricLineChart
        title="WebSocket RTT over time"
        description="Round-trip time for WebSocket message exchange between the agent and target servers."
        data={webSocketData}
        metricType="WEBSOCKET_RTT"
      />

      <DeltaLineChart
        title="WebSocket RTT gap over time"
        description="Istanbul minus Frankfurt. Values above zero mean Istanbul was slower; values below zero mean Istanbul was faster."
        data={deltaData}
        metricType="WEBSOCKET_RTT"
      />
    </section>
  );
}

function TransferPage() {
  const [downloadData, setDownloadData] = useState([]);
  const [uploadData, setUploadData] = useState([]);

  const downloadDeltaData = buildDeltaChartData(downloadData);
  const uploadDeltaData = buildDeltaChartData(uploadData);

  useEffect(() => {
    fetchRecentMeasurements()
      .then((data) => {
        setDownloadData(buildChartData(data, "THROUGHPUT_DOWN"));
        setUploadData(buildChartData(data, "THROUGHPUT_UP"));
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">Transfer duration</p>
        <h1>Upload and Download Duration</h1>
        <p className="page-description">
          48-hour upload and download duration trends, grouped into 5-minute averages. Lower duration
          means faster transfer.
        </p>
      </div>

      <MetricLineChart
        title="Download duration over time"
        description="Time needed to download the test payload from each target. Lower values are better."
        data={downloadData}
        metricType="THROUGHPUT_DOWN"
      />

      <DeltaLineChart
        title="Download duration gap over time"
        description="Istanbul minus Frankfurt. Values above zero mean Istanbul downloads were slower; values below zero mean Istanbul downloads were faster."
        data={downloadDeltaData}
        metricType="THROUGHPUT_DOWN"
      />

      <MetricLineChart
        title="Upload duration over time"
        description="Time needed to upload the test payload to each target. Lower values are better."
        data={uploadData}
        metricType="THROUGHPUT_UP"
      />

      <DeltaLineChart
        title="Upload duration gap over time"
        description="Istanbul minus Frankfurt. Values above zero mean Istanbul uploads were slower; values below zero mean Istanbul uploads were faster."
        data={uploadDeltaData}
        metricType="THROUGHPUT_UP"
      />
    </section>
  );
}

function StabilityPage() {
  const [jitterData, setJitterData] = useState([]);
  const [probeFailureData, setProbeFailureData] = useState([]);

  const jitterDeltaData = buildDeltaChartData(jitterData);

  useEffect(() => {
    fetchRecentMeasurements()
      .then((data) => {
        setJitterData(buildChartData(data, "JITTER"));
        setProbeFailureData(buildChartData(data, "PACKET_LOSS"));
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">Stability</p>
        <h1>Jitter and Probe Failure Rate</h1>
        <p className="page-description">
          Jitter shows latency variation over time. Probe failure rate represents failed
          application-level probes, not raw TCP packet loss. Lower values are better.
        </p>
      </div>

      <MetricLineChart
        title="Jitter over time"
        description="Lower jitter means more stable network behavior."
        data={jitterData}
        metricType="JITTER"
      />

      <DeltaLineChart
        title="Jitter gap over time"
        description="Istanbul minus Frankfurt. Values below zero mean Istanbul had lower jitter."
        data={jitterDeltaData}
        metricType="JITTER"
      />

      <MetricLineChart
        title="Probe failure rate over time"
        description="Failed application-level probe percentage for Frankfurt and Istanbul Local Zone targets. This does not measure raw TCP packet loss."
        data={probeFailureData}
        metricType="PACKET_LOSS"
      />
    </section>
  );
}

function DnsPage() {
  const [dnsData, setDnsData] = useState([]);
  const deltaData = buildDeltaChartData(dnsData);

  useEffect(() => {
    fetchRecentMeasurements()
      .then((data) => {
        setDnsData(buildChartData(data, "DNS_RESOLUTION"));
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">DNS</p>
        <h1>DNS Resolution</h1>
        <p className="page-description">
          DNS values should be interpreted carefully because this benchmark uses IP-based targets.
          The main decision metrics are HTTP, TCP, WebSocket, transfer duration, and jitter.
        </p>
      </div>

      <MetricLineChart
        title="DNS resolution over time"
        description="DNS resolution overhead observed by the agent. Interpret cautiously for IP-based targets."
        data={dnsData}
        metricType="DNS_RESOLUTION"
      />

      <DeltaLineChart
        title="DNS resolution gap over time"
        description="Istanbul minus Frankfurt. Interpret cautiously because the benchmark uses IP-based targets."
        data={deltaData}
        metricType="DNS_RESOLUTION"
      />
    </section>
  );
}

function App() {
  return (
    <BrowserRouter>
      <div className="app-shell">
        <aside className="sidebar">
          <div className="brand">
            <span className="brand-title">AWS Benchmark</span>
            <span className="brand-subtitle">Istanbul LZ vs Frankfurt</span>
          </div>

          <nav className="nav">
            <NavLink to="/" end>
              Overview
            </NavLink>
            <NavLink to="/http">HTTP TTFB</NavLink>
            <NavLink to="/tcp">TCP Latency</NavLink>
            <NavLink to="/websocket">WebSocket RTT</NavLink>
            <NavLink to="/transfer">Transfer Duration</NavLink>
            <NavLink to="/stability">Stability</NavLink>
            <NavLink to="/dns">DNS</NavLink>
          </nav>
        </aside>

        <main className="content">
          <Routes>
            <Route path="/" element={<OverviewPage />} />
            <Route path="/http" element={<HttpLatencyPage />} />
            <Route path="/tcp" element={<TcpPage />} />
            <Route path="/websocket" element={<WebSocketPage />} />
            <Route path="/transfer" element={<TransferPage />} />
            <Route path="/stability" element={<StabilityPage />} />
            <Route path="/dns" element={<DnsPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;