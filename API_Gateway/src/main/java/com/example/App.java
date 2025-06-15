package com.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final String PAYMENTS_SERVICE_URL = "http://localhost:8082";
    private static final String ORDERS_SERVICE_URL = "http://localhost:8081";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main( String[] args )
    {
        port(8080); 

        System.out.println( "API Gateway is starting on port 8080" );

        // Настройка CORS
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        });

        options("/*", (request, response) -> {
            response.status(200);
            return "OK";
        });

        // Basic API endpoints
        get("/api/hello", (req, res) -> "Hello from API Gateway!");

        // Account endpoints
        post("/api/accounts", (req, res) -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PAYMENTS_SERVICE_URL + "/payments/accounts"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(req.body()))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                res.status(response.statusCode());
                res.type("application/json");
                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // Проверка баланса
        get("/api/accounts/:userId/balance", (req, res) -> {
            try {
                String userId = req.params(":userId");
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PAYMENTS_SERVICE_URL + "/payments/accounts/" + userId + "/balance"))
                    .GET()
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                res.status(response.statusCode());
                res.type("application/json");
                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // Пополнение счета
        post("/api/accounts/:userId/deposit", (req, res) -> {
            try {
                String userId = req.params(":userId");
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PAYMENTS_SERVICE_URL + "/payments/accounts/" + userId + "/deposit"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(req.body()))
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                res.status(response.statusCode());
                res.type("application/json");
                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // Order endpoints
        // Создание заказа
        post("/api/orders", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
            
            if (req.requestMethod().equals("OPTIONS")) {
                res.status(200);
                return "";
            }
            
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ORDERS_SERVICE_URL + "/api/orders"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(req.body()))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                res.status(response.statusCode());
                res.type("application/json");
                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // Получение заказов пользователя
        get("/api/users/:userId/orders", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
            
            try {
                String userId = req.params(":userId");
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ORDERS_SERVICE_URL + "/api/users/" + userId + "/orders"))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                res.status(response.statusCode());
                res.type("application/json");
                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // Получение деталей заказа
        get("/api/orders/:orderId", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
            
            try {
                String orderId = req.params(":orderId");
                String userId = req.queryParams("userId");
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ORDERS_SERVICE_URL + "/api/orders/" + orderId + "?userId=" + userId))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                res.status(response.statusCode());
                res.type("application/json");
                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

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
