package sma;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgenteMonitor extends Agent {
    private int totalCompradores;
    private int finalizados = 0;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            totalCompradores = Integer.parseInt(args[0].toString());
        }

        addBehaviour(new jade.core.behaviours.CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null) {
                    if ("finished".equals(msg.getContent())) {
                        finalizados++;
                        if (finalizados >= totalCompradores) {
                            Utils.log("TODAS AS OPERAÇÕES DE COMPRA E VENDA FORAM FINALIZADAS!");
                            try { Thread.sleep(5000); } catch (Exception ignored) {}
                            System.exit(0);
                        }
                    }
                } else block();
            }
        });
    }
}
