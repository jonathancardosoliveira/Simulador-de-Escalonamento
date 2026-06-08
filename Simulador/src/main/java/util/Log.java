/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

/**
 *
 * @author jonat
 */

public class Log {
    private static long inicioSimulacaoMs;

    public static void setInicio(long inicio) {
        inicioSimulacaoMs = inicio;
    }

    public static void evento(String msg) {
        double tempo = (System.currentTimeMillis() - inicioSimulacaoMs) / 1000.0;
        System.out.printf("[%.2f] %s\n", tempo, msg);
    }
}
