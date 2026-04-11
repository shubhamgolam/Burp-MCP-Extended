package net.portswigger.mcp.tools

enum class BambdaLocation {
    PROXY_HTTP_FILTER,
    PROXY_WS_FILTER,
    LOGGER_VIEW_FILTER,
    LOGGER_CAPTURE_FILTER,
    TARGET_SITE_MAP_FILTER,
    REPEATER_CUSTOM_ACTION,
    CUSTOM_COLUMN,
    MATCH_REPLACE
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

        normalized == "logger" ||
            normalized == "logger view" ||
            normalized == "logger view filter" ->
            BambdaLocation.LOGGER_VIEW_FILTER

        normalized == "logger capture" ||
            normalized == "logger capture filter" ->
            BambdaLocation.LOGGER_CAPTURE_FILTER

        normalized == "target" ||
            normalized == "site map" ||
            normalized == "target filter" ||
            normalized == "target site map" ||
            normalized == "target site map filter" ||
            normalized == "site map filter" ->
            BambdaLocation.TARGET_SITE_MAP_FILTER

        normalized == "repeater" ||
            normalized == "custom action" ||
            normalized == "repeater custom action" ||
            normalized == "repeater action" ->
            BambdaLocation.REPEATER_CUSTOM_ACTION

        normalized == "custom column" ||
            normalized == "column" ->
            BambdaLocation.CUSTOM_COLUMN

        normalized == "match replace" ||
            normalized == "match and replace" ->
            BambdaLocation.MATCH_REPLACE

        else -> throw IllegalArgumentException(
            "Unsupported bambda location '$raw'. Try values like: " +
                "'proxy http', 'proxy websocket', 'logger', 'logger capture', " +
                "'target', 'repeater custom action', 'custom column', 'match and replace'."
        )
    }
}

fun usageHintFor(location: BambdaLocation): String {
    return when (location) {
        BambdaLocation.PROXY_HTTP_FILTER ->
            "Proxy > HTTP history > Filter settings > Script"

        BambdaLocation.PROXY_WS_FILTER ->
            "Proxy > WebSockets history > Filter settings > Script"

        BambdaLocation.LOGGER_VIEW_FILTER ->
            "Logger > View filter > Script"

        BambdaLocation.LOGGER_CAPTURE_FILTER ->
            "Logger > Capture filter > Script"

        BambdaLocation.TARGET_SITE_MAP_FILTER ->
            "Target > Site map > Filter > Script"

        BambdaLocation.REPEATER_CUSTOM_ACTION ->
            "Repeater > Custom actions"

        BambdaLocation.CUSTOM_COLUMN ->
            "A Burp table that supports custom columns, such as HTTP history, Logger, or WebSockets history"

        BambdaLocation.MATCH_REPLACE ->
            "Proxy > Match and replace rules with script support"
    }
}

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
        BambdaLocation.REPEATER_CUSTOM_ACTION ->
            repeaterCustomActionFromPrompt(normalizedPrompt)

        BambdaLocation.TARGET_SITE_MAP_FILTER ->
            targetFilterFromPrompt(normalizedPrompt)

        BambdaLocation.PROXY_HTTP_FILTER ->
            proxyHttpFilterFromPrompt(normalizedPrompt)

        BambdaLocation.LOGGER_VIEW_FILTER ->
            loggerViewFilterFromPrompt(normalizedPrompt)

        BambdaLocation.LOGGER_CAPTURE_FILTER ->
            loggerCaptureFilterFromPrompt(normalizedPrompt)

        BambdaLocation.PROXY_WS_FILTER ->
            proxyWsFilterFromPrompt(normalizedPrompt)

        BambdaLocation.CUSTOM_COLUMN ->
            customColumnFromPrompt(normalizedPrompt)

        BambdaLocation.MATCH_REPLACE ->
            matchReplaceFromPrompt(normalizedPrompt)
    }

    return header + body
}

