# Simulador de Escalonamento de Processos com Threads

## 📌 Descrição

Este projeto implementa um **simulador de escalonamento de processos** utilizando **threads em Java**, conforme os requisitos da disciplina de Sistemas Operacionais. O simulador permite:

- Representar **cada processo como uma thread**.
- Controlar a execução simultânea por meio de um **semáforo** que limita o número de processos ativos ao número de núcleos configurado.
- Gerenciar **filas de prontos (READY) e de espera (WAITING)** com sincronização (`synchronized`, `wait/notify`).
- Executar dois algoritmos de escalonamento: **FIFO (FCFS)** e **Round Robin (RR)**.
- Simular **operação de E/S** (estado WAITING) com uma thread gerente de I/O.
- Exibir **logs detalhados** com transições de estado e **estatísticas finais** (turnaround médio, espera média, throughput).

O projeto foi desenvolvido na **IDE NetBeans** com **Java 11+** e utiliza apenas bibliotecas padrão da linguagem.

---
