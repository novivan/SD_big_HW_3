package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.awaitInitialization;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.port;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        port(8080); 

        System.out.println( "API Gateway is starting on port 8080" );

        // Basic API endpoints
        get("/api/hello", (req, res) -> "Hello from API Gateway!");

        // Swagger YAML endpoint
        get("/swagger.yaml", (req, res) -> {
            res.type("application/yaml");
            try {
                String yamlContent = Files.readString(Paths.get("swagger.yaml"));
                System.out.println("Successfully loaded swagger.yaml");
                return yamlContent;
            } catch (IOException e) {
                System.err.println("Failed to load swagger.yaml: " + e.getMessage());
                e.printStackTrace();
                res.status(404);
                return "Swagger YAML file not found";
            }
        });

        // Swagger UI endpoint
        get("/docs", (req, res) -> {
            res.type("text/html");
            String html = "<!DOCTYPE html>\n" +
                   "<html lang=\"en\">\n" +
                   "<head>\n" +
                   "    <meta charset=\"UTF-8\">\n" +
                   "    <title>API Gateway Documentation</title>\n" +
                   "    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui.css\">\n" +
                   "    <style>\n" +
                   "        body { margin: 0; padding: 0; }\n" +
                   "        #swagger-ui { max-width: 1200px; margin: 0 auto; }\n" +
                   "    </style>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "    <div id=\"swagger-ui\"></div>\n" +
                   "    <script src=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-bundle.js\"></script>\n" +
                   "    <script src=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-standalone-preset.js\"></script>\n" +
                   "    <script>\n" +
                   "        window.onload = function() {\n" +
                   "            console.log('Initializing Swagger UI...');\n" +
                   "            window.ui = SwaggerUIBundle({\n" +
                   "                url: window.location.origin + '/swagger.yaml',\n" +
                   "                dom_id: '#swagger-ui',\n" +
                   "                deepLinking: true,\n" +
                   "                presets: [\n" +
                   "                    SwaggerUIBundle.presets.apis,\n" +
                   "                    SwaggerUIStandalonePreset\n" +
                   "                ],\n" +
                   "                plugins: [\n" +
                   "                    SwaggerUIBundle.plugins.DownloadUrl\n" +
                   "                ],\n" +
                   "                layout: \"StandaloneLayout\",\n" +
                   "                onComplete: function() {\n" +
                   "                    console.log('Swagger UI initialization complete');\n" +
                   "                }\n" +
                   "            });\n" +
                   "        };\n" +
                   "    </script>\n" +
                   "</body>\n" +
                   "</html>";
            System.out.println("Serving Swagger UI HTML");
            return html;
        });

        // Root redirect
        get("/", (req, res) -> {
            res.redirect("/docs");
            return null;
        });

        // Альтернативный путь к Swagger UI
        get("/swagger-ui", (req, res) -> {
            res.redirect("/docs");
            return null;
        });

        // Альтернативный путь к Swagger UI
        get("/swagger", (req, res) -> {
            res.redirect("/docs");
            return null;
        });

        // 404 handler
        notFound((req, res) -> {
            res.type("application/json");
            return "{\"error\":\"Not Found\",\"path\":\"" + req.pathInfo() + "\"}";
        });

        // Wait for all routes to initialize
        awaitInitialization();
        System.out.println("API Gateway is ready at http://localhost:8080/docs");
    }
}
