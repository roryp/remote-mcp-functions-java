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
     * @param request The HTTP request message with JSON payload
     * @param context The execution context for logging and tracing function execution.
     * @return HTTP response with the appropriate SSE or tool invocation result
     */
    @FunctionName("HelloWorld")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req", 
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS},
                    authLevel = AuthorizationLevel.FUNCTION, 
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
            
            // Handle SSE connection establishment for GET requests
            if (request.getHttpMethod() == HttpMethod.GET) {
                context.getLogger().info("Establishing SSE connection");
                
                // Very simple response with minimal headers and payload
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
            
            JsonObject jsonObject = JsonParser.parseString(jsonBody).getAsJsonObject();
            
            // Check if this is a valid MCP tool invocation
            if (jsonObject.has("name") && jsonObject.has("arguments")) {
                String toolName = jsonObject.get("name").getAsString();
                JsonObject arguments = jsonObject.get("arguments").getAsJsonObject();
                
                if ("getsnippets".equals(toolName) && arguments.has("triggerInput")) {
                    String triggerInput = arguments.get("triggerInput").getAsString();
                    context.getLogger().info("MCP Tool: " + toolName);
                    context.getLogger().info("Trigger input: " + triggerInput);
                    
                    // Create response using Gson to ensure proper JSON formatting
                    Map<String, Object> responseMap = new HashMap<>();
                    responseMap.put("content", triggerInput);
                    
                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .header("Access-Control-Allow-Origin", "*")
                            .body(gson.toJson(responseMap))
                            .build();
                }
            }
            
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Invalid MCP tool request format");
            
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .header("Access-Control-Allow-Origin", "*")
                    .body(gson.toJson(errorMap))
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
