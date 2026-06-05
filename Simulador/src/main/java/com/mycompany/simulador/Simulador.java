package com.mycompany.simulador;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Classe Processo que estende Thread
class Processo extends Thread {
    int id;
    int chegada;
    int duracao;
    int prioridade;
    int restante;
    int turnaround;
    int espera;
    int tempoRestanteIO;
    long chegadaRealMs;
    volatile String estado;
    volatile boolean pronto = false;      // sinaliza que o escalonador liberou para execução
    volatile boolean finalizado = false;
    
    Processo(int id, int chegada, int duracao, int prioridade) {
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
        // O processo só executa quando o escalonador o liberar (pronto = true)
        while (!finalizado) {
            synchronized (this) {
                while (!pronto && !finalizado) {
                    try { wait(); } catch (InterruptedException e) { return; }
                }
                if (finalizado) break;
                estado = "RUNNING";
                pronto = false;   // só executa uma vez por liberação
            }
            
            // Executa um burst (quantum ou restante)
            int exec = (Simulador.algoritmo == 0) ? restante : Math.min(restante, Simulador.quantum);
            try {
                Thread.sleep(exec * Simulador.VELOCIDADE_MS);
            } catch (InterruptedException e) {
                break;
            }
            restante -= exec;
            
            // Após executar, decide o próximo estado
            synchronized (this) {
                if (restante <= 0) {
                    estado = "FINISHED";
                    finalizado = true;
                    long fimMs = System.currentTimeMillis();
                    long decorrido = fimMs - chegadaRealMs;
                    turnaround = (int)(decorrido / Simulador.VELOCIDADE_MS + 0.5);
                    espera = turnaround - duracao;
                    Simulador.processosFinalizados.incrementAndGet();
                } else {
                    // Chance de E/S (30%)
                    if (new Random().nextInt(100) < 30) {
                        tempoRestanteIO = 1 + new Random().nextInt(3);
                        estado = "WAITING";
                    } else {
                        estado = "READY";
                    }
                }
            }
            // Notifica o escalonador que terminou este burst
            synchronized (Simulador.lockEscalonador) {
                Simulador.escalonadorPronto = true;
                Simulador.lockEscalonador.notify();
            }
        }
    }
    
    // Método chamado pelo escalonador para liberar o processo
    void liberar() {
        synchronized (this) {
            pronto = true;
            notify();
        }
    }
}

public class Simulador {
    public static final int VELOCIDADE_MS = 1500;
    private static final int MAX_NUCLEOS = 4;
    private static int numNucleos;
    public static int algoritmo;      // 0 = FIFO, 1 = Round Robin
    public static int quantum;
    
    private static List<Processo> todosProcessos = new ArrayList<>();
    private static Queue<Processo> filaProntos = new LinkedList<>();
    private static Queue<Processo> filaEspera = new LinkedList<>();
    public static AtomicInteger processosFinalizados = new AtomicInteger(0);
    
    // Sincronização entre escalonador e processos
    public static Object lockEscalonador = new Object();
    public static boolean escalonadorPronto = false;
    
    // Controle de núcleos: semáforo com número de permissões = numNucleos
    private static Semaphore semaforoNucleos;
    
    private static long inicioSimulacaoMs;
    
    public static void main(String[] args) throws InterruptedException {
        configurarSistema();
        gerarProcessosAleatorios();
        semaforoNucleos = new Semaphore(numNucleos, true);
        
        inicioSimulacaoMs = System.currentTimeMillis();
        
        // Inicia as threads dos processos
        for (Processo p : todosProcessos) {
            p.start();
        }
        
        // Thread geradora de processos (insere na fila de prontos no tempo de chegada)
        Thread gerador = new Thread(() -> geradorProcessos());
        gerador.start();
        
        // Thread escalonadora
        Thread escalonador = new Thread(() -> escalonador());
        escalonador.start();
        
        // Aguarda o gerador terminar (todos processos já inseridos)
        gerador.join();
        
        // Aguarda todos os processos finalizarem
        while (processosFinalizados.get() < todosProcessos.size()) {
            Thread.sleep(10);
        }
        
        // Interrompe escalonador
        escalonador.interrupt();
        escalonador.join();
        
        mostrarEstatisticas();
    }
    
    private static void configurarSistema() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Numero de nucleos (max " + MAX_NUCLEOS + "): ");
        numNucleos = sc.nextInt();
        if (numNucleos < 1) numNucleos = 1;
        if (numNucleos > MAX_NUCLEOS) numNucleos = MAX_NUCLEOS;
        
