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
        port(8082); 

        System.out.println( "Hello World!" );
        System.out.println("Payments microservice is started on port 8082");

        // Пример маршрута
        get("/payments/hello", (req, res) -> "Hello from Payments Microservice!");
        
        // Здесь нет необходимости в бесконечном цикле - Spark сам запускает фоновый поток
        
    }
}
