# 🚀 Burp Suite MCP Server — Extended Edition

> A feature-rich fork of the official [PortSwigger MCP Server](https://github.com/PortSwigger/mcp-server) that supercharges your AI-assisted penetration testing workflow with **Bambda generation** and **BCheck scripting**,.


---

## 🤔 What Is This?

The official PortSwigger MCP Server lets AI assistants talk to Burp Suite. This **Extended Edition** goes further — your AI agent can now **write and deploy Bambda scripts directly into Burp's Bambda library**, targeting Proxy, Repeater, Logger, Site map, Match & Replace, WebSocket history, and Custom columns. It can also generate **Custom Scan Checks** that plug into Burp's scanner engine and surface findings with configurable severity — all in real time, without touching a line of Java manually.

Instead of manually writing Java code and copy-pasting it into Burp, you just describe what you want:

> *"Create a proxy filter that shows only POST requests with JSON responses"*

...and the agent generates, saves it in the centralized lirabray — all without leaving your chat window.

☝️ The smarter and more context-aware your AI model is, the more precise and production-ready the generated Bambda will be.


📢 **Before using the tool**, train your AI agent using the pre-built training prompt 
located at `mcp-server/Training Prompts/TrainingPrompt.txt`. Feel free to modify it to make the agent more smarter.

---

## ✨ What's New in This Fork

The original MCP server exposes Burp's core tools to AI agents. This fork adds:

- 🧠 **Bambda Generator** — Describe a filter, action, or scan check in plain English. The agent writes valid Java Bambda code and saves it directly to your Burp Bambda library
- 📍 **Multi-location support** — Bambdas targeting Proxy, Repeater, Logger, Site Map, Scanner, WebSocket history, Match & Replace, and Custom Columns
- ✏️ **Raw code passthrough** — Already have Bambda code? Pass it directly via `scriptCode` and it gets saved as-is
- 🔁 **Overwrite support** — Update an existing Bambda by name without duplicating entries
- 🔍 **Smart prompt parsing** — The generator understands natural language cues like "filter GET requests", "sign request with SHA-256", or "test TRACE method"

---

## 📍 Supported Bambda Locations

| What you say to the agent | `function` (internal) | `location` (internal) | Where it appears in Burp |
|:---|:---|:---|:---|
| `proxy http` | `VIEW_FILTER` | `PROXY_HTTP_HISTORY` | Proxy → HTTP History → Filter settings → Script |
| `proxy websocket` | `VIEW_FILTER` | `PROXY_WEBSOCKET` | Proxy → WebSockets History → Filter settings → Script |
| `match and replace request` | `MATCH_AND_REPLACE_REQUEST` | `PROXY_HTTP_HISTORY` | Proxy → Match and replace → Request rule |
| `match and replace response` | `MATCH_AND_REPLACE_RESPONSE` | `PROXY_HTTP_HISTORY` | Proxy → Match and replace → Response rule |
| `logger` | `VIEW_FILTER` | `LOGGER` | Logger → View filter → Script |
| `logger capture` | `CAPTURE_FILTER` | `LOGGER` | Logger → Capture filter → Script |
| `sitemap` | `VIEW_FILTER` | `SITEMAP` | Target → Site map → Filter → Script |
| `repeater custom action` | `CUSTOM_ACTION` | `REPEATER` | Repeater → Custom actions |
| `custom column` | `CUSTOM_COLUMN` | `PROXY_HTTP_HISTORY` | HTTP History / Logger → Custom column |
| `active scanner check` | `SCAN_CHECK_ACTIVE_PER_REQUEST` | `SCANNER` | Extensions → Bambda library → Active scan checks |

---

## ⚙️ Installation & Configuration

> **Note:** The setup below is identical to the official PortSwigger MCP Server. If you have already configured the original, skip to [the new tools](#-using-the-bambda-generator).

### Step 1 — Load the Extension in Burp

1. Build the project: `./gradlew build`
2. In Burp Suite, go to **Extensions → Add**
3. Select the built `.jar` from `mcp-server/build/libs/`
4. You should see an **MCP** tab appear in Burp's top navigation

<!-- SCREENSHOT: Burp Suite Extensions tab with MCP extension loaded and MCP tab visible in navbar -->

### Step 2 — Configure the MCP Server

In the **MCP tab** inside Burp:

- ✅ **Enabled** — Toggle the MCP server on
- ✅ **Enable tools that can edit your config** — Required for Bambda creation
- 🌐 **Host/Port** — Default is `http://127.0.0.1:9876` (change if needed)



### Step 3 — Connect Your AI Client

#### Option A — Auto-install (Recommended)

Click the **installer button** in the MCP tab. It will automatically add Burp to your Claude Desktop config and handle the proxy setup.

#### Option B — Manual Claude Desktop Config

Open `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) and add:

```json
{
  "mcpServers": {
    "burp": {
      "command": "<path to Java executable packaged with Burp>",
      "args": [
        "-jar",
        "/path/to/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ]
    }
  }
}
```

Then restart Claude Desktop with Burp already running.

#### Option C — SSE Direct (for clients that support SSE)

Point your MCP client directly at:

```
http://127.0.0.1:9876
# or
http://127.0.0.1:9876/sse
```

---

## 🧠 Using the Bambda Generator

The `create_bambda` tool accepts these parameters:

| Parameter | Required | Description |
|:---|:---|:---|
| `name` | ✅ | Name shown in the Bambda library |
| `description` | ✅ | What this Bambda does |
| `location` | ✅ | Where it runs (see table above) |
| `prompt` | ✅ | Plain English description of the logic |
| `scriptCode` | ❌ | Raw Java code — skips prompt generation entirely |
| `overwrite` | ❌ | `true` to replace existing Bambda with same name |

---

## 💡 Real-World Example Prompts

These are copy-paste ready prompts you can give your AI agent right now.

---

### 🔍 Proxy Filters

**Show only POST requests that return JSON:**
```
Create a bambda in proxy http location named "JSON POST Filter"
that filters to show only POST requests where the response is JSON.
```

**Highlight requests missing security headers:**
```
Create a bambda in proxy http location named "Missing Security Headers"
that returns true when the response is missing the X-Frame-Options header.
```

**Filter by hostname:**
```
Create a bambda in proxy http location named "Target Scope Filter"
that only shows requests going to api.example.com
```


---

### ⚡ Repeater Custom Actions

**Test HTTP TRACE method:**
```
Create a bambda in repeater custom action location named "Test TRACE"
that sends the current request with the TRACE method and logs the status code.
```

**API version downgrade/upgrade tester:**
```
Create a bambda in repeater custom action location named "API Version Scanner"
that detects the API version in the URL path (e.g. /v2/) and tests v1 through v5,
logging the status code for each version.
```

**GraphQL introspection check:**
```
Create a bambda in repeater custom action location named "GraphQL Introspection"
that tests the Introspection paylaod and logs whether GraphQL introspection is enabled on the current request.
```


---

### 🔄 Match and Replace

**Inject a custom request header:**
```
Create a bambda in match and replace request location named "Add Debug Header"
that adds the header X-Debug with value true to every request.
```

**Strip server fingerprinting from responses:**
```
Create a bambda in match and replace response location named "Remove Server Header"
that removes the Server header from all responses.
```

**Redirect CSP reports to Burp Collaborator:**
```
Create a bambda in match and replace response location named "CSP Collaborator Redirect"
that rewrites the Content-Security-Policy header to redirect report-uri
and report-to to a Burp Collaborator payload URL.
```


---

### 📊 Custom Columns

**GraphQL operation name column:**
```
Create a bambda custom column named "GraphQL Operation"
that extracts the operationName field from the JSON request body
and displays it in the HTTP history table.
```

**Show response status codes as a column:**
```
Create a bambda custom column named "Status"
that returns the HTTP response status code as a string.
```


---

### 🔬 Active Scan Checks

**CORS misconfiguration scanner:**
```
Create a bambda in scanner active location named "CORS Misconfiguration"
that sends a request with a random evil origin header and checks if it is
reflected in Access-Control-Allow-Origin. Report HIGH severity if credentials
are also allowed.
```



---

### 🌐 WebSocket Filters

**Show only server-to-client messages:**
```
Create a bambda in proxy websocket location named "Server Messages Only"
that filters to show only server-to-client WebSocket messages.
```

---

### 🗺️ Site Map Filters

**Hide requests with no response:**
```
Create a bambda in sitemap location named "Hide No Response"
that filters out any site map nodes that have no HTTP response.
```

---

### 📋 Logger Filters

**Highlight traffic by tool origin:**
```
Create a bambda in logger location named "Highlight By Tool"
that highlights each request a different colour based on which
Burp tool generated it — red for Target, blue for Proxy,
magenta for Repeater, green for Scanner.
```

---


## 🤝 Credits & Acknowledgments

This project builds directly on the outstanding foundational work by the **PortSwigger** team.

- 🏗️ **Original Repository:** [PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server)
- 📖 **Montoya API Docs:** [portswigger.net/burp/documentation/desktop/extend-burp](https://portswigger.net/burp/documentation/desktop/extend-burp)
- 🧩 **Bambda Library:** [github.com/PortSwigger/bambdas](https://github.com/PortSwigger/bambdas)

> This fork maintains full compatibility with the original PortSwigger MCP Server while extending it for advanced security automation workflows.

---

## 📄 License

Apache 2.0 — see [LICENSE](LICENSE) for details.

---

<div align="center">
  <sub>Built with ❤️ for the security community | Fork of <a href="https://github.com/PortSwigger/mcp-server">PortSwigger/mcp-server</a></sub>
</div>
