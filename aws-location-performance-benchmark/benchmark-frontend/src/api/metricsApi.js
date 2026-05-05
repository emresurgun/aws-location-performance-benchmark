const API_BASE_URL = "http://35.159.232.162:8082";

async function request(path) {
  const response = await fetch(`${API_BASE_URL}${path}`);

  if (!response.ok) {
    throw new Error(`API request failed: ${response.status} ${response.statusText}`);
  }

  return response.json();
}

export function fetchMetricComparisons() {
  return request("/api/metrics/comparison?baseline=frankfurt&candidate=istanbul");
}

export function fetchMetricSummaries() {
  return request("/api/metrics/summary");
}

export function fetchRecentMeasurements() {
  return request("/api/metrics/recent");
}