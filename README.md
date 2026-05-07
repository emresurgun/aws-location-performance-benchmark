# ZoneBench

**AWS Istanbul Local Zone vs Frankfurt — Distributed Latency Benchmark**

ZoneBench is an end-to-end benchmark and observability system that measures real-world network and application-level performance differences between AWS Frankfurt Region and AWS Istanbul Local Zone from a Turkey-based end-user connection.

The main goal of this project is to test whether geographic proximity alone guarantees lower application latency.

---

## Project Status

This project was built as a completed 48-hour benchmark experiment.

The AWS EC2 instances used during the benchmark were terminated after data collection to avoid unnecessary cloud costs. This repository is kept as a case study containing the source code, architecture, methodology, validation notes, and final results.

---

## Motivation

After AWS Istanbul Local Zone became available, I wanted to compare Istanbul Local Zone with AWS Frankfurt Region under the same measurement conditions.

The initial assumption was simple:

> Since Istanbul is geographically closer to users in Turkey, Istanbul Local Zone should provide lower latency than Frankfurt.

Instead of relying on this assumption, I built a benchmark system and collected data for 48 hours.

---

## Architecture

The system consists of four main parts:

1. **Benchmark Agent**
   - Ran locally from Turkey over my own internet connection.
   - Sent measurement requests every 30 seconds.
   - Measured both Frankfurt and Istanbul targets under the same local network conditions.

2. **Benchmark Target Services**
   - One target ran on an EC2 instance in AWS Frankfurt Region.
   - One target ran on an EC2 instance in AWS Istanbul Local Zone.
   - Both targets ran the same Spring Boot application.

3. **Central Collector**
   - Ran on an EC2 instance in Frankfurt.
   - Received measurement results from the agent.
   - Wrote time-series data to InfluxDB.
   - Exposed summary, comparison, and recent-measurement APIs.

4. **React Dashboard**
   - Visualized average, p95, success rate, time-series charts, and latency-gap charts.
   - Built with React and Recharts.

```text
Local Machine in Turkey
Benchmark Agent
        |
        | measures every 30 seconds
        |
        |------------------------------|
        |                              |
        v                              v
AWS Frankfurt EC2              AWS Istanbul Local Zone EC2
benchmark-target               benchmark-target
eu-central-1                   eu-central-1-ist-1a
        |                              |
        | measurement results are sent by the agent
        | to the central collector after target measurement
        v
AWS Frankfurt EC2
benchmark-central
Spring Boot API + InfluxDB
        |
        v
React + Recharts Dashboard
```

The agent measured the target service first, then sent the result to the central collector.

The time spent sending results from the agent to the central collector is **not included** in any benchmark metric. The comparison focuses only on the agent-to-target measurements.

---

## Deployment Setup

| Component | Location |
|---|---|
| Benchmark Agent | Local machine in Turkey |
| Frankfurt Target | AWS Frankfurt Region |
| Istanbul Target | AWS Istanbul Local Zone |
| Central Collector | AWS Frankfurt Region |
| Time-Series Database | InfluxDB running on the central EC2 |
| Dashboard | React frontend |

The Istanbul target was verified through EC2 instance metadata:

```text
Availability Zone: eu-central-1-ist-1a
Public IPv4: 83.119.129.151
Private IPv4: 172.31.48.154
```

This confirms that the Istanbul target was actually running inside AWS Istanbul Local Zone.

---

## Measured Metrics

The benchmark focused on the following application-level and network-level metrics:

| Metric | Description |
|---|---|
| TCP Latency | TCP connection establishment time |
| HTTP TTFB | HTTP time to first byte |
| WebSocket RTT | WebSocket round-trip time |
| Download Duration | Time required to download the test payload |
| Upload Duration | Time required to upload the test payload |
| Jitter | Variation between consecutive TCP latency measurements |

The backend also supports DNS resolution and probe-failure related measurements, but the main analysis focuses on the metrics above.

DNS results should be interpreted carefully because the benchmark uses IP-based targets. Probe failure rate represents failed application-level probes, not raw TCP packet loss.

---

## Measurement Methodology

- The agent ran locally from Turkey.
- Measurements were performed every 30 seconds.
- Both target services were measured from the same agent.
- Both targets ran identical Spring Boot applications.
- The same metric logic was used for both AWS locations.
- The benchmark ran for 48 hours.
- Results were stored in InfluxDB.
- Summary APIs calculate:
  - average
  - min
  - max
  - p50
  - p95
  - p99
  - sample count
  - success rate

