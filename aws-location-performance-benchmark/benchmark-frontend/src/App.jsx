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
} from "recharts";
import "./App.css";

function buildChartData(data, metricType) {
  const BUCKET_SIZE_MS = 5 * 60 * 1000;

  const metrics = data
    .filter((item) => item.metricType === metricType)
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

function MetricLineChart({ title, description, data }) {
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
              itemStyle={{
                fontWeight: 700,
              }}
              formatter={(value, name) => [
                `${Number(value).toFixed(2)} ms`,
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
              name="Istanbul"
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

function OverviewPage() {
  const [comparisons, setComparisons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchMetricComparisons()
      .then((data) => {
        setComparisons(data);
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
          A Turkey-based agent compares AWS Frankfurt with the Istanbul Local Zone across network and
          application-level metrics.
        </p>
      </div>

      {loading && <div className="empty-state">Loading comparison data...</div>}
      {error && <div className="error-state">{error}</div>}
      {!loading && !error && <ComparisonTable comparisons={comparisons} />}
    </section>
  );
}

function HttpLatencyPage() {
  const [chartData, setChartData] = useState([]);

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
          48-hour HTTP time-to-first-byte trend, grouped into time-window averages. Lower values are
          better.
        </p>
      </div>

      <MetricLineChart
        title="HTTP TTFB over time"
        description="Time-to-first-byte trend for Frankfurt and Istanbul targets."
        data={chartData}
      />
    </section>
  );
}

function TcpJitterPage() {
  const [tcpData, setTcpData] = useState([]);
  const [jitterData, setJitterData] = useState([]);

  useEffect(() => {
    fetchRecentMeasurements()
      .then((data) => {
        setTcpData(buildChartData(data, "TCP_LATENCY"));
        setJitterData(buildChartData(data, "JITTER"));
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">TCP & Jitter</p>
        <h1>TCP Latency and Jitter</h1>
        <p className="page-description">
          48-hour TCP latency and jitter trends, grouped into time-window averages. Lower values are
          better.
        </p>
      </div>

      <MetricLineChart
        title="TCP Latency over time"
        description="TCP connection latency trend for Frankfurt and Istanbul targets."
        data={tcpData}
      />

      <MetricLineChart
        title="Jitter over time"
        description="Jitter shows latency variation over time. Lower jitter means more stable network behavior."
        data={jitterData}
      />
    </section>
  );
}

function WebSocketPage() {
  const [webSocketData, setWebSocketData] = useState([]);

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
          48-hour WebSocket round-trip-time trend, grouped into time-window averages.
          Lower values are better for real-time applications.
        </p>
      </div>

      <MetricLineChart
        title="WebSocket RTT over time"
        description="Round-trip time for WebSocket message exchange between the agent and target servers."
        data={webSocketData}
      />
    </section>
  );
}

function ThroughputPage() {
  const [downloadData, setDownloadData] = useState([]);
  const [uploadData, setUploadData] = useState([]);

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
        <p className="eyebrow">Throughput</p>
        <h1>Transfer Duration</h1>
        <p className="page-description">
          48-hour upload and download duration trends, grouped into time-window averages.
          Lower duration means faster transfer.
        </p>
      </div>

      <MetricLineChart
        title="Download duration over time"
        description="Time needed to download the test payload from each target. Lower values are better."
        data={downloadData}
      />

      <MetricLineChart
        title="Upload duration over time"
        description="Time needed to upload the test payload to each target. Lower values are better."
        data={uploadData}
      />
    </section>
  );
}

function ReliabilityPage() {
  const [packetLossData, setPacketLossData] = useState([]);
  const [tlsData, setTlsData] = useState([]);

  useEffect(() => {
    fetchRecentMeasurements()
      .then((data) => {
        setPacketLossData(buildChartData(data, "PACKET_LOSS"));
        setTlsData(buildChartData(data, "TLS_HANDSHAKE"));
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  return (
    <section className="page">
      <div className="page-header">
        <p className="eyebrow">Reliability</p>
        <h1>Packet Loss and TLS</h1>
        <p className="page-description">
          48-hour reliability indicators, grouped into time-window averages.
          Packet loss should stay near zero. TLS is expected to fail in HTTP-only testing.
        </p>
      </div>

      <MetricLineChart
        title="Packet loss over time"
        description="Packet loss percentage trend for Frankfurt and Istanbul targets. Lower values are better."
        data={packetLossData}
      />

      <MetricLineChart
        title="TLS handshake duration over time"
        description="TLS handshake duration trend. In HTTP-only tests, this may stay at zero because TLS checks fail."
        data={tlsData}
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
            <NavLink to="/http">HTTP Latency</NavLink>
            <NavLink to="/tcp-jitter">TCP & Jitter</NavLink>
            <NavLink to="/websocket">WebSocket</NavLink>
            <NavLink to="/throughput">Throughput</NavLink>
            <NavLink to="/reliability">Reliability</NavLink>
          </nav>
        </aside>

        <main className="content">
          <Routes>
            <Route path="/" element={<OverviewPage />} />
            <Route path="/http" element={<HttpLatencyPage />} />
            <Route path="/tcp-jitter" element={<TcpJitterPage />} />
            <Route path="/websocket" element={<WebSocketPage />} />
            <Route path="/throughput" element={<ThroughputPage />} />
            <Route path="/reliability" element={<ReliabilityPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;