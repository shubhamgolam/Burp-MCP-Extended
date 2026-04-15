package net.portswigger.mcp.tools

enum class BambdaLocation {
    // Proxy
    PROXY_HTTP_FILTER,
    PROXY_WS_FILTER,
    MATCH_AND_REPLACE_REQUEST,
    MATCH_AND_REPLACE_RESPONSE,
    // Logger
    LOGGER_VIEW_FILTER,
    LOGGER_CAPTURE_FILTER,
    // Site map
    SITEMAP_FILTER,
    // Repeater
    REPEATER_CUSTOM_ACTION,
    // Custom column
    CUSTOM_COLUMN,
    // Scanner
    SCANNER_ACTIVE
}

data class BambdaJsonMeta(val functionType: String, val location: String)

// ── ALL values confirmed from real exported .bambda files ─────────────────────
fun bambdaJsonMeta(location: BambdaLocation): BambdaJsonMeta = when (location) {
    BambdaLocation.PROXY_HTTP_FILTER          -> BambdaJsonMeta("VIEW_FILTER",                    "PROXY_HTTP_HISTORY")
    BambdaLocation.PROXY_WS_FILTER            -> BambdaJsonMeta("VIEW_FILTER",                    "PROXY_WEBSOCKET")
    BambdaLocation.MATCH_AND_REPLACE_REQUEST  -> BambdaJsonMeta("MATCH_AND_REPLACE_REQUEST",      "PROXY_HTTP_HISTORY")
    BambdaLocation.MATCH_AND_REPLACE_RESPONSE -> BambdaJsonMeta("MATCH_AND_REPLACE_RESPONSE",     "PROXY_HTTP_HISTORY")
    BambdaLocation.LOGGER_VIEW_FILTER         -> BambdaJsonMeta("VIEW_FILTER",                    "LOGGER")
    BambdaLocation.LOGGER_CAPTURE_FILTER      -> BambdaJsonMeta("CAPTURE_FILTER",                 "LOGGER")
    BambdaLocation.SITEMAP_FILTER             -> BambdaJsonMeta("VIEW_FILTER",                    "SITEMAP")
    BambdaLocation.REPEATER_CUSTOM_ACTION     -> BambdaJsonMeta("CUSTOM_ACTION",                  "REPEATER")
    BambdaLocation.CUSTOM_COLUMN              -> BambdaJsonMeta("CUSTOM_COLUMN",                  "PROXY_HTTP_HISTORY")
    BambdaLocation.SCANNER_ACTIVE             -> BambdaJsonMeta("SCAN_CHECK_ACTIVE_PER_REQUEST",  "SCANNER")
}

fun parseBambdaLocation(raw: String): BambdaLocation {
    val normalized = raw.trim()
        .lowercase()
        .replace("-", " ")
        .replace("_", " ")
        .replace(Regex("\\s+"), " ")

    return when {
        normalized == "proxy" ||
            normalized == "proxy http" ||
            normalized == "proxy filter" ||
            normalized == "proxy http filter" ||
            normalized == "proxy http history" ||
            normalized == "proxy http history filter" ->
            BambdaLocation.PROXY_HTTP_FILTER

        normalized == "proxy websocket" ||
            normalized == "proxy websockets" ||
            normalized == "proxy ws" ||
            normalized == "proxy websocket filter" ||
            normalized == "proxy ws filter" ->
            BambdaLocation.PROXY_WS_FILTER

        normalized == "match replace request" ||
            normalized == "match and replace request" ||
            normalized == "match replace req" ->
            BambdaLocation.MATCH_AND_REPLACE_REQUEST

        normalized == "match replace response" ||
            normalized == "match and replace response" ||
            normalized == "match replace resp" ->
            BambdaLocation.MATCH_AND_REPLACE_RESPONSE

        normalized == "match replace" ||
            normalized == "match and replace" ->
            BambdaLocation.MATCH_AND_REPLACE_REQUEST // default to request side

        normalized == "logger" ||
            normalized == "logger view" ||
            normalized == "logger view filter" ->
            BambdaLocation.LOGGER_VIEW_FILTER

        normalized == "logger capture" ||
            normalized == "logger capture filter" ->
            BambdaLocation.LOGGER_CAPTURE_FILTER

        normalized == "sitemap" ||
            normalized == "site map" ||
            normalized == "target" ||
            normalized == "target filter" ||
            normalized == "target site map" ||
            normalized == "target site map filter" ||
            normalized == "site map filter" ||
            normalized == "sitemap filter" ->
            BambdaLocation.SITEMAP_FILTER

        normalized == "repeater" ||
            normalized == "custom action" ||
            normalized == "repeater custom action" ||
            normalized == "repeater action" ->
            BambdaLocation.REPEATER_CUSTOM_ACTION

        normalized == "custom column" ||
            normalized == "column" ->
            BambdaLocation.CUSTOM_COLUMN

        normalized == "scanner" ||
            normalized == "scanner active" ||
            normalized == "active scan" ||
            normalized == "scan check" ||
            normalized == "scan check active" ->
            BambdaLocation.SCANNER_ACTIVE

        else -> throw IllegalArgumentException(
            "Unsupported bambda location '$raw'. Try values like: " +
                "'proxy http', 'proxy websocket', 'match and replace request', " +
                "'match and replace response', 'logger', 'logger capture', " +
                "'sitemap', 'repeater custom action', 'custom column', 'scanner active'."
        )
    }
}

