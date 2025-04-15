# MCP Snippets Functions

This repository demonstrates two Azure Functions that leverage the MCP (Microsoft Custom Platform) Tool Trigger annotations to save and retrieve code snippets from Azure Blob Storage, as well as a simple “HelloWorld” function that logs a message.

## Contents

1. **`HelloWorld` Function**
    - File: [`Function.java` (HelloWorld)](./HelloWorld/src/main/java/com/function/Function.java)
    - Triggered by an MCP tool named `getsnippets`.
    - Logs the `triggerInput` it receives from the MCP tool, then logs “Hello, World!”.

2. **`SaveSnippets` and `GetSnippets` Functions**
    - File: [`Function.java` (SaveSnippets and GetSnippets)](./Snippets/src/main/java/com/function/Function.java)
    - `SaveSnippets`:
        - Triggered by an MCP tool named `saveSnippets`.
        - Accepts two parameters in JSON (`snippetName` and `snippet`).
        - Saves the snippet content to Azure Blob Storage, under `snippets/<snippetName>.json`.
    - `GetSnippets`:
        - Triggered by an MCP tool named `getSnippets`.
        - Accepts a single parameter `snippetName`.
        - Retrieves the snippet content from Blob Storage and logs it.

## How It Works
#### Important
To use the `@McpToolTrigger` annotation you must use a `azure.functions.java.library.version` that is set to `3.1.1-alpha` or above in your function's pom.xml
```xml
<azure.functions.java.library.version>3.1.1-alpha</azure.functions.java.library.version>
```

Also, your host.json file must include the experimental extension bundle:
```json
"extensionBundle": {
  "id": "Microsoft.Azure.Functions.ExtensionBundle.Experimental",
  "version": "[4.*, 5.0.0)"
}
```

### 1. MCP Tool Trigger


Each Azure Function uses the `@McpToolTrigger` annotation to define:
- **toolName**: The unique name of the MCP tool (e.g., `saveSnippets`, `getSnippets`).
- **description**: Short description for documentation.
- **toolProperties**: JSON schema describing the expected arguments. This tells the MCP tool what properties to provide at runtime.

Example (from `SaveSnippets`):
```java
@McpToolTrigger(
    toolName = "saveSnippets",
    description = "Saves a text snippet to your snippets collection.",
    toolProperties = SAVE_SNIPPET_ARGUMENTS
)
```

### 2. Reading Arguments

The `toolArguments` parameter in each function is a JSON string containing the arguments defined in `toolProperties`.
- For “SaveSnippets,” the JSON includes `"snippetName"` and `"snippet"`.
- For “GetSnippets,” the JSON includes `"snippetName"`.
- For “HelloWorld,” the JSON includes `"triggerInput"`.

Gson’s `JsonParser` is used to extract these fields from the JSON within each function.

### 3. Blob Storage

For the snippet-related functions:
- `@BlobOutput`: Writes the snippet content to a blob path (`snippets/{mcptoolargs.snippetName}.json`).
- `@BlobInput`: Reads the snippet content from the same path.
- `@StorageAccount("AzureWebJobsStorage")`: Tells the function to use the default storage account connection (replace with your actual storage connection string in local settings or environment variables).

### 4. Building & Deploying

1. **Prerequisites**:
    - Java 8+ (preferably Java 17 if supported by your Azure Functions runtime).
    - [Azure Functions Core Tools](https://docs.microsoft.com/azure/azure-functions/functions-run-local) installed, if you want to run locally.
    - A valid Azure Storage connection string in `local.settings.json` or the Azure Portal for the `AzureWebJobsStorage` setting.
      - If you are running locally keep the default value that is provided (`UseDevelopmentStorage=true`) and ensure that [AzureRite](https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite?tabs=visual-studio%2Cblob-storage) blob storage emulator is installed and running. 

2. **Run Locally** (using Maven, for example):
   ```bash
   mvn clean package
   mvn azure-functions:run
   ```
   This starts the functions on your local machine, accessible on their default endpoints. The MCP tool triggers might be tested differently, depending on your environment.

3. **Deploy**:
   ```bash
   mvn azure-functions:deploy
   ```
   Ensure your Azure credentials are set up (e.g., via `az login`) and that `pom.xml` is configured with your Azure Function App name.