private fun repeaterCustomActionFromPrompt(prompt: String): String {
    if (looksLikeGraphQlIntrospectionAction(prompt)) {
        return """
            var request = requestResponse.request();

            logging.logToOutput("Running GraphQL introspection custom action");

            var queryParam = request.parameters()
                .firstOrNull { it.name().equals("query", ignoreCase = true) };

            if (queryParam == null) {
                logging.logToOutput("No GraphQL query URL parameter found");
                return;
            }

            var introspectionQuery = "{__schema{queryType{name}}}";

            var modifiedRequest = request.withParameter(
                burp.api.montoya.http.message.params.HttpParameter.urlParameter("query", introspectionQuery)
            );

            httpEditor.requestPane().set(modifiedRequest);

            var requestResponseResult = api().http().sendRequest(modifiedRequest);
            if (!requestResponseResult.hasResponse()) {
                logging.logToOutput("No response received");
                return;
            }

            var response = requestResponseResult.response();
            httpEditor.responsePane().set(response);

            var responseText = response.toString();
            var hasSchemaData = responseText.contains("__schema") || responseText.contains("queryType");
            var hasErrors = responseText.contains("\"errors\"");

            if (response.statusCode() == 200 && hasSchemaData && !hasErrors) {
                logging.logToOutput("Introspection is enabled");
            } else {
                logging.logToOutput("Introspection is not enabled");
            }
        """.trimIndent()
    }

    val path = extractQuotedPath(prompt) ?: extractPath(prompt)
    val queryParamName = extractQueryParamName(prompt)
    val queryParamValue = extractQueryParamValue(prompt)

    if (prompt.contains("add") && prompt.contains("query") && queryParamName != null && queryParamValue != null) {
        val pathCheck = if (path != null) {
            """
            if (!"$path".equals(request.pathWithoutQuery())) {
                logging.logToOutput("Path did not match $path");
                return;
            }
            
            """.trimIndent()
        } else {
            ""
        }

        return """
            var request = requestResponse.request();

            logging.logToOutput("Running custom action");

            $pathCheck
            var modifiedRequest = request.withParameter(
                burp.api.montoya.http.message.params.HttpParameter.urlParameter("$queryParamName", "$queryParamValue")
            );

            httpEditor.requestPane().set(modifiedRequest);

            var response = api().http().sendRequest(modifiedRequest).response();
            httpEditor.responsePane().set(response);

            logging.logToOutput("Request sent with updated query parameter");
        """.trimIndent()
    }

    return """
        var request = requestResponse.request();

        logging.logToOutput("Running custom action");
        logging.logToOutput("Prompt: $prompt");

        // TODO: Implement the requested custom action logic.
        // This is a Repeater Custom Action template.
        httpEditor.requestPane().set(request);
    """.trimIndent()
}

private fun targetFilterFromPrompt(prompt: String): String {
    val host = extractHost(prompt)
    val wantsParameterized = prompt.contains("parameterized") || prompt.contains("has parameters")
    val statusCode = extractStatusCode(prompt)
    val wantsHtml = prompt.contains("html")

    if (host != null || wantsParameterized || statusCode != null || wantsHtml) {
        val conditions = mutableListOf<String>()

        conditions += "node.requestResponse().hasResponse()"

        if (host != null) {
            conditions += "node.requestResponse().request().httpService().host().equals(\"$host\")"
        }

        if (wantsParameterized) {
            conditions += "node.requestResponse().request().hasParameters()"
        }

        if (wantsHtml) {
            conditions += "node.requestResponse().response().statedMimeType() == burp.api.montoya.http.message.MimeType.HTML"
        }

        if (statusCode != null) {
            conditions += "node.requestResponse().response().statusCode() == $statusCode"
        }

        return "return " + conditions.joinToString("\n    && ") + ";"
    }

    return """
        if (!node.requestResponse().hasResponse()) {
            return false;
        }

        // TODO: Implement the requested Target Site map filter logic.
        return node.requestResponse().response().statusCode() == 200;
    """.trimIndent()
}

private fun proxyHttpFilterFromPrompt(prompt: String): String {
    val path = extractQuotedPath(prompt) ?: extractPath(prompt)
    val host = extractHost(prompt)
    val statusCode = extractStatusCode(prompt)

    if (path != null && host == null && statusCode == null) {
        return """
            String path = requestResponse.request().pathWithoutQuery();
            return "$path".equals(path);
        """.trimIndent()
    }

    val conditions = mutableListOf<String>()
    if (host != null) {
        conditions += "requestResponse.request().httpService().host().equals(\"$host\")"
    }
    if (path != null) {
        conditions += "requestResponse.request().pathWithoutQuery().equals(\"$path\")"
    }
    if (statusCode != null) {
        conditions += "requestResponse.hasResponse()"
        conditions += "requestResponse.response().statusCode() == $statusCode"
    }

    if (conditions.isNotEmpty()) {
        return "return " + conditions.joinToString("\n    && ") + ";"
    }

    return """
        // TODO: Implement the requested Proxy HTTP filter logic.
        return requestResponse.request().method().equals("GET");
    """.trimIndent()
}