fun usageHintFor(location: BambdaLocation): String = when (location) {
    BambdaLocation.PROXY_HTTP_FILTER          -> "Proxy > HTTP history > Filter settings > Script"
    BambdaLocation.PROXY_WS_FILTER            -> "Proxy > WebSockets history > Filter settings > Script"
    BambdaLocation.MATCH_AND_REPLACE_REQUEST  -> "Proxy > Match and replace rules > Request rule with script"
    BambdaLocation.MATCH_AND_REPLACE_RESPONSE -> "Proxy > Match and replace rules > Response rule with script"
    BambdaLocation.LOGGER_VIEW_FILTER         -> "Logger > View filter > Script"
    BambdaLocation.LOGGER_CAPTURE_FILTER      -> "Logger > Capture filter > Script"
    BambdaLocation.SITEMAP_FILTER             -> "Target > Site map > Filter > Script"
    BambdaLocation.REPEATER_CUSTOM_ACTION     -> "Repeater > Custom actions"
    BambdaLocation.CUSTOM_COLUMN             ->
        "Any Burp table supporting custom columns (HTTP history, Logger, WebSockets history)"
    BambdaLocation.SCANNER_ACTIVE            -> "Extensions > Bambda library > apply to active scan checks"
}

// ─────────────────────────────────────────────────────────────────────────────
//  TOP-LEVEL ENTRY POINT
// ─────────────────────────────────────────────────────────────────────────────

fun generateBambdaSource(
    locationRaw: String,
    name: String,
    description: String,
    prompt: String
): String {
    val location = parseBambdaLocation(locationRaw)
    val normalizedPrompt = prompt.trim()

    val header = buildString {
        appendLine("/**")
        appendLine(" * Name: $name")
        appendLine(" * Description: $description")
        appendLine(" * Prompt: $normalizedPrompt")
        appendLine(" */")
    }

    val body = when (location) {
        BambdaLocation.PROXY_HTTP_FILTER          -> proxyHttpFilterFromPrompt(normalizedPrompt)
        BambdaLocation.PROXY_WS_FILTER            -> proxyWsFilterFromPrompt(normalizedPrompt)
        BambdaLocation.MATCH_AND_REPLACE_REQUEST  -> matchReplaceRequestFromPrompt(normalizedPrompt)
        BambdaLocation.MATCH_AND_REPLACE_RESPONSE -> matchReplaceResponseFromPrompt(normalizedPrompt)
        BambdaLocation.LOGGER_VIEW_FILTER         -> loggerViewFilterFromPrompt(normalizedPrompt)
        BambdaLocation.LOGGER_CAPTURE_FILTER      -> loggerCaptureFilterFromPrompt(normalizedPrompt)
        BambdaLocation.SITEMAP_FILTER             -> sitemapFilterFromPrompt(normalizedPrompt)
        BambdaLocation.REPEATER_CUSTOM_ACTION     -> repeaterCustomActionFromPrompt(normalizedPrompt)
        BambdaLocation.CUSTOM_COLUMN              -> customColumnFromPrompt(normalizedPrompt)
        BambdaLocation.SCANNER_ACTIVE             -> scannerActiveFromPrompt(normalizedPrompt)
    }

    return header + "\n" + body
}

