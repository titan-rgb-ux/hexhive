package com.example.titan;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
public class tank {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String[] TARGET_PORTS = {"8081", "8082", "8083"};

    @GetMapping("/monitor")
    public ResponseEntity<String> monitorDashboard() {

        StringBuilder html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>🚀 Microservice Monitor Dashboard</title>
                    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
                            min-height: 100vh;
                            color: #e0e0e0;
                            padding: 20px;
                        }

                        .header {
                            text-align: center;
                            padding: 20px;
                            margin-bottom: 30px;
                            background: rgba(255,255,255,0.1);
                            border-radius: 15px;
                            backdrop-filter: blur(10px);
                        }

                        .header h1 {
                            font-size: 2.5em;
                            background: linear-gradient(90deg, #00d9ff, #00ff88);
                            -webkit-background-clip: text;
                            -webkit-text-fill-color: transparent;
                            margin-bottom: 10px;
                        }

                        .header p { color: #a0a0a0; font-size: 1.1em; }

                        .dashboard-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
                            gap: 25px;
                            max-width: 1400px;
                            margin: 0 auto;
                        }

                        .service-card {
                            background: rgba(255,255,255,0.05);
                            border-radius: 20px;
                            padding: 25px;
                            border: 1px solid rgba(255,255,255,0.1);
                            transition: all 0.3s ease;
                            backdrop-filter: blur(10px);
                        }

                        .service-card:hover {
                            transform: translateY(-5px);
                            box-shadow: 0 20px 40px rgba(0,0,0,0.3);
                            border-color: rgba(0,217,255,0.3);
                        }

                        .service-header {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            margin-bottom: 20px;
                            padding-bottom: 15px;
                            border-bottom: 1px solid rgba(255,255,255,0.1);
                        }

                        .port-badge {
                            font-size: 1.5em;
                            font-weight: bold;
                            color: #00d9ff;
                        }

                        .status-indicator {
                            display: flex;
                            align-items: center;
                            gap: 8px;
                            padding: 8px 16px;
                            border-radius: 20px;
                            font-weight: bold;
                            font-size: 0.9em;
                        }

                        .status-dot {
                            width: 12px;
                            height: 12px;
                            border-radius: 50%;
                            animation: pulse 2s infinite;
                        }

                        @keyframes pulse {
                            0%, 100% { opacity: 1; transform: scale(1); }
                            50% { opacity: 0.5; transform: scale(1.2); }
                        }

                        .UP .status-indicator {
                            background: rgba(0,255,136,0.2);
                            color: #00ff88;
                        }

                        .UP .status-dot {
                            background: #00ff88;
                            box-shadow: 0 0 10px #00ff88;
                        }

                        .DOWN .status-indicator {
                            background: rgba(255,71,87,0.2);
                            color: #ff4757;
                        }

                        .DOWN .status-dot {
                            background: #ff4757;
                            box-shadow: 0 0 10px #ff4757;
                        }

                        .metrics-grid {
                            display: grid;
                            grid-template-columns: repeat(2, 1fr);
                            gap: 15px;
                            margin-bottom: 20px;
                        }

                        .metric-box {
                            background: rgba(0,0,0,0.3);
                            padding: 15px;
                            border-radius: 12px;
                            text-align: center;
                        }

                        .metric-label {
                            font-size: 0.85em;
                            color: #a0a0a0;
                            margin-bottom: 5px;
                        }

                        .metric-value {
                            font-size: 1.4em;
                            font-weight: bold;
                            color: #ffffff;
                        }

                        .metric-unit {
                            font-size: 0.7em;
                            color: #808080;
                        }

                        .chart-container {
                            margin-top: 15px;
                            height: 150px;
                        }

                        .footer {
                            text-align: center;
                            margin-top: 40px;
                            padding: 20px;
                            color: #606060;
                            font-size: 0.9em;
                        }

                        .refresh-btn {
                            background: linear-gradient(90deg, #00d9ff, #00ff88);
                            border: none;
                            padding: 12px 30px;
                            border-radius: 25px;
                            color: #1a1a2e;
                            font-weight: bold;
                            cursor: pointer;
                            margin-top: 20px;
                            transition: all 0.3s ease;
                        }

                        .refresh-btn:hover {
                            transform: scale(1.05);
                            box-shadow: 0 10px 20px rgba(0,217,255,0.3);
                        }

                        .loading { opacity: 0.5; pointer-events: none; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>🚀 Microservice Monitor</h1>
                        <p>Real-time health & performance dashboard</p>
                    </div>

                    <div class="dashboard-grid" id="dashboard">
                """);

        // Generate cards for each port
        for (String port : TARGET_PORTS) {
            String baseUrl = "http://localhost:" + port;
            String status = "DOWN";
            double cpu = 0, memory = 0, heap = 0, threads = 0, uptime = 0, httpReqs = 0;

            try {
                // Health Check
                Map health = restTemplate.getForObject(baseUrl + "/actuator/health", Map.class);
                status = (String) health.get("status");

                // CPU Usage
                Map cpuMetric = restTemplate.getForObject(baseUrl + "/actuator/metrics/system.cpu.usage", Map.class);
                if (cpuMetric != null && cpuMetric.get("measurements") != null) {
                    List measurements = (List) cpuMetric.get("measurements");
                    if (!measurements.isEmpty()) {
                        cpu = ((Number) ((Map) measurements.get(0)).get("value")).doubleValue() * 100;
                    }
                }

                // Memory Used
                Map memMetric = restTemplate.getForObject(baseUrl + "/actuator/metrics/jvm.memory.used", Map.class);
                if (memMetric != null && memMetric.get("measurements") != null) {
                    List measurements = (List) memMetric.get("measurements");
                    if (!measurements.isEmpty()) {
                        memory = ((Number) ((Map) measurements.get(0)).get("value")).doubleValue() / 1024 / 1024;
                    }
                }

                // Heap Memory
                Map heapMetric = restTemplate.getForObject(baseUrl + "/actuator/metrics/jvm.memory.used?area=heap", Map.class);
                if (heapMetric != null && heapMetric.get("measurements") != null) {
                    List measurements = (List) heapMetric.get("measurements");
                    if (!measurements.isEmpty()) {
                        heap = ((Number) ((Map) measurements.get(0)).get("value")).doubleValue() / 1024 / 1024;
                    }
                }

                // Threads
                Map threadMetric = restTemplate.getForObject(baseUrl + "/actuator/metrics/jvm.threads.live", Map.class);
                if (threadMetric != null && threadMetric.get("measurements") != null) {
                    List measurements = (List) threadMetric.get("measurements");
                    if (!measurements.isEmpty()) {
                        threads = ((Number) ((Map) measurements.get(0)).get("value")).doubleValue();
                    }
                }

                // Uptime
                Map uptimeMetric = restTemplate.getForObject(baseUrl + "/actuator/metrics/process.uptime", Map.class);
                if (uptimeMetric != null && uptimeMetric.get("measurements") != null) {
                    List measurements = (List) uptimeMetric.get("measurements");
                    if (!measurements.isEmpty()) {
                        uptime = ((Number) ((Map) measurements.get(0)).get("value")).doubleValue();
                    }
                }

                // HTTP Requests
                Map httpMetric = restTemplate.getForObject(baseUrl + "/actuator/metrics/http.server.requests.count", Map.class);
                if (httpMetric != null && httpMetric.get("measurements") != null) {
                    List measurements = (List) httpMetric.get("measurements");
                    if (!measurements.isEmpty()) {
                        httpReqs = ((Number) ((Map) measurements.get(0)).get("value")).doubleValue();
                    }
                }

            } catch (Exception ignored) {}

            String uptimeFormatted = formatUptime(uptime);

            html.append("""
                    <div class="service-card %s" id="card-%s">
                        <div class="service-header">
                            <span class="port-badge">⚡ Port %s</span>
                            <div class="status-indicator">
                                <span class="status-dot"></span>
                                <span class="status-text">%s</span>
                            </div>
                        </div>

                        <div class="metrics-grid">
                            <div class="metric-box">
                                <div class="metric-label">CPU Usage</div>
                                <div class="metric-value" id="cpu-%s">%.2f<span class="metric-unit">%%</span></div>
                            </div>
                            <div class="metric-box">
                                <div class="metric-label">Memory Used</div>
                                <div class="metric-value" id="mem-%s">%.2f<span class="metric-unit">MB</span></div>
                            </div>
                            <div class="metric-box">
                                <div class="metric-label">Heap Memory</div>
                                <div class="metric-value" id="heap-%s">%.2f<span class="metric-unit">MB</span></div>
                            </div>
                            <div class="metric-box">
                                <div class="metric-label">Live Threads</div>
                                <div class="metric-value" id="threads-%s">%.0f</div>
                            </div>
                            <div class="metric-box">
                                <div class="metric-label">HTTP Requests</div>
                                <div class="metric-value" id="http-%s">%.0f</div>
                            </div>
                            <div class="metric-box">
                                <div class="metric-label">Uptime</div>
                                <div class="metric-value" id="uptime-%s">%s</div>
                            </div>
                        </div>

                        <div class="chart-container">
                            <canvas id="chart-%s"></canvas>
                        </div>
                    </div>
                    """.formatted(
                    status, port, port, status,
                    port, cpu, port, memory, port, heap,
                    port, threads, port, httpReqs, port, uptimeFormatted, port
            ));
        }

        html.append("""
                    </div>

                    <div style="text-align: center;">
                        <button class="refresh-btn" onclick="refreshDashboard()">🔄 Refresh Now</button>
                    </div>

                    <div class="footer">
                        <p>Last Updated: <span id="lastUpdate">--:--:--</span> | Auto-refresh: 5 seconds</p>
                        <p>Monitoring: %s</p>
                    </div>

                    <script>
                        const charts = {};
                        const PORTS = %s;

                        // Initialize charts for each port
                        function initCharts() {
                            PORTS.forEach(port => {
                                const ctx = document.getElementById('chart-' + port);
                                if (ctx) {
                                    charts[port] = new Chart(ctx, {
                                        type: 'line',
                                         {
                                            labels: [],
                                            datasets: [{
                                                label: 'CPU %%',
                                                 [],
                                                borderColor: '#00d9ff',
                                                backgroundColor: 'rgba(0,217,255,0.1)',
                                                tension: 0.4,
                                                fill: true
                                            }]
                                        },
                                        options: {
                                            responsive: true,
                                            maintainAspectRatio: false,
                                            plugins: { legend: { display: false } },
                                            scales: {
                                                x: { display: false },
                                                y: { 
                                                    beginAtZero: true, 
                                                    max: 100,
                                                    grid: { color: 'rgba(255,255,255,0.1)' },
                                                    ticks: { color: '#a0a0a0' }
                                                }
                                            },
                                            animation: { duration: 300 }
                                        }
                                    });
                                }
                            });
                        }

                        function formatUptime(seconds) {
                            const hrs = Math.floor(seconds / 3600);
                            const mins = Math.floor((seconds %% 3600) / 60);
                            const secs = Math.floor(seconds %% 60);
                            return `${hrs}h ${mins}m ${secs}s`;
                        }

                        async function refreshDashboard() {
                            document.getElementById('dashboard').classList.add('loading');
                            
                            try {
                                const response = await fetch('/api/monitoring/data');
                                const data = await response.json();
                                
                                data.forEach(instance => {
                                    const port = instance.port;
                                    const card = document.getElementById('card-' + port);
                                    
                                    // Update status
                                    card.className = 'service-card ' + instance.status;
                                    card.querySelector('.status-text').textContent = instance.status;
                                    
                                    // Update metrics
                                    document.getElementById('cpu-' + port).innerHTML = 
                                        instance.cpu.toFixed(2) + '<span class="metric-unit">%%</span>';
                                    document.getElementById('mem-' + port).innerHTML = 
                                        instance.memory.toFixed(2) + '<span class="metric-unit">MB</span>';
                                    document.getElementById('heap-' + port).innerHTML = 
                                        instance.heap.toFixed(2) + '<span class="metric-unit">MB</span>';
                                    document.getElementById('threads-' + port).textContent = 
                                        instance.threads.toFixed(0);
                                    document.getElementById('http-' + port).textContent = 
                                        instance.httpReqs.toFixed(0);
                                    document.getElementById('uptime-' + port).textContent = 
                                        formatUptime(instance.uptime);
                                    
                                    // Update chart
                                    if (charts[port]) {
                                        const time = new Date().toLocaleTimeString();
                                        charts[port].data.labels.push(time);
                                        charts[port].data.datasets[0].data.push(instance.cpu);
                                        
                                        if (charts[port].data.labels.length > 10) {
                                            charts[port].data.labels.shift();
                                            charts[port].data.datasets[0].data.shift();
                                        }
                                        charts[port].update();
                                    }
                                });
                                
                                document.getElementById('lastUpdate').textContent = 
                                    new Date().toLocaleTimeString();
                                    
                            } catch (error) {
                                console.error('Refresh failed:', error);
                            }
                            
                            document.getElementById('dashboard').classList.remove('loading');
                        }

                        // Initialize on load
                        initCharts();
                        refreshDashboard();
                        setInterval(refreshDashboard, 5000);
                    </script>
                </body>
                </html>
                """.formatted(String.join(", ", TARGET_PORTS), Arrays.toString(TARGET_PORTS)));

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html.toString());
    }

    /**
     * API endpoint for JavaScript to fetch fresh data
     */
    @GetMapping("/apimonitor")
    public ResponseEntity<List<Map<String, Object>>> getMonitoringData() {
        List<Map<String, Object>> allData = new ArrayList<>();

        for (String port : TARGET_PORTS) {
            String baseUrl = "http://localhost:" + port;
            Map<String, Object> instanceData = new HashMap<>();
            instanceData.put("port", port);

            try {
                // Health
                Map health = restTemplate.getForObject(baseUrl + "/actuator/health", Map.class);
                instanceData.put("status", health != null ? health.get("status") : "DOWN");

                // CPU
                instanceData.put("cpu", getMetricValue(baseUrl, "system.cpu.usage") * 100);

                // Memory
                instanceData.put("memory", getMetricValue(baseUrl, "jvm.memory.used") / 1024 / 1024);

                // Heap
                instanceData.put("heap", getMetricValue(baseUrl, "jvm.memory.used?area=heap") / 1024 / 1024);

                // Threads
                instanceData.put("threads", getMetricValue(baseUrl, "jvm.threads.live"));

                // HTTP Requests
                instanceData.put("httpReqs", getMetricValue(baseUrl, "http.server.requests.count"));

                // Uptime
                instanceData.put("uptime", getMetricValue(baseUrl, "process.uptime"));

            } catch (Exception e) {
                instanceData.put("status", "DOWN");
                instanceData.put("cpu", 0.0);
                instanceData.put("memory", 0.0);
                instanceData.put("heap", 0.0);
                instanceData.put("threads", 0.0);
                instanceData.put("httpReqs", 0.0);
                instanceData.put("uptime", 0.0);
            }

            allData.add(instanceData);
        }

        return ResponseEntity.ok(allData);
    }

    private double getMetricValue(String baseUrl, String metricPath) {
        try {
            Map metric = restTemplate.getForObject(baseUrl + "/actuator/metrics/" + metricPath, Map.class);
            if (metric != null && metric.get("measurements") != null) {
                List measurements = (List) metric.get("measurements");
                if (!measurements.isEmpty()) {
                    return ((Number) ((Map) measurements.get(0)).get("value")).doubleValue();
                }
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private String formatUptime(double seconds) {
        long hrs = (long) (seconds / 3600);
        long mins = (long) ((seconds % 3600) / 60);
        long secs = (long) (seconds % 60);
        return String.format("%dh %dm %ds", hrs, mins, secs);
    }
}