        System.out.print("Algoritmo (0=FIFO, 1=Round Robin): ");
        algoritmo = sc.nextInt();
        if (algoritmo == 1) {
            System.out.print("Quantum (unidades) [recomendado: 2]: ");
            quantum = sc.nextInt();
            if (quantum < 1) quantum = 1;
        }
    }
    
    private static void gerarProcessosAleatorios() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Quantos processos? ");
        int quantidade = sc.nextInt();
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
        // ordena por chegada (para o gerador)
        todosProcessos.sort(Comparator.comparingInt(p -> p.chegada));
    }
    
    private static void logEvento(String msg) {
        double tempo = (System.currentTimeMillis() - inicioSimulacaoMs) / 1000.0;
        System.out.printf("[%.2f] %s\n", tempo, msg);
    }
    
    // Gerador: insere processos na fila de prontos no momento correto
    private static void geradorProcessos() {
        for (int i = 0; i < todosProcessos.size(); i++) {
            Processo p = todosProcessos.get(i);
            if (i == 0) {
                if (p.chegada > 0) {
                    try { Thread.sleep(p.chegada * VELOCIDADE_MS); } catch (InterruptedException e) { return; }
                }
            } else {
                int delta = p.chegada - todosProcessos.get(i-1).chegada;
                if (delta > 0) {
                    try { Thread.sleep(delta * VELOCIDADE_MS); } catch (InterruptedException e) { return; }
                }
            }
            p.chegadaRealMs = System.currentTimeMillis();
            synchronized (filaProntos) {
                filaProntos.add(p);
                filaProntos.notify();
            }
            logEvento(String.format("Processo P%d chegou (dur=%d) -> estado READY", p.id, p.duracao));
        }
    }
    
    // Thread gerente de E/S
    private static void gerenciarIO() {
        while (true) {
            try { Thread.sleep(VELOCIDADE_MS); } catch (InterruptedException e) { return; }
            synchronized (filaEspera) {
                Iterator<Processo> it = filaEspera.iterator();
                while (it.hasNext()) {
                    Processo p = it.next();
                    synchronized (p) {
                        if (p.tempoRestanteIO > 0) {
                            p.tempoRestanteIO--;
                            if (p.tempoRestanteIO == 0) {
                                p.estado = "READY";
                                it.remove();
                                synchronized (filaProntos) {
                                    filaProntos.add(p);
                                    filaProntos.notify();
                                }
                                logEvento(String.format("Processo P%d: WAITING -> READY (retorno de E/S)", p.id));
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Escalonador: controla qual processo executa e quando
    private static void escalonador() {
        // Inicia thread gerente de E/S
        Thread ioThread = new Thread(() -> gerenciarIO());
        ioThread.start();
        
        while (true) {
            Processo p = null;
            // Obtém próximo processo da fila de prontos (bloqueante)
            synchronized (filaProntos) {
                while (filaProntos.isEmpty() && processosFinalizados.get() < todosProcessos.size()) {
                    try { filaProntos.wait(); } catch (InterruptedException e) { return; }
                }
                if (processosFinalizados.get() >= todosProcessos.size()) break;
                p = filaProntos.poll();
            }
            if (p == null) continue;
            
            // Aguarda um núcleo livre (semáforo)
            try {
                semaforoNucleos.acquire();
            } catch (InterruptedException e) { return; }
            
            // Libera o processo para executar
            p.liberar();
            logEvento(String.format("Processo P%d: READY -> RUNNING (nucleo alocado)", p.id));
            
            // Aguarda o processo terminar o burst ou ser preemptado/E/S
            // O processo notifica o escalonador via lockEscalonador
            synchronized (lockEscalonador) {
                while (!escalonadorPronto && processosFinalizados.get() < todosProcessos.size()) {
                    try { lockEscalonador.wait(); } catch (InterruptedException e) { return; }
                }
                escalonadorPronto = false;
            }
            
            // Após o burst, verifica o estado do processo
            synchronized (p) {
                if (p.estado.equals("FINISHED")) {
                    logEvento(String.format("Processo P%d: RUNNING -> FINISHED (turn=%d, esp=%d)", 
                            p.id, p.turnaround, p.espera));
                    semaforoNucleos.release(); // libera núcleo
                } else if (p.estado.equals("WAITING")) {
                    logEvento(String.format("Processo P%d: RUNNING -> WAITING (E/S de %d unidades)", p.id, p.tempoRestanteIO));
                    synchronized (filaEspera) {
                        filaEspera.add(p);
                    }
                    semaforoNucleos.release();
                } else if (p.estado.equals("READY")) {
                    logEvento(String.format("Processo P%d: RUNNING -> READY (preemptado, restante %d)", p.id, p.restante));
                    // Recoloca na fila de prontos
                    synchronized (filaProntos) {
                        filaProntos.add(p);
                        filaProntos.notify();
                    }
                    semaforoNucleos.release();
                }
            }
        }
        
        // Finaliza a thread de I/O
        ioThread.interrupt();
        try { ioThread.join(); } catch (InterruptedException e) {}
    }
    
    private static void mostrarEstatisticas() {
        double somaTurn = 0, somaEsp = 0;
        System.out.println("\n========== RESULTADOS FINAIS ==========");
        System.out.printf("Algoritmo: %s\n", algoritmo == 0 ? "FIFO" : "Round Robin");
        if (algoritmo == 1) System.out.printf("Quantum: %d\n", quantum);
        System.out.printf("Nucleos: %d\n", numNucleos);
        System.out.printf("Velocidade: %d ms/unidade\n\n", VELOCIDADE_MS);
        System.out.printf("%-10s %-10s %-10s %-12s %s\n", "Processo", "Chegada", "Duracao", "Turnaround", "Espera");
        for (Processo p : todosProcessos) {
            somaTurn += p.turnaround;
            somaEsp += p.espera;
            System.out.printf("P%-7d %-10d %-10d %-12d %d\n", p.id, p.chegada, p.duracao, p.turnaround, p.espera);
        }
        System.out.printf("\nMedia turnaround: %.2f\n", somaTurn / todosProcessos.size());
        System.out.printf("Media espera: %.2f\n", somaEsp / todosProcessos.size());
        System.out.printf("Throughput: %.2f proc/unidade\n", todosProcessos.size() / (somaTurn / todosProcessos.size()));
    }
}