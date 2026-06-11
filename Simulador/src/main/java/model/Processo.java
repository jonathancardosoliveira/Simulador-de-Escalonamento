/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author jonat
 */
import scheduler.FilaPronto;
import scheduler.FilaEspera;
import config.Configuracao;
import scheduler.Escalonador;
import java.util.Random;

public class Processo extends Thread {
    public int id;
    public int chegada;
    public int duracao;
    public int prioridade;
    public int restante;
    public int turnaround;
    public int espera;
    public int tempoRestanteIO;
    public long chegadaRealMs;
    public volatile String estado;
    private volatile boolean pronto = false;
    public volatile boolean finalizado = false;

    public Processo(int id, int chegada, int duracao, int prioridade) {
        this.id = id;
        this.chegada = chegada;
        this.duracao = duracao;
        this.prioridade = prioridade;
        this.restante = duracao;
        this.tempoRestanteIO = 0;
        this.estado = "READY";
    }

    @Override
    public void run() {
        while (!finalizado) {
            synchronized (this) {
                while (!pronto && !finalizado) {
                    try { wait(); } catch (InterruptedException e) { return; }
                }
                if (finalizado) break;
                estado = "RUNNING";
                pronto = false;
            }

            int exec = (Configuracao.algoritmo == 0) ? restante : Math.min(restante, Configuracao.quantum);
            try {
                Thread.sleep(exec * Configuracao.VELOCIDADE_MS);
            } catch (InterruptedException e) {
                break;
            }
            restante -= exec;

            synchronized (this) {
                if (restante <= 0) {
                    estado = "FINISHED";
                    finalizado = true;
                    long fimMs = System.currentTimeMillis();
                    long decorrido = fimMs - chegadaRealMs;
                    turnaround = (int)(decorrido / Configuracao.VELOCIDADE_MS + 0.5);
                    espera = turnaround - duracao;
                    Escalonador.incrementarFinalizados();
                    Escalonador.liberarNucleo();  
                } else {
                    if (new Random().nextInt(100) < 30) {
                        tempoRestanteIO = 1 + new Random().nextInt(3);
                        estado = "WAITING";
                        FilaEspera.add(this);
                        Escalonador.liberarNucleo();
                    } else {
                        estado = "READY";
                        FilaPronto.add(this);    
                        Escalonador.liberarNucleo();
                    }
                }
            }
        }
    }

    public void liberar() {
        synchronized (this) {
            pronto = true;
            notify();
        }
    }
}

