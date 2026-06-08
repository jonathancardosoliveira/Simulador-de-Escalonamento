/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io;

/**
 *
 * @author jonat
 */

import config.Configuracao;
import model.Processo;
import scheduler.FilaEspera;
import scheduler.FilaPronto;
import util.Log;

public class GerenciadorIO implements Runnable {
    @Override
    public void run() {
        while (true) {
            try { Thread.sleep(Configuracao.VELOCIDADE_MS); } catch (InterruptedException e) { return; }
            var iterator = FilaEspera.iterator();
            while (iterator.hasNext()) {
                Processo p = iterator.next();
                synchronized (p) {
                    if (p.tempoRestanteIO > 0) {
                        p.tempoRestanteIO--;
                        if (p.tempoRestanteIO == 0) {
                            p.estado = "READY";
                            FilaEspera.remove(p);
                            FilaPronto.add(p);
                            Log.evento(String.format("Processo P%d: WAITING -> READY (retorno de E/S)", p.id));
                        }
                    }
                }
            }
        }
    }
}
