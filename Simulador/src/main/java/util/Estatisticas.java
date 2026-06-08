/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

/**
 *
 * @author jonat
 */
import config.Configuracao;
import generator.GeradorProcessos;
import model.Processo;

public class Estatisticas {
    public static void exibir() {
        double somaTurn = 0, somaEsp = 0;
        System.out.println("\n========== RESULTADOS FINAIS ==========");
        System.out.printf("Algoritmo: %s\n", Configuracao.algoritmo == 0 ? "FIFO" : "Round Robin");
        if (Configuracao.algoritmo == 1) System.out.printf("Quantum: %d\n", Configuracao.quantum);
        System.out.printf("Nucleos: %d\n", Configuracao.numNucleos);
        System.out.printf("Velocidade: %d ms/unidade\n\n", Configuracao.VELOCIDADE_MS);
        System.out.printf("%-10s %-10s %-10s %-12s %s\n", "Processo", "Chegada", "Duracao", "Turnaround", "Espera");
        for (Processo p : GeradorProcessos.getTodosProcessos()) {
            somaTurn += p.turnaround;
            somaEsp += p.espera;
            System.out.printf("P%-7d %-10d %-10d %-12d %d\n", p.id, p.chegada, p.duracao, p.turnaround, p.espera);
        }
        System.out.printf("\nMedia turnaround: %.2f\n", somaTurn / GeradorProcessos.getTotal());
        System.out.printf("Media espera: %.2f\n", somaEsp / GeradorProcessos.getTotal());
        System.out.printf("Throughput: %.2f proc/unidade\n", GeradorProcessos.getTotal() / (somaTurn / GeradorProcessos.getTotal()));
    }
}
