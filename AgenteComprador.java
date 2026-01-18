package sma;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgenteComprador extends Agent {

    private String tipoDesejado;
    private String produtoDesejado;
    private double orcamento;
    private final long timeoutResposta = 3000;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            tipoDesejado = args[0].toString().toLowerCase();
            produtoDesejado = args[1].toString();
            try {
                orcamento = Double.parseDouble(args[2].toString());
            } catch (Exception e) {
                orcamento = 0;
            }
        } else {
            Utils.log(getLocalName() + ": argumentos insuficientes.");
            doDelete();
            return;
        }

        Utils.log(getLocalName() + " iniciado. Quer: " + tipoDesejado + " -> '" + produtoDesejado + "' com orçamento R$ " + orcamento);

        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                try {
                    // 1. Encontrar Vendedores
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(tipoDesejado); // --> CORRIGIDO
                    template.addServices(sd);
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result == null || result.length == 0) {
                        Utils.log(getLocalName() + " não encontrou vendedores do tipo " + tipoDesejado); // --> CORRIGIDO
                        finalizarComprador();
                        return;
                    }

                    List<AID> vendedores = new ArrayList<>();
                    for (DFAgentDescription dfd : result) vendedores.add(dfd.getName());

                    // 2. Enviar CFP para todos os vendedores
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID s : vendedores) cfp.addReceiver(s);
                    cfp.setContent(produtoDesejado); // --> CORRIGIDO
                    send(cfp);
                    Utils.log(getLocalName() + " -> CFP para " + vendedores.size() + " vendedores do produto '" + produtoDesejado + "'"); // --> CORRIGIDO

                    // 3. Coletar Propostas
                    Map<AID, String[]> propostas = new HashMap<>();
                    long deadline = System.currentTimeMillis() + 2000;
                    while (System.currentTimeMillis() < deadline) {
                        ACLMessage reply = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
                        if (reply != null) {
                            String content = reply.getContent();
                            String[] parts = content.split("\\|");
                            if (parts.length >= 2) {
                                propostas.put(reply.getSender(), new String[]{parts[0], parts[1]});
                                Utils.log(getLocalName() + " recebeu PROPOSTA de " + reply.getSender().getLocalName() + " : Produto ID " + parts[0] + ", Preço R$ " + parts[1]);
                            }
                        } else {
                            block(500);
                        }
                    }

                    if (propostas.isEmpty()) {
                        Utils.log(getLocalName() + " não recebeu propostas válidas. Compra sem sucesso.");
                        finalizarComprador();
                        return;
                    }

                    // 4. Escolher a melhor proposta
                    AID melhorVendedor = null;
                    String melhorProdutoId = null;
                    double melhorPreco = Double.MAX_VALUE;
                    for (Map.Entry<AID, String[]> entry : propostas.entrySet()) {
                        double price = Double.parseDouble(entry.getValue()[1]);
                        if (price < melhorPreco) {
                            melhorPreco = price;
                            melhorVendedor = entry.getKey();
                            melhorProdutoId = entry.getValue()[0];
                        }
                    }
                    Utils.log(getLocalName() + " melhor proposta: " + melhorVendedor.getLocalName() + " por R$ " + melhorPreco + " (Produto ID: " + melhorProdutoId + ")");

                    // 5. Tentar negociar (se o preço estiver dentro do orçamento)
                    if (melhorPreco > orcamento) { // --> CORRIGIDO
                        Utils.log(getLocalName() + " melhor proposta (R$ " + melhorPreco + ") excede o orçamento (R$ " + orcamento + "). Compra cancelada."); // --> CORRIGIDO
                        finalizarComprador();
                        return;
                    }

                    double precoOfertado = melhorPreco * 0.95;

                    ACLMessage negociarMsg = new ACLMessage(ACLMessage.PROPOSE);
                    negociarMsg.setConversationId("negociacao-compra");
                    negociarMsg.addReceiver(melhorVendedor);
                    negociarMsg.setContent(melhorProdutoId + "|" + precoOfertado);
                    send(negociarMsg);
                    Utils.log(getLocalName() + " -> NEGOCIAÇÃO para " + melhorVendedor.getLocalName() + " oferecendo R$ " + precoOfertado);

                    // 6. Esperar resultado da negociação
                    ACLMessage conf = blockingReceive(timeoutResposta);

                    if (conf != null && conf.getPerformative() == ACLMessage.INFORM) {
                        Utils.log(getLocalName() + " COMPRA BEM-SUCEDIDA (negociado)! " + conf.getContent());
                    } else {
                        // 7. Se negociação falhou, tentar comprar pelo preço original (fallback)
                        Utils.log(getLocalName() + " negociação falhou. Tentando comprar pelo preço original...");

                        ACLMessage aceiteMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        aceiteMsg.setConversationId("fallback-compra");
                        aceiteMsg.addReceiver(melhorVendedor);
                        aceiteMsg.setContent(melhorProdutoId + "|" + melhorPreco);
                        send(aceiteMsg);
                        Utils.log(getLocalName() + " -> ACCEPT_PROPOSAL fallback para " + melhorVendedor.getLocalName() + " por R$ " + melhorPreco);

                        ACLMessage confFinal = blockingReceive(timeoutResposta);

                        if (confFinal != null && confFinal.getPerformative() == ACLMessage.INFORM) {
                            Utils.log(getLocalName() + " COMPRA BEM-SUCEDIDA (fallback)! " + confFinal.getContent());
                        } else {
                            String motivo = (confFinal != null) ? confFinal.getContent() : "timeout";
                            Utils.log(getLocalName() + " compra falhou no fallback. Motivo: " + motivo);
                        }
                    }
                    finalizarComprador();
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
    }

    private void finalizarComprador() {
        Utils.log(getLocalName() + " finalizando suas atividades.");
        // --> ALTERADO: Nome da classe do monitor
        ACLMessage informMonitor = new ACLMessage(ACLMessage.INFORM);
        informMonitor.addReceiver(new AID("AgenteMonitor", AID.ISLOCALNAME));
        informMonitor.setContent("finished");
        send(informMonitor);
        doDelete();
    }
}
