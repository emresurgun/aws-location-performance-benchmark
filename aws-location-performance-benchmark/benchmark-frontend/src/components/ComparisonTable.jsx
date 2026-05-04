function formatMs(value) {
  if (value === null || value === undefined) {
    return "-";
  }

  return `${Number(value).toFixed(2)} ms`;
}

function formatImprovement(value) {
  if (value === null || value === undefined) {
    return "-";
  }

  if (value > 0) {
    return `${Number(value).toFixed(1)}% faster`;
  }

  if (value < 0) {
    return `${Math.abs(Number(value)).toFixed(1)}% slower`;
  }

  return "Same";
}

function metricLabel(metricType) {
  const labels = {
    TCP_LATENCY: "TCP Latency",
    HTTP_TTFB: "HTTP TTFB",
    TLS_HANDSHAKE: "TLS Handshake",
    DNS_RESOLUTION: "DNS Resolution",
    JITTER: "Jitter",
    PACKET_LOSS: "Packet Loss",
    WEBSOCKET_RTT: "WebSocket RTT",
    THROUGHPUT_DOWN: "Download Duration",
    THROUGHPUT_UP: "Upload Duration",
  };

  return labels[metricType] || metricType;
}

function improvementClass(value) {
  if (value > 0) {
    return "faster";
  }

  if (value < 0) {
    return "slower";
  }

  return "neutral";
}

export default function ComparisonTable({ comparisons }) {
  if (!comparisons || comparisons.length === 0) {
    return <div className="empty-state">No comparison data available yet.</div>;
  }

  return (
    <div className="table-card">
      <div className="table-header">
        <div>
          <h2>Istanbul Local Zone vs Frankfurt</h2>
          <p>Based on the last 48 hours of measurements. Lower milliseconds are better.

      Improvement shows how much faster Istanbul is compared to Frankfurt.</p>
        </div>
      </div>

      <div className="table-wrapper">
        <table className="comparison-table">
          <thead>
            <tr>
              <th>Metric</th>
              <th>Frankfurt Avg</th>
              <th>Istanbul Avg</th>
              <th>Avg Improvement</th>
              <th>Frankfurt p95</th>
              <th>Istanbul p95</th>
              <th>p95 Improvement</th>
            </tr>
          </thead>
          <tbody>
            {comparisons.map((item) => (
              <tr key={item.metricType}>
                <td className="metric-name">{metricLabel(item.metricType)}</td>
                <td>{formatMs(item.baselineAverageMs)}</td>
                <td>{formatMs(item.candidateAverageMs)}</td>
                <td className={improvementClass(item.averageImprovementPercent)}>
                  {formatImprovement(item.averageImprovementPercent)}
                </td>
                <td>{formatMs(item.baselineP95Ms)}</td>
                <td>{formatMs(item.candidateP95Ms)}</td>
                <td className={improvementClass(item.p95ImprovementPercent)}>
                  {formatImprovement(item.p95ImprovementPercent)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}