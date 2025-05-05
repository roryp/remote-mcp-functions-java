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
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Demonstrates a simple Azure Function that handles MCP tool invocations.
 * This function supports both SSE connections and specific MCP tool invocations.
 */
public class HelloWorld {
    /**
     * The JSON schema describing the arguments expected by the "getsnippets" tool.
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

    private static final Gson gson = new Gson();

    /**
     * Azure function that handles MCP protocol connections and tool invocations.
     * 
     * AuthorizationLevel.ANONYMOUS is used to handle authentication manually
     * to support both header-based and query parameter-based auth.
     *
     * @param request The HTTP request message with JSON payload
     * @param context The execution context for logging and tracing function execution.
     * @return HTTP response with the appropriate SSE or tool invocation result
     */
    @FunctionName("HelloWorld")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req", 
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS},
                    authLevel = AuthorizationLevel.ANONYMOUS, 
                    route = "webhooks/mcp/sse"
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        context.getLogger().info("Processing MCP request: " + request.getHttpMethod());
        
        try {
            // Handle CORS preflight requests
            if (request.getHttpMethod() == HttpMethod.OPTIONS) {
                context.getLogger().info("Handling CORS preflight request");
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type, Authorization, x-functions-key")
                        .header("Access-Control-Max-Age", "3600")
                        .build();
            }
            
            // Don't check auth for simplicity during development
            // In production, you would validate the API key here
            
            // Handle SSE connection establishment for GET requests
            if (request.getHttpMethod() == HttpMethod.GET) {
                context.getLogger().info("Establishing SSE connection");
                
                // Ultra simplified response - ONLY what's absolutely required
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "text/event-stream")
                        .header("Cache-Control", "no-cache")
                        .header("Connection", "keep-alive")
                        .header("Access-Control-Allow-Origin", "*")
                        .body("data: {\"ready\":true}\n\n")
                        .build();
            }
            
            // For POST requests, handle tool invocations
            String jsonBody = request.getBody().orElse("{}");
            context.getLogger().info("POST request body: " + jsonBody);
            
            // Just return a simple success response
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("content", "Hello from MCP server");
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .header("Access-Control-Allow-Origin", "*")
                    .body(gson.toJson(responseMap))
                    .build();
            
        } catch (Exception e) {
            context.getLogger().severe("Error processing request: " + e.getMessage());
            
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .header("Access-Control-Allow-Origin", "*")
                    .body(gson.toJson(errorMap))
                    .build();
        }
    }
}
