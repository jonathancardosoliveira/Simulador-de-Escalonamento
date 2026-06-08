/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package scheduler;

/**
 *
 * @author jonat
 */

import config.Configuracao;
import model.Processo;
import util.Log;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Escalonador implements Runnable {
    private static Semaphore semaforoNucleos;
    private static AtomicInteger processosFinalizados = new AtomicInteger(0);
    private static int totalProcessos;

    public Escalonador(int numNucleos, int total) {
        semaforoNucleos = new Semaphore(numNucleos, true);
        totalProcessos = total;
    }

    public static void incrementarFinalizados() {
        processosFinalizados.incrementAndGet();
    }

    public static int getFinalizados() {
        return processosFinalizados.get();
    }

    public static void liberarNucleo() {
        semaforoNucleos.release();
    }

    @Override
    public void run() {
        while (true) {
            try {
                FilaPronto.waitIfEmpty();
                if (processosFinalizados.get() >= totalProcessos) break;
            } catch (InterruptedException e) {
                break;
            }

            Processo p = FilaPronto.poll();
            if (p == null) continue;

            try {
                semaforoNucleos.acquire();   // aguarda núcleo livre
            } catch (InterruptedException e) {
                break;
            }

            p.liberar();
            int ocupados = Configuracao.numNucleos - semaforoNucleos.availablePermits();
            Log.evento(String.format("Processo P%d: READY -> RUNNING (nucleo alocado, %d ocupados)",
                    p.id, ocupados));
        }
    }
}