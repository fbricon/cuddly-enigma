package com.github.fbricon.cuddlyenigma;

import io.quarkus.runtime.Quarkus;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.settings.capabilities.MicroProfileCapabilityManager;

public class Plugin {
    public static void main(String[] args) {
        Diagnostic d = new Diagnostic();
        Quarkus q = null;
        MicroProfileCapabilityManager mp = null;
    }
}