private fun loggerViewFilterFromPrompt(prompt: String): String {
    val host = extractHost(prompt)
    val statusCode = extractStatusCode(prompt)

    val conditions = mutableListOf<String>()
    if (statusCode != null) {
        conditions += "requestResponse.hasResponse()"
        conditions += "requestResponse.response().statusCode() == $statusCode"
    }
    if (host != null) {
        conditions += "requestResponse.request().httpService().host().equals(\"$host\")"
    }

    if (conditions.isNotEmpty()) {
        return "return " + conditions.joinToString("\n    && ") + ";"
    }

    return """
        if (!requestResponse.hasResponse()) {
            return false;
        }

        // TODO: Implement the requested Logger view filter logic.
        return requestResponse.response().statusCode() == 200;
    """.trimIndent()
}

private fun loggerCaptureFilterFromPrompt(prompt: String): String {
    return loggerViewFilterFromPrompt(prompt)
}

private fun proxyWsFilterFromPrompt(prompt: String): String {
    if (prompt.contains("server to client") || prompt.contains("server-to-client")) {
        return """
            return message.direction() == burp.api.montoya.websocket.Direction.SERVER_TO_CLIENT;
        """.trimIndent()
    }

    val length = extractNumber(prompt)
    if (length != null && (prompt.contains("length") || prompt.contains("payload"))) {
        return """
            return message.payload().length() > $length;
        """.trimIndent()
    }

    return """
        // TODO: Implement the requested Proxy WebSocket filter logic.
        return message.payload().length() > 0;
    """.trimIndent()
}

private fun customColumnFromPrompt(prompt: String): String {
    if (prompt.contains("path")) {
        return """return requestResponse.request().pathWithoutQuery();"""
    }

    if (prompt.contains("status")) {
        return """
            return requestResponse.hasResponse()
                ? Integer.toString(requestResponse.response().statusCode())
                : "";
        """.trimIndent()
    }

    return """
        // TODO: Implement the requested custom column logic.
        return requestResponse.request().pathWithoutQuery();
    """.trimIndent()
}

private fun matchReplaceFromPrompt(prompt: String): String {
    return """
        // TODO: Implement the requested Match and Replace logic.
        // Prompt: $prompt
        return false;
    """.trimIndent()
}

private fun looksLikeGraphQlIntrospectionAction(prompt: String): Boolean {
    val p = prompt.lowercase()
    return p.contains("graphql") && p.contains("introspection")
}

private fun extractQuotedPath(prompt: String): String? {
    val regex = Regex("\"(/[^\"]+)\"")
    return regex.find(prompt)?.groupValues?.getOrNull(1)
}

private fun extractPath(prompt: String): String? {
    val regex = Regex("(/[-a-zA-Z0-9_./]+)")
    return regex.find(prompt)?.groupValues?.getOrNull(1)
}

private fun extractHost(prompt: String): String? {
    val regex = Regex("\\b([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b")
    return regex.find(prompt)?.groupValues?.getOrNull(1)
}

private fun extractStatusCode(prompt: String): Int? {
    val regex = Regex("\\b(20[0-9]|30[0-9]|40[0-9]|50[0-9])\\b")
    return regex.find(prompt)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

private fun extractNumber(prompt: String): Int? {
    val regex = Regex("\\b(\\d+)\\b")
    return regex.find(prompt)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

private fun extractQueryParamName(prompt: String): String? {
    val regex = Regex("""parameter\s+([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)
    return regex.find(prompt)?.groupValues?.getOrNull(1)
}

private fun extractQueryParamValue(prompt: String): String? {
    val regex = Regex("""(?:to|=)\s*([a-zA-Z0-9_{}:.-]+)""", RegexOption.IGNORE_CASE)
    return regex.find(prompt)?.groupValues?.getOrNull(1)
}