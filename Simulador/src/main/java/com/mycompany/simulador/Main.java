package com.mycompany.simulador;

import io.GerenciadorIO;
import config.Configuracao;
import generator.GeradorProcessos;
import model.Processo;
import scheduler.Escalonador;
import util.Log;
import util.Estatisticas;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        configurarSistema();
        GeradorProcessos.gerarProcessosAleatorios();

        Log.setInicio(System.currentTimeMillis());

        // Inicia todos os processos (estão em estado WAITING, aguardando liberação)
        for (Processo p : GeradorProcessos.getTodosProcessos()) {
            p.start();
        }

        // Thread que gera os processos no tempo de chegada
        Thread gerador = new Thread(new GeradorProcessos());
        gerador.start();

        // Thread que gerencia a fila de E/S
        Thread ioThread = new Thread(new GerenciadorIO());
        ioThread.start();

        // Thread escalonadora (despachante)
        Thread escalonador = new Thread(new Escalonador(Configuracao.numNucleos, GeradorProcessos.getTotal()));
        escalonador.start();

        // Aguarda o gerador terminar de inserir todos os processos
        gerador.join();

        // Aguarda todos os processos finalizarem
        while (Escalonador.getFinalizados() < GeradorProcessos.getTotal()) {
            Thread.sleep(10);
        }

        // Encerra as threads auxiliares
        escalonador.interrupt();
        ioThread.interrupt();
        escalonador.join();
        ioThread.join();

        Estatisticas.exibir();
    }

    private static void configurarSistema() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Numero de nucleos (max " + Configuracao.MAX_NUCLEOS + "): ");
        Configuracao.numNucleos = sc.nextInt();
        if (Configuracao.numNucleos < 1) Configuracao.numNucleos = 1;
        if (Configuracao.numNucleos > Configuracao.MAX_NUCLEOS) Configuracao.numNucleos = Configuracao.MAX_NUCLEOS;

        System.out.print("Algoritmo (0=FIFO, 1=Round Robin): ");
        Configuracao.algoritmo = sc.nextInt();
        if (Configuracao.algoritmo == 1) {
            System.out.print("Quantum (unidades) [recomendado: 2]: ");
            Configuracao.quantum = sc.nextInt();
            if (Configuracao.quantum < 1) Configuracao.quantum = 1;
        }
    }
}