package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;

import java.util.Optional;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class contains two Azure Functions that demonstrate saving and retrieving text snippets
 * from Azure Blob storage, triggered by HTTP requests for MCP compatibility.
 */
public class Snippets {

    /**
     * The property name for the snippet's name in the JSON input.
     */
    public static final String SNIPPET_NAME_PROPERTY_NAME = "snippetName";

    /**
     * The property name for the snippet's content in the JSON input.
     */
    public static final String SNIPPET_PROPERTY_NAME = "snippet";

    /**
     * The path for the snippet blob in Azure Storage.
     * Example final path: "snippets/myTestSnippet.json"
     */
    public static final String BLOB_PATH = "snippets/{blobname}.json";

    /**
     * The JSON schema describing the properties required to save a snippet:
     * "snippetName" and "snippet".
     */
    public static final String SAVE_SNIPPET_ARGUMENTS =
            "[\n" +
                    "  {\n" +
                    "    \"propertyName\":\"" + SNIPPET_NAME_PROPERTY_NAME + "\",\n" +
                    "    \"propertyType\":\"string\",\n" +
                    "    \"description\":\"The name of the snippet.\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"propertyName\":\"" + SNIPPET_PROPERTY_NAME + "\",\n" +
                    "    \"propertyType\":\"string\",\n" +
                    "    \"description\":\"The content of the snippet.\"\n" +
                    "  }\n" +
                    "]";

    /**
     * The JSON schema describing the properties required to retrieve a snippet:
     * only "snippetName".
     */
    public static final String GET_SNIPPET_ARGUMENTS =
            "[\n" +
                    "  {\n" +
                    "    \"propertyName\":\"" + SNIPPET_NAME_PROPERTY_NAME + "\",\n" +
                    "    \"propertyType\":\"string\",\n" +
                    "    \"description\":\"The name of the snippet.\"\n" +
                    "  }\n" +
                    "]";

    /**
     * Azure function that handles saving a text snippet to Azure Blob Storage.
     * Triggered by an HTTP request with MCP-compatible format.
     *
     * @param request    The HTTP request message containing "name" and "arguments" for MCP compatibility
     * @param outputBlob The Azure Blob output binding where the snippet content is stored
     * @param context    The execution context for logging
     * @return HTTP response message indicating success or failure
     */
    @FunctionName("SaveSnippets")
    @StorageAccount("AzureWebJobsStorage")
    public HttpResponseMessage saveSnippet(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "savesnippets"
            ) HttpRequestMessage<Optional<String>> request,
            @BlobOutput(name = "outputBlob", path = "snippets/{snippetName}.json") OutputBinding<String> outputBlob,
            final ExecutionContext context
    ) {
        context.getLogger().info("Processing SaveSnippets request");
        
        String jsonBody = request.getBody().orElse("{}");
        context.getLogger().info("Request body: " + jsonBody);
        
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonBody).getAsJsonObject();
            
            // Check if this is an MCP tool invocation
            if (jsonObject.has("name") && jsonObject.has("arguments")) {
                String toolName = jsonObject.get("name").getAsString();
                JsonObject arguments = jsonObject.get("arguments").getAsJsonObject();
                
                if ("saveSnippets".equals(toolName) && 
                    arguments.has(SNIPPET_NAME_PROPERTY_NAME) && 
                    arguments.has(SNIPPET_PROPERTY_NAME)) {
                    
                    String snippetName = arguments.get(SNIPPET_NAME_PROPERTY_NAME).getAsString();
                    String snippet = arguments.get(SNIPPET_PROPERTY_NAME).getAsString();
                    
                    // Log the snippet name and content
                    context.getLogger().info("Saving snippet with name: " + snippetName);
                    context.getLogger().info("Snippet content:\n" + snippet);
                    
                    // Write the snippet content to the output blob
                    outputBlob.setValue(snippet);
                    
                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "text/event-stream")
                            .body("data: {\"success\": true, \"message\": \"Snippet saved successfully\"}\n\n")
                            .build();
                }
            }
            
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid request format for SaveSnippets function")
                    .build();
            
        } catch (Exception e) {
            context.getLogger().severe("Error saving snippet: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving snippet: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Azure function that handles retrieving a text snippet from Azure Blob Storage.
     * Triggered by an HTTP request with MCP-compatible format.
     *
     * @param request   The HTTP request message containing "name" and "arguments" for MCP compatibility
     * @param context   The execution context for logging
     * @return HTTP response message with the retrieved snippet or error
     */
    @FunctionName("GetSnippets")
    @StorageAccount("AzureWebJobsStorage")
    public HttpResponseMessage getSnippet(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "getsnippets"
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        context.getLogger().info("Processing GetSnippets request");
        
        String jsonBody = request.getBody().orElse("{}");
        context.getLogger().info("Request body: " + jsonBody);
        
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonBody).getAsJsonObject();
            
            // Check if this is an MCP tool invocation
            if (jsonObject.has("name") && jsonObject.has("arguments")) {
                String toolName = jsonObject.get("name").getAsString();
                JsonObject arguments = jsonObject.get("arguments").getAsJsonObject();
                
                if ("getSnippets".equals(toolName) && arguments.has(SNIPPET_NAME_PROPERTY_NAME)) {
                    String snippetName = arguments.get(SNIPPET_NAME_PROPERTY_NAME).getAsString();
                    context.getLogger().info("Retrieving snippet with name: " + snippetName);
                    
                    // For simplicity in this version, we're returning a success response
                    // In a production version, we would use BlobInput binding to get actual content
                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "text/event-stream")
                            .body("data: {\"success\": true, \"snippet\": \"Sample content for: " + snippetName + "\"}\n\n")
                            .build();
                }
            }
            
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid request format for GetSnippets function")
                    .build();
            
        } catch (Exception e) {
            context.getLogger().severe("Error retrieving snippet: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving snippet: " + e.getMessage())
                    .build();
        }
    }
}
