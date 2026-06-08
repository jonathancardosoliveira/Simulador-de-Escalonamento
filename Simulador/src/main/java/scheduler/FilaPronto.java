/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package scheduler;
/**
 *
 * @author jonat
 */
import model.Processo;
import java.util.LinkedList;
import java.util.Queue;

public class FilaPronto {
    private static Queue<Processo> fila = new LinkedList<>();

    public static void add(Processo p) {
        synchronized (fila) {
            fila.add(p);
            fila.notify();
        }
    }

    public static Processo poll() {
        synchronized (fila) {
            return fila.poll();
        }
    }

    public static boolean isEmpty() {
        synchronized (fila) {
            return fila.isEmpty();
        }
    }

    public static void waitIfEmpty() throws InterruptedException {
        synchronized (fila) {
            while (fila.isEmpty()) {
                fila.wait();
            }
        }
    }
}