// ─────────────────────────────────────────────────────────────────────────────
//  PROXY HTTP HISTORY FILTER  (VIEW_FILTER / PROXY_HTTP_HISTORY)
//  Context object: requestResponse
//  Returns: boolean
// ─────────────────────────────────────────────────────────────────────────────

private fun proxyHttpFilterFromPrompt(prompt: String): String {
    if (prompt.contains("return") && prompt.contains("requestResponse")) return prompt

    val p          = prompt.lowercase()
    val method     = extractMethod(p)
    val statusCode = extractStatusCode(p)
    val mimeType   = extractMimeType(p)
    val paramName  = extractParamName(p)
    val host       = extractHost(p)
    val path       = extractQuotedPath(prompt)
        ?: if (method == null && mimeType == null) extractPath(p) else null

    val needsResponseGuard = statusCode != null || mimeType != null
    val conditions = mutableListOf<String>()

    method?.let    { conditions += """requestResponse.request().method().equalsIgnoreCase("$it")""" }
    host?.let      { conditions += """requestResponse.request().httpService().host().equals("$it")""" }
    path?.let      { conditions += """requestResponse.request().pathWithoutQuery().equals("$it")""" }
    paramName?.let { conditions += """requestResponse.request().hasParameter("$it", HttpParameterType.URL)""" }
    statusCode?.let { conditions += "requestResponse.response().statusCode() == $it" }
    mimeType?.let { mime ->
        conditions += when (mime) {
            "text/html"               -> "requestResponse.response().statedMimeType() == MimeType.HTML"
            "application/json"        -> "requestResponse.response().statedMimeType() == MimeType.JSON"
            "application/javascript",
            "text/javascript"         -> "requestResponse.response().statedMimeType() == MimeType.SCRIPT"
            "text/css"                -> "requestResponse.response().statedMimeType() == MimeType.CSS"
            "application/xml",
            "text/xml"                -> "requestResponse.response().statedMimeType() == MimeType.XML"
            "image"                   -> "requestResponse.response().statedMimeType() == MimeType.IMAGE_UNKNOWN"
            else                      ->
                """requestResponse.response().headerValue("Content-Type") != null &&
    requestResponse.response().headerValue("Content-Type").toLowerCase().contains("$mime")"""
        }
    }

    return if (conditions.isEmpty()) {
        """
if (!requestResponse.hasResponse()) {
    return false;
}
return requestResponse.request().method().equalsIgnoreCase("GET");
        """.trimIndent()
    } else {
        buildString {
            if (needsResponseGuard) {
                appendLine("if (!requestResponse.hasResponse()) {")
                appendLine("    return false;")
                appendLine("}")
                appendLine()
            }
            append("return ")
            append(conditions.joinToString(" &&\n    "))
            append(";")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PROXY WEBSOCKET FILTER  (VIEW_FILTER / PROXY_WEBSOCKET)
//  Context object: message
//  Returns: boolean
// ─────────────────────────────────────────────────────────────────────────────

private fun proxyWsFilterFromPrompt(prompt: String): String {
    if (prompt.contains("return") && prompt.contains("message")) return prompt

    val p = prompt.lowercase()

    if (p.contains("server to client") || p.contains("server-to-client")) {
        return "return message.direction() == burp.api.montoya.websocket.Direction.SERVER_TO_CLIENT;"
    }

    val length = extractNumber(p)
    if (length != null && (p.contains("length") || p.contains("payload"))) {
        return "return message.payload().length() > $length;"
    }

    // Default: return all messages that have a payload
    return "return message.payload().length() > 0;"
}

// ─────────────────────────────────────────────────────────────────────────────
//  MATCH AND REPLACE — REQUEST  (MATCH_AND_REPLACE_REQUEST / PROXY_HTTP_HISTORY)
//  Context object: requestResponse
//  Returns: modified HttpRequest object
// ─────────────────────────────────────────────────────────────────────────────

private fun matchReplaceRequestFromPrompt(prompt: String): String {
    if (prompt.contains("return") && prompt.contains("requestResponse")) return prompt

    val p          = prompt.lowercase()
    val headerName = extractHeaderName(p)
    val headerVal  = extractHeaderValue(prompt)

    // Add/replace a header
    if (headerName != null && headerVal != null &&
        (p.contains("add") || p.contains("inject") || p.contains("set"))) {
        return """
var req = requestResponse.request();
return req.withAddedHeader("$headerName", "$headerVal");
        """.trimIndent()
    }

    // Remove a header
    if (headerName != null && (p.contains("remove") || p.contains("strip") || p.contains("delete"))) {
        return """
var req = requestResponse.request();
if (!req.hasHeader("$headerName")) {
    return req;
}
return req.withRemovedHeader("$headerName");
        """.trimIndent()
    }

    // Sign request with SHA-256 (pattern: sign, digest, hash, sha)
    if (p.contains("sign") || p.contains("digest") || p.contains("sha")) {
        return """
var digest = utilities.cryptoUtils().generateDigest(
    requestResponse.request().body(),
    DigestAlgorithm.SHA_256
);
var signature = HexFormat.of().formatHex(digest.getBytes());
return requestResponse.request().withAddedHeader("Content-Sha256", signature);
        """.trimIndent()
    }

    // Default: return request unmodified with a placeholder comment
    return """
var req = requestResponse.request();
// TODO: Apply your match and replace logic here
// Prompt: $prompt
return req;
    """.trimIndent()
}

// ─────────────────────────────────────────────────────────────────────────────
//  MATCH AND REPLACE — RESPONSE  (MATCH_AND_REPLACE_RESPONSE / PROXY_HTTP_HISTORY)
//  Context object: requestResponse
//  Returns: modified HttpResponse object
// ─────────────────────────────────────────────────────────────────────────────

private fun matchReplaceResponseFromPrompt(prompt: String): String {
    if (prompt.contains("return") && prompt.contains("requestResponse")) return prompt

    val p          = prompt.lowercase()
    val headerName = extractHeaderName(p)
    val headerVal  = extractHeaderValue(prompt)

    // Add/replace a response header
    if (headerName != null && headerVal != null &&
        (p.contains("add") || p.contains("inject") || p.contains("set"))) {
        return """
var resp = requestResponse.response();
if (resp == null) {
    return resp;
}
return resp.withAddedHeader("$headerName", "$headerVal");
        """.trimIndent()
    }

    // Remove a response header
    if (headerName != null && (p.contains("remove") || p.contains("strip") || p.contains("delete"))) {
        return """
var resp = requestResponse.response();
if (resp == null || !resp.hasHeader("$headerName")) {
    return resp;
}
return resp.withRemovedHeader("$headerName");
        """.trimIndent()
    }

    // Collaborator redirect pattern (CSP, report-uri)
    if (p.contains("collaborator") || p.contains("report-uri") || p.contains("report uri")) {
        return """
var resp = requestResponse.response();
var collaborator = "https://" + api().collaborator().defaultPayloadGenerator().generatePayload();

if (!resp.hasHeader("Content-Security-Policy")) {
    return resp;
}

var cspValue = resp
    .headerValue("Content-Security-Policy")
    .replaceAll("(report-uri|report-to)\\\\s+[^;]+", "report-to csp-reports");

return resp
    .withUpdatedHeader("Content-Security-Policy", cspValue)
    .withRemovedHeader("Reporting-Endpoints")
    .withAddedHeader("Reporting-Endpoints", "csp-reports=\\"" + collaborator + "\\"");
        """.trimIndent()
    }

    // Default: return response unmodified with placeholder comment
    return """
var resp = requestResponse.response();
if (resp == null) {
    return resp;
}
// TODO: Apply your match and replace logic here
// Prompt: $prompt
return resp;
    """.trimIndent()
}

// ─────────────────────────────────────────────────────────────────────────────
//  LOGGER VIEW FILTER  (VIEW_FILTER / LOGGER)
//  Context object: requestResponse
//  Returns: boolean
// ─────────────────────────────────────────────────────────────────────────────

private fun loggerViewFilterFromPrompt(prompt: String): String {
    if (prompt.contains("return") && prompt.contains("requestResponse")) return prompt

    val p          = prompt.lowercase()
    val method     = extractMethod(p)
    val host       = extractHost(p)
    val statusCode = extractStatusCode(p)
    val mimeType   = extractMimeType(p)

    // Highlight by tool type pattern
    if (p.contains("tool") || p.contains("highlight") || p.contains("color")) {
        return """
var highlights = Map.of(
    ToolType.TARGET,     HighlightColor.RED,
    ToolType.PROXY,      HighlightColor.BLUE,
    ToolType.INTRUDER,   HighlightColor.CYAN,
    ToolType.REPEATER,   HighlightColor.MAGENTA,
    ToolType.EXTENSIONS, HighlightColor.ORANGE,
    ToolType.SCANNER,    HighlightColor.GREEN,
    ToolType.SEQUENCER,  HighlightColor.PINK
);

requestResponse.annotations().setHighlightColor(
    highlights.getOrDefault(requestResponse.toolSource().toolType(), HighlightColor.NONE)
);

return true;
        """.trimIndent()
    }

    val needsResponseGuard = statusCode != null || mimeType != null
    val conditions = mutableListOf<String>()

    method?.let     { conditions += """requestResponse.request().method().equalsIgnoreCase("$it")""" }
    host?.let       { conditions += """requestResponse.request().httpService().host().equals("$it")""" }
    statusCode?.let { conditions += "requestResponse.response().statusCode() == $it" }
    mimeType?.let {
        if (it == "text/html") conditions += "requestResponse.response().statedMimeType() == MimeType.HTML"
    }

    return if (conditions.isEmpty()) {
        """
if (!requestResponse.hasResponse()) {
    return false;
}
return requestResponse.response().statusCode() == 200;
        """.trimIndent()
    } else {
        buildString {
            if (needsResponseGuard) {
                appendLine("if (!requestResponse.hasResponse()) {")
                appendLine("    return false;")
                appendLine("}")
                appendLine()
            }
            append("return ")
            append(conditions.joinToString(" &&\n    "))
            append(";")
        }
    }
}

private fun loggerCaptureFilterFromPrompt(prompt: String): String =
    loggerViewFilterFromPrompt(prompt)

// ─────────────────────────────────────────────────────────────────────────────
//  SITEMAP FILTER  (VIEW_FILTER / SITEMAP)
//  Context object: node
//  Returns: boolean
// ─────────────────────────────────────────────────────────────────────────────

private fun sitemapFilterFromPrompt(prompt: String): String {
    if (prompt.contains("return") && prompt.contains("node")) return prompt

    val p                  = prompt.lowercase()
    val host               = extractHost(p)
    val statusCode         = extractStatusCode(p)
    val mimeType           = extractMimeType(p)
    val wantsParameterized = p.contains("parameterized") || p.contains("has parameters")
    val wantsResponse      = p.contains("response") || p.contains("responded")

    val conditions = mutableListOf<String>()

    // Always guard with hasResponse when filtering on response properties
    if (wantsResponse || statusCode != null || mimeType != null) {
        conditions += "node.requestResponse().hasResponse()"
    }

    host?.let {
        conditions += """node.requestResponse().request().httpService().host().equals("$it")"""
    }
    if (wantsParameterized) {
        conditions += "node.requestResponse().request().hasParameters()"
    }
    mimeType?.let {
        if (it == "text/html")
            conditions += "node.requestResponse().response().statedMimeType() == MimeType.HTML"
    }
    statusCode?.let {
        conditions += "node.requestResponse().response().statusCode() == $it"
    }

    return if (conditions.isEmpty()) {
        "return node.requestResponse().hasResponse();"
    } else {
        "return " + conditions.joinToString(" &&\n    ") + ";"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  REPEATER CUSTOM ACTION  (CUSTOM_ACTION / REPEATER)
//  Context object: requestResponse, httpEditor, api(), logging()
//  Returns: void
// ─────────────────────────────────────────────────────────────────────────────

private fun repeaterCustomActionFromPrompt(prompt: String): String {
    if (prompt.contains("requestResponse") || prompt.contains("httpEditor")) return prompt

    val p = prompt.lowercase()

    // GraphQL introspection
    if (looksLikeGraphQlIntrospectionAction(prompt)) {
        return """
var request = requestResponse.request();
logging().logToOutput("Running GraphQL introspection check");

var introspectionQuery = "{__schema{queryType{name}}}";
var modifiedRequest = request.withParameter(
    burp.api.montoya.http.message.params.HttpParameter.urlParameter("query", introspectionQuery)
);

httpEditor.requestPane().set(modifiedRequest);

var result = api().http().sendRequest(modifiedRequest);
if (!result.hasResponse()) {
    logging().logToOutput("No response received");
    return;
}

var response = result.response();
httpEditor.responsePane().set(response);

var body = response.bodyToString();
if (response.statusCode() == 200 && body.contains("__schema") && !body.contains("\"errors\"")) {
    logging().logToOutput("Introspection is ENABLED");
} else {
    logging().logToOutput("Introspection is NOT enabled");
}
        """.trimIndent()
    }

    // HTTP method test (TRACE, OPTIONS, PUT etc.)
    val method = extractMethod(p)
    if (method != null && (p.contains("test") || p.contains("send") || p.contains("try"))) {
        return """
var req = requestResponse.request();
var httpCode = api().http().sendRequest(req.withMethod("$method")).response().statusCode();
logging().logToOutput("HTTP $method → " + httpCode);
        """.trimIndent()
    }

    // API version downgrade/upgrade
    if (p.contains("version") || p.contains("downgrade") || p.contains("upgrade")) {
        return """
var baserequest = requestResponse.request();
String path = baserequest.path();
Pattern versionPattern = Pattern.compile("/v(\\d+)");
Matcher matcher = versionPattern.matcher(path);
if (!matcher.find()) {
    logging().logToOutput("No version (/v[NUM]) found in path.");
    return;
}
String currentVersion = matcher.group(0);
logging().logToOutput("Detected version: " + currentVersion);

for (int i = 1; i <= 5; i++) {
    String newVersion = "/v" + i;
    if (newVersion.equals(currentVersion)) continue;
    String newPath = path.replace(currentVersion, newVersion);
    logging().logToOutput("Sending: " + newPath);
    var response = api().http().sendRequest(baserequest.withPath(newPath));
    if (response == null || !response.hasResponse()) {
        logging().logToOutput(newVersion + " → No response");
        continue;
    }
    logging().logToOutput(newVersion + " → " + response.response().statusCode());
}
        """.trimIndent()
    }

    // Default skeleton
    return """
var request = requestResponse.request();
logging().logToOutput("Running custom action");
logging().logToOutput("Prompt: $prompt");
httpEditor.requestPane().set(request);
    """.trimIndent()
}

// ─────────────────────────────────────────────────────────────────────────────
//  CUSTOM COLUMN  (CUSTOM_COLUMN / PROXY_HTTP_HISTORY)
//  Context object: requestResponse, utilities
//  Returns: String
// ─────────────────────────────────────────────────────────────────────────────

private fun customColumnFromPrompt(prompt: String): String {
    if (prompt.contains("return") && prompt.contains("requestResponse")) return prompt

    val p = prompt.lowercase()

    // Status code column
    if (p.contains("status")) {
        return """
return requestResponse.hasResponse()
    ? Integer.toString(requestResponse.response().statusCode())
    : "";
        """.trimIndent()
    }

    // GraphQL operation name column
    if (p.contains("graphql") || p.contains("operation")) {
        return """
String requestBody = requestResponse.request().bodyToString();
if (!utilities.jsonUtils().isValidJson(requestBody)) {
    return "";
}
return utilities.jsonUtils().readString(requestBody, "operationName");
        """.trimIndent()
    }

    // JSON field extraction
    val jsonField = extractJsonFieldName(p)
    if (jsonField != null) {
        return """
String requestBody = requestResponse.request().bodyToString();
if (!utilities.jsonUtils().isValidJson(requestBody)) {
    return "";
}
return utilities.jsonUtils().readString(requestBody, "$jsonField");
        """.trimIndent()
    }

    // Default: show path
    return """return requestResponse.request().pathWithoutQuery();"""
}

// ─────────────────────────────────────────────────────────────────────────────
//  SCANNER ACTIVE CHECK  (SCAN_CHECK_ACTIVE_PER_REQUEST / SCANNER)
//  Context object: requestResponse, api(), http
//  Returns: AuditResult
// ─────────────────────────────────────────────────────────────────────────────

private fun scannerActiveFromPrompt(prompt: String): String {
    if (prompt.contains("return") && prompt.contains("AuditResult")) return prompt

    val p = prompt.lowercase()

    // CORS misconfiguration
    if (p.contains("cors")) {
        return """
if (!requestResponse.hasResponse()) {
    return null;
}

var evilOrigin = "https://" + api().utilities().randomUtils().randomString(6) +
    "." + api().utilities().randomUtils().randomString(3);

var rr = http.sendRequest(requestResponse.request().withAddedHeader("Origin", evilOrigin));
if (!rr.hasResponse()) {
    return AuditResult.auditResult();
}

var headers = rr.response().headers().toString().toLowerCase();
var reflected = headers.contains("access-control-allow-origin: " + evilOrigin.toLowerCase());
var credentialed = headers.contains("access-control-allow-credentials: true");

if (reflected) {
    var severity = credentialed
        ? AuditIssueSeverity.HIGH
        : AuditIssueSeverity.MEDIUM;
    return AuditResult.auditResult(
        AuditIssue.auditIssue(
            "CORS: Arbitrary Origin Reflection",
            "The application reflects arbitrary origins. Credentials allowed: " + credentialed,
            "Implement a strict origin allowlist.",
            rr.request().url(),
            severity,
            AuditIssueConfidence.FIRM,
            "", "",
            severity,
            rr
        )
    );
}
return AuditResult.auditResult();
        """.trimIndent()
    }

    // Default scanner skeleton
    return """
if (!requestResponse.hasResponse()) {
    return null;
}

// TODO: implement scan logic for: $prompt
// Send modified request:
// var rr = http.sendRequest(requestResponse.request().withAddedHeader("X-Test", "value"));

// Return an issue if vulnerable:
// return AuditResult.auditResult(
//     AuditIssue.auditIssue(
//         "Issue Name",
//         "Detail",
//         "Remediation",
//         requestResponse.request().url(),
//         AuditIssueSeverity.MEDIUM,
//         AuditIssueConfidence.FIRM,
//         "", "",
//         AuditIssueSeverity.MEDIUM,
//         requestResponse
//     )
// );

return AuditResult.auditResult();
    """.trimIndent()
}

// ─────────────────────────────────────────────────────────────────────────────
//  SIGNAL EXTRACTORS
// ─────────────────────────────────────────────────────────────────────────────

private fun looksLikeGraphQlIntrospectionAction(prompt: String): Boolean =
    prompt.lowercase().let { it.contains("graphql") && it.contains("introspection") }

private fun extractMethod(prompt: String): String? {
    val p = prompt.lowercase()
    return when {
        Regex("""\btrace\b""").containsMatchIn(p)   -> "TRACE"
        Regex("""\bget\b""").containsMatchIn(p)     -> "GET"
        Regex("""\bpost\b""").containsMatchIn(p)    -> "POST"
        Regex("""\bput\b""").containsMatchIn(p)     -> "PUT"
        Regex("""\bdelete\b""").containsMatchIn(p)  -> "DELETE"
        Regex("""\bpatch\b""").containsMatchIn(p)   -> "PATCH"
        Regex("""\bhead\b""").containsMatchIn(p)    -> "HEAD"
        Regex("""\boptions\b""").containsMatchIn(p) -> "OPTIONS"
        else -> null
    }
}

private fun extractMimeType(prompt: String): String? {
    val p = prompt.lowercase()
    return when {
        p.contains("text/html") || p.contains("html response") ||
            p.contains("html only") || p.contains("only html") ||
            Regex("""\bhtml\b""").containsMatchIn(p)        -> "text/html"
        p.contains("application/json") || p.contains("json response") ||
            p.contains("json only") || p.contains("only json") ||
            Regex("""\bjson\b""").containsMatchIn(p)        -> "application/json"
        p.contains("application/javascript") || p.contains("text/javascript") ||
            Regex("""\bjavascript\b""").containsMatchIn(p)  -> "application/javascript"
        p.contains("text/css") ||
            Regex("""\bcss\b""").containsMatchIn(p)         -> "text/css"
        p.contains("application/xml") || p.contains("text/xml") ||
            Regex("""\bxml\b""").containsMatchIn(p)         -> "application/xml"
        p.contains("image only") || p.contains("image response") -> "image"
        else -> null
    }
}

private fun extractParamName(prompt: String): String? {
    val p = prompt.lowercase()
    Regex("""([a-z][a-z0-9_]*)=""").find(p)
        ?.groupValues?.getOrNull(1)?.let { return it }
    Regex("""param(?:eter)?s?\s+(?:named?\s+)?([a-z][a-z0-9_]*)""")
        .find(p)?.groupValues?.getOrNull(1)?.let { return it }
    return null
}

private fun extractHeaderName(prompt: String): String? =
    Regex("""([A-Za-z][A-Za-z0-9\-]+)(?:\s+header|\s*:)""", RegexOption.IGNORE_CASE)
        .find(prompt)?.groupValues?.getOrNull(1)

private fun extractHeaderValue(prompt: String): String? =
    Regex("""(?:value|to|=)\s*["\']?([A-Za-z0-9_.:\-/]+)["\']?""", RegexOption.IGNORE_CASE)
        .find(prompt)?.groupValues?.getOrNull(1)

private fun extractJsonFieldName(prompt: String): String? =
    Regex("""(?:field|key|property|column)\s+["\']?([a-zA-Z0-9_]+)["\']?""", RegexOption.IGNORE_CASE)
        .find(prompt)?.groupValues?.getOrNull(1)

private fun extractQuotedPath(prompt: String): String? =
    Regex(""""(/[^"]+)"""").find(prompt)?.groupValues?.getOrNull(1)

private fun extractPath(prompt: String): String? =
    Regex("(/[-a-zA-Z0-9_./]+)").find(prompt)?.groupValues?.getOrNull(1)

private fun extractHost(prompt: String): String? =
    Regex("""\b([a-zA-Z0-9][-a-zA-Z0-9.]*\.[a-zA-Z]{2,})\b""")
        .find(prompt)?.groupValues?.getOrNull(1)

private fun extractStatusCode(prompt: String): Int? =
    Regex("""\b(20[0-9]|30[0-9]|40[0-9]|50[0-9])\b""")
        .find(prompt)?.groupValues?.getOrNull(1)?.toIntOrNull()

private fun extractNumber(prompt: String): Int? =
    Regex("""\b(\d+)\b""").find(prompt)?.groupValues?.getOrNull(1)?.toIntOrNull()

private fun extractQueryParamName(prompt: String): String? =
    Regex("""parameter\s+([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)
        .find(prompt)?.groupValues?.getOrNull(1)

private fun extractQueryParamValue(prompt: String): String? =
    Regex("""(?:to|=)\s*([a-zA-Z0-9_{}:.\-]+)""", RegexOption.IGNORE_CASE)
        .find(prompt)?.groupValues?.getOrNull(1)