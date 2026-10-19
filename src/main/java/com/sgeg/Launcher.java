package com.sgeg;

/**
 * Lanzador para ejecutar desde VS Code con F5 / botón Run.
 * Si la clase principal hereda de Application, el JDK exige que JavaFX esté en el
 * module-path y falla con "JavaFX runtime components are missing". Esta clase no
 * hereda de Application, así que evita ese chequeo y simplemente delega en Main.
 */
public class Launcher {

    public static void main(String[] args) {
        Main.main(args);
    }
}
