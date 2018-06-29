/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SearchEngine;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Mohamed
 */
public class Main {

    static DB Database;
    static int Max_Crawled = 5000;
    static double safety = 0.1;
    public static List<Thread> indexerThreads = new LinkedList<Thread>();
    static  int Threads_Count;
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        Database = new DB();
        //Get number of threads from user
//        Scanner input = new Scanner(System.in);
//        System.out.println("Please enter number of threads: ");
//        Threads_Count = input.nextInt();
//        Thread Crawler=new Thread(new Crawler());
//        Crawler.start();
        System.out.println("Indexer started");
//        for(int i=0; i<10; i++) {
	      Thread Indexer=new Thread(new Indexer());
//	      indexerThreads.add(Indexer);
	      Indexer.start();
//        }
//        for(Thread indexerThread : indexerThreads) {
//
//            indexerThread.join();
//        }
	      Indexer.join();
    //    Crawler.join();
        System.out.println("Returned");
    }

}
