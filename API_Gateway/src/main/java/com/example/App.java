package com.example;

import static spark.Spark.get;
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

        System.out.println( "Hello World!" );
        System.out.println("API Gateway is started on port 8080");

        get("/api/hello", (req, res) -> "Hello from API Gateway!");
        // Здесь нет необходимости в бесконечном цикле - Spark сам запускает фоновый поток
        
    }
}
