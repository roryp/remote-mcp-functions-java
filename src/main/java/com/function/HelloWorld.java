package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Demonstrates a simple Azure Function that prints a provided string and logs "Hello, World!".
 * This function is triggered by an HTTP request, handling MCP tool invocations.
 */
public class HelloWorld {
    /**
     * The JSON schema describing the arguments expected by the "getsnippets" tool.
     * In this example, it expects one property named "triggerInput" which is a string.
     */
    public static final String ARGUMENTS = """
        [
           {
              "propertyName": "triggerInput",
              "propertyType": "string",
              "description": "input string"
           }
        ]
    """;

    /**
     * Azure function that:
     * <ul>
     *   <li>Logs the {@code triggerInput} provided by the MCP tool.</li>
     *   <li>Logs "Hello, World!" to demonstrate a simple response.</li>
     * </ul>
     *
     * @param request  The HTTP request message with JSON payload
     * @param context  The execution context for logging and tracing function execution.
     * @return HTTP response with the tool invocation result
     */
    @FunctionName("HelloWorld")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req", 
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION, 
                    route = "webhooks/mcp/sse"
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        context.getLogger().info("Processing MCP request");
        
        try {
            // Handle SSE connection establishment for GET requests
            if (request.getHttpMethod() == HttpMethod.GET) {
                context.getLogger().info("Establishing SSE connection");
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "text/event-stream")
                        .header("Cache-Control", "no-cache")
                        .header("Connection", "keep-alive")
                        // Important: Only include data: with JSON and double newlines
                        // No other text or content should be in the response
                        .body("data: {\"ready\":true}\n\n")
                        .build();
            }
            
            // For POST requests, handle tool invocations
            String jsonBody = request.getBody().orElse("{}");
            context.getLogger().info("Request body: " + jsonBody);
            
            JsonObject jsonObject = JsonParser.parseString(jsonBody).getAsJsonObject();
            
            // Check if this is a valid MCP tool invocation
            if (jsonObject.has("name") && jsonObject.has("arguments")) {
                String toolName = jsonObject.get("name").getAsString();
                JsonObject arguments = jsonObject.get("arguments").getAsJsonObject();
                
                if ("getsnippets".equals(toolName) && arguments.has("triggerInput")) {
                    String triggerInput = arguments.get("triggerInput").getAsString();
                    context.getLogger().info("MCP Tool: " + toolName);
                    context.getLogger().info("Trigger input: " + triggerInput);
                    
                    // Create properly structured response according to MCP protocol
                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "application/json")  // Use JSON content type for POST responses
                            .body("{\"content\":\"" + triggerInput + "\"}")
                            .build();
                }
            }
            
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Invalid MCP tool request format\"}")
                    .build();
            
        } catch (Exception e) {
            context.getLogger().severe("Error processing request: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