Failed measurements are excluded from latency percentile calculations but included in success-rate calculations.

---

## Results

The final 48-hour measurement showed that Frankfurt performed better than Istanbul Local Zone on most application-level latency metrics from this specific test environment.

Approximate average values:

| Metric | Frankfurt Avg | Istanbul Local Zone Avg | Difference |
|---|---:|---:|---:|
| TCP Latency | 54 ms | 88 ms | Istanbul ~63% slower |
| HTTP TTFB | 103 ms | 169 ms | Istanbul ~65% slower |
| WebSocket RTT | 51 ms | 89 ms | Istanbul ~76% slower |
| Download Duration | 572 ms | 946 ms | Istanbul ~65% slower |
| Upload Duration | 516 ms | 651 ms | Istanbul ~26% slower |
| Jitter | 21.87 ms | 19.50 ms | Istanbul ~11% lower |

The result was different from my initial expectation. Although Istanbul Local Zone is geographically closer to Turkey, Frankfurt achieved lower average latency in this test.

This does **not** mean Istanbul Local Zone is inherently slower or not useful. The result reflects this specific measurement setup:

- one local agent
- one Turkish residential ISP connection
- public internet routing
- two EC2 targets
- application-level measurements over a 48-hour period

A possible explanation is that real-world cloud latency is strongly affected by ISP routing, peering, public internet paths, and region/local zone network maturity, not only physical distance.

---

## Validation

Several checks were performed to reduce the risk of incorrect measurements:

- The Istanbul EC2 instance was verified as `eu-central-1-ist-1a` using EC2 instance metadata.
- The agent configuration was checked to ensure that:
  - `frankfurt` mapped to the Frankfurt EC2 public IP.
  - `istanbul` mapped to the Istanbul Local Zone EC2 public IP.
- Manual `curl` tests from the same local network showed the same general latency pattern.
- Both target services ran the same Spring Boot application.
- Failed measurements were not included in average and percentile latency calculations.
- The central collector write duration was not included in target measurement values.

---

## Modules

```text
aws-location-performance-benchmark/
├── benchmark-agent/       # Scheduler-based measurement agent
├── benchmark-target/      # HTTP + WebSocket target service
├── benchmark-central/     # Collector API + InfluxDB integration
└── benchmark-frontend/    # React + Recharts dashboard
```

---

## benchmark-agent

The benchmark agent is a Spring Boot application responsible for performing scheduled measurements.

It:

- reads target definitions from `application.yml`
- measures each configured target
- calculates metric values
- sends measurement results to the central collector
- excludes central API call duration from target measurements

Example configuration used during the benchmark:

```yaml
server:
  port: 8080

spring:
  application:
    name: benchmark-agent

agent:
  source-region: turkey
  central-api-url: http://<CENTRAL_EC2_PUBLIC_IP>:8082
  targets:
    - region: frankfurt
      host: <FRANKFURT_TARGET_PUBLIC_IP>
      port: 8081
    - region: istanbul
      host: <ISTANBUL_LOCAL_ZONE_TARGET_PUBLIC_IP>
      port: 8081
```

---

## benchmark-target

The target service is a lightweight Spring Boot application deployed to both AWS locations.

Both Frankfurt and Istanbul target instances ran the same application.

Main endpoints:

| Endpoint | Purpose |
|---|---|
| `GET /ping` | Basic HTTP latency / TTFB measurement |
| `GET /health` | Health check |
| `POST /upload` | Upload duration measurement |
| Download endpoint | Download duration measurement |
| WebSocket endpoint | WebSocket RTT measurement |

---

## benchmark-central

The central collector is a Spring Boot application that receives measurement results from the agent and writes them to InfluxDB.

Main responsibilities:

- receive measurement results
- write time-series data to InfluxDB
- expose summary APIs
- expose comparison APIs
- expose recent raw measurements for dashboard charts

Main APIs:

| Endpoint | Description |
|---|---|
| `POST /api/measurements` | Receives measurement results |
| `GET /api/metrics/recent` | Returns recent measurement data |
| `GET /api/metrics/summary` | Returns summary statistics |
| `GET /api/metrics/comparison` | Compares two target regions |

Example comparison request used during the benchmark:

