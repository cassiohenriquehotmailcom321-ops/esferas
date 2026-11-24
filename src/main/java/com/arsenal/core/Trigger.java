package com.arsenal.core;

/** Forma de ativar uma habilidade. */
public enum Trigger {
    RIGHT("Botão Direito"),
    SNEAK_RIGHT("Shift + Botão Direito"),
    SWAP("Tecla F");

    private final String label;

    Trigger(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
