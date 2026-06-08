/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package generator;

/**
 *
 * @author jonat
 */

import config.Configuracao;
import model.Processo;
import scheduler.FilaPronto;
import util.Log;
import java.util.*;

public class GeradorProcessos implements Runnable {
    private static List<Processo> todosProcessos = new ArrayList<>();
    private static int total;

    public static void gerarProcessosAleatorios() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Quantos processos? ");
        int quantidade = sc.nextInt();
        total = quantidade;
        Random rand = new Random();
        for (int i = 0; i < quantidade; i++) {
            int id = i+1;
            int chegada = rand.nextInt(10);
            int duracao = 3 + rand.nextInt(8);
            int prioridade = 1 + rand.nextInt(3);
            Processo p = new Processo(id, chegada, duracao, prioridade);
            todosProcessos.add(p);
            System.out.printf("Processo %d: id=%d, chegada=%d, duracao=%d, prioridade=%d\n",
                    i+1, id, chegada, duracao, prioridade);
        }
        todosProcessos.sort(Comparator.comparingInt(p -> p.chegada));
    }

    public static List<Processo> getTodosProcessos() { return todosProcessos; }
    public static int getTotal() { return total; }

    @Override
    public void run() {
        for (int i = 0; i < todosProcessos.size(); i++) {
            Processo p = todosProcessos.get(i);
            if (i == 0) {
                if (p.chegada > 0) {
                    try { Thread.sleep(p.chegada * Configuracao.VELOCIDADE_MS); } catch (InterruptedException e) { return; }
                }
            } else {
                int delta = p.chegada - todosProcessos.get(i-1).chegada;
                if (delta > 0) {
                    try { Thread.sleep(delta * Configuracao.VELOCIDADE_MS); } catch (InterruptedException e) { return; }
                }
            }
            p.chegadaRealMs = System.currentTimeMillis();
            FilaPronto.add(p);
            Log.evento(String.format("Processo P%d chegou (dur=%d) -> estado READY", p.id, p.duracao));
        }
    }
}