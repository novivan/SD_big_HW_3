package com.example.swagger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import spark.Spark;

/**
 * Класс для настройки Swagger UI
 */
public class SwaggerUIServlet {

    private final String swaggerYamlPath;
    private String swaggerYamlContent;

    public SwaggerUIServlet(String swaggerYamlPath) {
        this.swaggerYamlPath = swaggerYamlPath;
        try {
            File file = new File(swaggerYamlPath);
            if (file.exists()) {
                this.swaggerYamlContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                System.out.println("Loaded Swagger YAML from: " + swaggerYamlPath);
            } else {
                System.err.println("Swagger YAML not found at: " + swaggerYamlPath);
                this.swaggerYamlContent = createMinimalSwagger();
            }
        } catch (IOException e) {
            System.err.println("Error reading Swagger YAML: " + e.getMessage());
            this.swaggerYamlContent = createMinimalSwagger();
        }
    }

    private String createMinimalSwagger() {
        return "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: API Gateway\n" +
                "  description: API Gateway\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /api/hello:\n" +
                "    get:\n" +
                "      summary: Hello endpoint\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK";
    }

    public void registerRoutes() {
        // Endpoint для swagger.yaml
        Spark.get("/swagger.yaml", (req, res) -> {
            res.type("application/yaml");
            return swaggerYamlContent;
        });

        // Endpoint для Swagger UI HTML
        Spark.get("/docs", (req, res) -> {
            res.type("text/html");
            return createSwaggerUIHtml();
        });
    }

    private String createSwaggerUIHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>API Documentation</title>\n" +
                "    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui.css\">\n" +
                "    <style>\n" +
                "        html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }\n" +
                "        *, *:before, *:after { box-sizing: inherit; }\n" +
                "        body { margin: 0; padding: 0; }\n" +
                "    </style>\n" +
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
                "                plugins: [\n" +
                "                    SwaggerUIBundle.plugins.DownloadUrl\n" +
                "                ],\n" +
                "                layout: \"BaseLayout\"\n" +
                "            });\n" +
                "        };\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
