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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class FilaEspera {
    private static Queue<Processo> fila = new LinkedList<>();

    public static void add(Processo p) {
        synchronized (fila) {
            fila.add(p);
        }
    }

    public static Iterator<Processo> iterator() {
        synchronized (fila) {
            return new LinkedList<>(fila).iterator(); // retorna cópia segura
        }
    }

    public static void remove(Processo p) {
        synchronized (fila) {
            fila.remove(p);
        }
    }

    public static boolean isEmpty() {
        synchronized (fila) {
            return fila.isEmpty();
        }
    }
}
