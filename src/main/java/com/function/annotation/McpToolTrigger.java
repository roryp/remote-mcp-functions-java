package com.function.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Place this on a parameter whose value would come from MCP tool invocation.
 * </p>
 *
 * <p>
 * The parameter type can be one of the following:
 * </p>
 *
 * <ul>
 * <li>Any native Java types such as int, String, byte[]</li>
 * <li>Any POJO type</li>
 * </ul>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface McpToolTrigger {
    /**
     * The variable name used in function.json.
     * @return The variable name.
     */
    String name() default "mcp";
    
    /**
     * The name of the MCP tool.
     * @return The tool name.
     */
    String toolName() default "";
    
    /**
     * Description of the MCP tool.
     * @return The tool description.
     */
    String description() default "";
    
    /**
     * Properties for the MCP tool.
     * @return The tool properties.
     */
    String toolProperties() default "";

    /**
     * <p>Defines how Functions runtime should treat the parameter value on the request.</p>
     * <p>By default, empty string, blob triggers, and HTTP triggers will be parsed as JSON if 
     * the parameter type is not String or byte[].</p>
     * @return The dataType which will be used by the Functions runtime.
     */
    String dataType() default "";
}