```bash
curl "http://<CENTRAL_EC2_PUBLIC_IP>:8082/api/metrics/comparison?baseline=frankfurt&candidate=istanbul"
```

---

## benchmark-frontend

The frontend dashboard is built with React and Recharts.

It visualizes:

- overview comparison table
- average comparison bar chart
- p95 comparison bar chart
- HTTP TTFB time-series chart
- TCP latency time-series chart
- WebSocket RTT time-series chart
- transfer duration charts
- jitter chart
- latency-gap charts

The latency-gap charts use:

```text
Delta = Istanbul - Frankfurt
```

Interpretation:

```text
Delta > 0  => Istanbul was slower
Delta < 0  => Istanbul was faster
Delta = 0  => Equal
```

This helps separate shared local network fluctuations from the persistent performance gap between the two AWS locations.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend Framework | Spring Boot 3 |
| Build Tool | Maven |
| Time-Series Database | InfluxDB 2.x |
| Frontend | React |
| Charts | Recharts |
| Cloud Infrastructure | AWS EC2 |
| Cloud Location Feature | AWS Local Zones |
| Deployment | EC2 + JAR + Docker for InfluxDB |

---

## Deployment Notes

During the real benchmark:

- Central collector ran on a Frankfurt EC2 instance.
- InfluxDB ran inside Docker on the central EC2 instance.
- Frankfurt target ran on a separate EC2 instance in Frankfurt.
- Istanbul target ran on an EC2 instance in `eu-central-1-ist-1a`.
- Server-side JARs were run with `nohup` so they continued running after SSH sessions closed.

Example:

```bash
nohup java -jar /home/ubuntu/benchmark-target.jar > target.log 2>&1 &
```

The EC2 instances were terminated after the benchmark was completed.

---

## Limitations

- The agent ran from a single local network in Turkey.
- Results may differ across ISPs, cities, mobile networks, corporate networks, or datacenter-based agents.
- The benchmark used public IPs and public internet routing.
- DNS measurements are not a primary decision metric because targets were IP-based.
- Probe failure rate is an application-level failure metric, not raw network packet loss.
- TLS handshake results were excluded because the target services used HTTP, not HTTPS.
- Istanbul Local Zone was newly available at the time of testing; routing and peering behavior may change over time.
- This benchmark does not claim that one AWS location is universally better than the other.

---

## Key Takeaway

The closest cloud location is not always the fastest in practice.

In this 48-hour benchmark, Frankfurt outperformed Istanbul Local Zone on most application-level latency metrics from my local Turkish network.

The result highlights that real-world cloud performance depends on more than physical distance. ISP routing, peering, public internet paths, and infrastructure maturity can significantly affect latency.

---

## License

MIT

## Graphs and Tables
<img width="1281" height="509" alt="Screenshot 2026-05-08 at 01 13 40" src="https://github.com/user-attachments/assets/86044442-940f-4360-8a3c-c78ce9d14b1a" />
<img width="1286" height="995" alt="Screenshot 2026-05-08 at 01 14 42" src="https://github.com/user-attachments/assets/c51647ec-a8a2-474d-8506-c1ef4d5f5143" />
<img width="1281" height="993" alt="Screenshot 2026-05-08 at 01 15 17" src="https://github.com/user-attachments/assets/aa640b3b-393c-4b51-b4c3-b839eb06ed82" />
<img width="1283" height="990" alt="Screenshot 2026-05-08 at 01 16 15" src="https://github.com/user-attachments/assets/79132d6c-7d3f-46aa-9d47-17fbbfb529ea" />
<img width="1283" height="992" alt="Screenshot 2026-05-08 at 01 16 57" src="https://github.com/user-attachments/assets/0335d73c-5e66-4423-b712-b9c875401beb" />
<img width="1281" height="993" alt="Screenshot 2026-05-08 at 01 17 23" src="https://github.com/user-attachments/assets/3beeae30-0918-4599-8722-83a2834ee725" />
<img width="1283" height="993" alt="Screenshot 2026-05-08 at 01 17 47" src="https://github.com/user-attachments/assets/a68a6fce-c990-4b54-aed4-2f113dfec244" />
<img width="1283" height="992" alt="Screenshot 2026-05-08 at 01 18 14" src="https://github.com/user-attachments/assets/9ada65af-8667-4a7a-b609-6e9a4714b634" />


