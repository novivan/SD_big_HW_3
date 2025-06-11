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
        port(8081); 

        System.out.println( "Hello World!" );
        System.out.println("Orders microservice is started on port 8081");

        // Пример маршрута
        get("/orders/hello", (req, res) -> "Hello from Orders Microservice!");

        // Здесь нет необходимости в бесконечном цикле - Spark сам запускает фоновый поток
        
    }
}
