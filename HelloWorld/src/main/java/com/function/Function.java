package com.function;

import com.microsoft.azure.functions.ExecutionContext;

import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpToolTrigger;


/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
    * This function is triggered by an MCP Tool Trigger.
    * It logs the trigger input and prints "Hello, World!" to the log.
    *
    * @param triggerInput The input from the MCP Tool Trigger. It contains the name of the snippet.
    * @param context The execution context of the function, used for logging.
    */
    @FunctionName("HelloWorld")
    public void logCustomTriggerInput(
    @McpToolTrigger(
        toolName = "getsnippets",
        description = "Gets code snippets from your snippet collection.",
        toolProperties = "[{\"propertyName\":\"snippetname\",\"propertyType\":\"string\",\"description\":\"The name of the snippet.\"}]"
        ) String triggerInput,
        final ExecutionContext context) {
            context.getLogger().info(triggerInput);
            context.getLogger().info("Hello, World!");
        }
}
