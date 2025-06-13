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
                return Files.readString(Paths.get("/Users/ivannovikov/Desktop/SD/SD_big_HW_3/API_Gateway/swagger.yaml"));
            } catch (IOException e) {
                res.status(404);
                return "Swagger YAML file not found";
            }
        });

        // Swagger UI endpoint
        get("/docs", (req, res) -> {
            res.type("text/html");
            return "<!DOCTYPE html>\n" +
                   "<html lang=\"en\">\n" +
                   "<head>\n" +
                   "    <meta charset=\"UTF-8\">\n" +
                   "    <title>API Gateway Documentation</title>\n" +
                   "    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui.css\">\n" +
                   "    <style>body{margin:0;padding:0;}#swagger-ui{max-width:1200px;margin:0 auto;}</style>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "    <div id=\"swagger-ui\"></div>\n" +
                   "    <script src=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-bundle.js\"></script>\n" +
                   "    <script src=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-standalone-preset.js\"></script>\n" +
                   "    <script>\n" +
                   "        window.onload = function() {\n" +
                   "            window.ui = SwaggerUIBundle({\n" +
                   "                url: \"/swagger.yaml\",\n" +
                   "                dom_id: '#swagger-ui',\n" +
                   "                deepLinking: true,\n" +
                   "                presets: [\n" +
                   "                    SwaggerUIBundle.presets.apis,\n" +
                   "                    SwaggerUIStandalonePreset\n" +
                   "                ],\n" +
                   "                layout: \"StandaloneLayout\"\n" +
                   "            });\n" +
                   "        };\n" +
                   "    </script>\n" +
                   "</body>\n" +
                   "</html>";
        });

        // Root redirect
        get("/", (req, res) -> {
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
