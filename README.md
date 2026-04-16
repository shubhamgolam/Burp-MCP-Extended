# 🛡️ Burp Suite MCP Server — Extended Edition

> A feature-rich fork of the official [PortSwigger MCP Server](https://github.com/PortSwigger/mcp-server) that supercharges your AI-assisted penetration testing workflow with **Bambda generation**, **BCheck scripting**, and deep Burp Suite integration.

![Burp Suite](https://img.shields.io/badge/Burp%20Suite-2025.10%2B-orange?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0id2hpdGUiIGQ9Ik0xMiAyQzYuNDggMiAyIDYuNDggMiAxMnM0LjQ4IDEwIDEwIDEwIDEwLTQuNDggMTAtMTBTMTcuNTIgMiAxMiAyeiIvPjwvc3ZnPg==)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple?style=for-the-badge&logo=kotlin)
![MCP](https://img.shields.io/badge/MCP-Compatible-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-Apache%202.0-green?style=for-the-badge)

---

## 🤔 What Is This?

The official PortSwigger MCP Server lets AI assistants (like Claude) talk to Burp Suite. This **Extended Edition** goes further — it lets your AI agent **write and deploy Bambda scripts directly into Burp's Bambda library** in real time, across Proxy, Repeater, Scanner, Logger, and more.

Instead of manually writing Java code and copy-pasting it into Burp, you just describe what you want:

> *"Create a proxy filter that shows only POST requests with JSON responses"*

...and the agent generates, saves, and applies it — all without leaving your chat window.

<!-- SCREENSHOT: Hero shot — Claude chat on left, Burp Bambda library on right showing a freshly created script -->

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
| `scanner active` | `SCAN_CHECK_ACTIVE_PER_REQUEST` | `SCANNER` | Extensions → Bambda library → Active scan checks |

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

<!-- SCREENSHOT: Burp MCP tab showing the Enabled checkbox, config editing checkbox, and port settings -->

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

<!-- SCREENSHOT: Proxy HTTP History with a Bambda filter applied, showing filtered results -->

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
that tests whether GraphQL introspection is enabled on the current request.
```

<!-- SCREENSHOT: Repeater tab showing the Custom Actions dropdown with newly created Bambda actions listed -->

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

<!-- SCREENSHOT: Proxy Match and Replace tab showing the new Bambda-powered rule in the list -->

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

<!-- SCREENSHOT: HTTP History table showing custom column "GraphQL Operation" populated with operation names -->

---

### 🔬 Active Scan Checks

**CORS misconfiguration scanner:**
```
Create a bambda in scanner active location named "CORS Misconfiguration"
that sends a request with a random evil origin header and checks if it is
reflected in Access-Control-Allow-Origin. Report HIGH severity if credentials
are also allowed.
```

<!-- SCREENSHOT: Scanner findings showing a CORS issue raised by the custom Bambda scan check -->

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

## 🏗️ How It Works

```
┌─────────────────┐        MCP Protocol         ┌──────────────────────────┐
│                 │  ──────────────────────────▶ │                          │
│   AI Agent      │                              │   Burp MCP Extension     │
│  (Claude etc.)  │  ◀──────────────────────────  │   (this fork)            │
│                 │        Tool Results           │                          │
└─────────────────┘                              └────────────┬─────────────┘
                                                              │
                                                 create_bambda tool call
                                                              │
                                                              ▼
                                                 ┌────────────────────────┐
                                                 │  BambdaGenerator.kt    │
                                                 │  Parses location +     │
                                                 │  generates Java code   │
                                                 └────────────┬───────────┘
                                                              │
                                                  Montoya API importBambda()
                                                              │
                                                              ▼
                                                 ┌────────────────────────┐
                                                 │   Burp Bambda Library  │
                                                 │   Ready to apply! ✅   │
                                                 └────────────────────────┘
```

---

## 🐛 Troubleshooting

| Error | Cause | Fix |
|:---|:---|:---|
| `function is required, location is required` | Wrong JSON field names | Ensure fields are `"function"` and `"location"` not `"functionType"` and `"script"` |
| `LOADED_WITH_ERRORS` | Wrong `function` or `location` value | Check the supported locations table above for exact values |
| `Unresolved reference 'success'` | `BambdaImportResult` is an enum, not interface | Use `.toString().contains("SUCCESS")` instead of `.success()` |
| `Packages cannot be imported` | Broken or dangling import in `Tools.kt` | Check for `import net.portswigger.mcp.tools.` with nothing after the dot |
| `Syntax error: Expecting a top level declaration` | `package` declaration missing or misplaced | `package net.portswigger.mcp.tools` must be the very first line |
| Bambda saves but does nothing | Wrong context object used | Sitemap uses `node`, WebSocket uses `message`, others use `requestResponse` |
| Match & Replace Bambda ignored | Returning boolean instead of object | Return the modified `request` or `response` object, not `true`/`false` |

---

## 🤝 Credits & Acknowledgments

This project builds directly on the outstanding foundational work by the **PortSwigger** team.

- 🏗️ **Original Repository:** [PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server)
- 📖 **Montoya API Docs:** [portswigger.net/burp/documentation/desktop/extend-burp](https://portswigger.net/burp/documentation/desktop/extend-burp)
- 🧩 **Bambda Library:** [github.com/PortSwigger/bambdas](https://github.com/PortSwigger/bambdas)
- 💡 **Community Bambdas:** [gist.github.com/irsdl](https://gist.github.com/irsdl/28bfd1fe6c54c7c98288eeb86da23e6e)

> This fork maintains full compatibility with the original PortSwigger MCP Server while extending it for advanced security automation workflows.

---

## 📄 License

Apache 2.0 — see [LICENSE](LICENSE) for details.

---

<div align="center">
  <sub>Built with ❤️ for the security community | Fork of <a href="https://github.com/PortSwigger/mcp-server">PortSwigger/mcp-server</a></sub>
</div>
