package sma;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Main {
    public static void main(String[] args) {
        try {
            Runtime rt = Runtime.instance();
            Profile pMain = new ProfileImpl(null, 1099, null);
            ContainerController cc = rt.createMainContainer(pMain);

            // ===========================
            // VENDEDORES DE CARROS
            // ===========================

            String produtosFerrari =
                            "f1:Ferrari488:450000:2;" +
                            "f2:FerrariRoma:350000:3";
            cc.createNewAgent("VendedorFerrari", "sma.AgenteVendedor", new Object[]{"carro", produtosFerrari, "0.9"}).start();

            String produtosLamborghini =
                            "l1:LamborghiniAventador:480000:2;" +
                            "l2:LamborghiniHuracan:400000:4;" +
                            "l3:LamborghiniUrus:300000:3";
            cc.createNewAgent("VendedorLamborghini", "sma.AgenteVendedor", new Object[]{"carro", produtosLamborghini, "0.88"}).start();

            String produtosMercedes =
                            "m1:MercedesAMGGT:220000:5;" +
                            "m2:MercedesClasseS:180000:6;" +
                            "m3:MercedesGLS:150000:4";
            cc.createNewAgent("VendedorMercedes", "sma.AgenteVendedor", new Object[]{"carro", produtosMercedes, "0.85"}).start();

            // ===========================
            // VENDEDORES DE CASAS
            // ===========================

            String produtosCasas1 =
                            "h1:CasaCentro:1200000:1;" +
                            "h2:CasaCampo:600000:1;" +
                            "h3:CasaPraia:750000:1";
            cc.createNewAgent("VendedorCasas1", "sma.AgenteVendedor", new Object[]{"casa", produtosCasas1, "0.9"}).start();

            String produtosCasas2 =
                            "h4:AptoCentro:800000:2;" +
                            "h5:CasaSuburbio:550000:1;" +
                            "h6:Chacara:1100000:1";
            cc.createNewAgent("VendedorCasas2", "sma.AgenteVendedor", new Object[]{"casa", produtosCasas2, "0.9"}).start();

            // ===========================
            // VENDEDORES DE RELÓGIOS
            // ===========================

            String produtosRolex =
                            "r1:RolexDaytona:120000:2;" +
                            "r2:RolexSubmariner:95000:3;" +
                            "r3:RolexDatejust:80000:4";
            cc.createNewAgent("VendedorRolex", "sma.AgenteVendedor", new Object[]{"relogio", produtosRolex, "0.85"}).start();

            // ===========================
            // COMPRADORES
            // ===========================

            String[][] dadosCompradores = {
                    {"Comprador1", "carro", "FerrariRoma", "320000"},
                    {"Comprador2", "carro", "Ferrari488", "470000"},
                    {"Comprador3", "carro", "LamborghiniHuracan", "350000"},
                    {"Comprador4", "carro", "LamborghiniUrus", "280000"},
                    {"Comprador5", "carro", "MercedesAMGGT", "230000"},
                    {"Comprador6", "carro", "MercedesClasseS", "150000"},
                    {"Comprador7", "casa", "CasaCentro", "1000000"},
                    {"Comprador8", "casa", "CasaPraia", "800000"},
                    {"Comprador9", "casa", "Chacara", "1050000"},
                    {"Comprador10", "relogio", "*", "90000"}
            };

            for (String[] dados : dadosCompradores) {
                AgentController comprador = cc.createNewAgent(dados[0], "sma.AgenteComprador", new Object[]{dados[1], dados[2], dados[3]});
                comprador.start();
            }

            Utils.log("ContainerPrincipal: todos os agentes foram iniciados.");

            cc.createNewAgent("AgenteMonitor", "sma.AgenteMonitor", new Object[]{dadosCompradores.length}).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
