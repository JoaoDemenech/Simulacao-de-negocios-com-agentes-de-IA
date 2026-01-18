package sma;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

public class AgenteVendedor extends Agent {

    private String tipoProduto;
    private final List<Produto> inventario = new ArrayList<>();
    private double minPorcentagem = 0.85;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        if (args != null && args.length >= 2) {
            tipoProduto = args[0].toString().toLowerCase();
            carregarInventario(args[1].toString());
        }
        else {
            Utils.log(getLocalName() + ": argumentos insuficientes.");
            doDelete();
            return;
        }
        if (args.length >= 3) {
            try {
                minPorcentagem = Double.parseDouble(args[2].toString());
            } catch (Exception ignored) {}
        }

        registrarServico();
        Utils.log(getLocalName() + " iniciado. Estoque: " + inventario);

        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.CFP),
                        MessageTemplate.or(
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
                        )
                );
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    switch (msg.getPerformative()) {
                        case ACLMessage.CFP:
                            lidarComCFP(msg);
                            break;
                        case ACLMessage.PROPOSE:
                            lidarComNegociacao(msg);
                            break;
                        case ACLMessage.ACCEPT_PROPOSAL:
                            lidarComAceiteDeProposta(msg);
                            break;
                    }
                }
                else {
                    block();
                }
            }
        });
    }

    private void lidarComCFP(ACLMessage cfp) {
        String content = cfp.getContent();
        Produto found = buscarProdutoDisponivel(content);

        if (found == null) {
            ACLMessage refuse = cfp.createReply();
            refuse.setPerformative(ACLMessage.REFUSE);
            refuse.setContent("no_item");
            send(refuse);
        }
        else {
            ACLMessage propose = cfp.createReply();
            propose.setPerformative(ACLMessage.PROPOSE);
            propose.setContent(found.getId() + "|" + found.getPrice());
            send(propose);
            Utils.log(getLocalName() + " -> PROPOSTA para " + cfp.getSender().getLocalName() + " sobre " + found.getName() + " por R$ " + found.getPrice());
        }
    }

    private void lidarComNegociacao(ACLMessage proposal) {
        String[] parts = proposal.getContent().split("\\|");
        if (parts.length < 2) return;

        String productId = parts[0];
        double offeredPrice = Double.parseDouble(parts[1]);
        Produto product = buscarProdutoPorId(productId);

        ACLMessage reply = proposal.createReply();
        reply.setConversationId(proposal.getConversationId());

        if (product != null && offeredPrice >= product.getPrice() * minPorcentagem) {
            if (product.purchase()) {
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("NEGOCIO_FECHADO! Produto " + product.getName() + " vendido por R$ " + offeredPrice);
                Utils.log(getLocalName() + " VENDEU (negociado) " + product.getName() + " para " + proposal.getSender().getLocalName());
            }
            else {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("ITEM_VENDIDO");
                Utils.log(getLocalName() + " -> FALHA para " + proposal.getSender().getLocalName() + ". Motivo: ITEM_VENDIDO");
            }
        }
        else {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("NEGOCIACAO_FALHOU: Preço baixo demais.");
            Utils.log(getLocalName() + " -> FALHA para " + proposal.getSender().getLocalName() + ". Motivo: Preço baixo demais.");
        }
        send(reply);
    }

    private void lidarComAceiteDeProposta(ACLMessage accept) {
        String[] parts = accept.getContent().split("\\|");
        if (parts.length < 2) return;

        String productId = parts[0];
        Produto product = buscarProdutoPorId(productId);

        ACLMessage reply = accept.createReply();
        reply.setConversationId(accept.getConversationId());

        if (product != null) {
            if (product.purchase()) {
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("FALLBACK_OK! Produto " + product.getName() + " vendido por R$ " + product.getPrice());
                Utils.log(getLocalName() + " VENDEU (fallback) " + product.getName() + " para " + accept.getSender().getLocalName());
            }
            else {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("ITEM_VENDIDO");
                Utils.log(getLocalName() + " -> FALHA para " + accept.getSender().getLocalName() + ". Motivo: ITEM_VENDIDO");
            }
        }
        else {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("ITEM_NAO_ENCONTRADO");
            Utils.log(getLocalName() + " -> FALHA para " + accept.getSender().getLocalName() + ". Motivo: ITEM_NAO_ENCONTRADO");
        }
        send(reply);
    }

    private void carregarInventario(String prodList) {
        if (prodList == null || prodList.trim().isEmpty()) return;

        for (String it : prodList.split(";")) {
            String[] p = it.split(":");
            if (p.length >= 4) {
                try {
                    inventario.add(new Produto(p[0], p[1], tipoProduto, Double.parseDouble(p[2]), Integer.parseInt(p[3])));
                } catch (Exception ignored) {}
            }
        }
    }

    private void registrarServico() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipoProduto);
        sd.setName(getLocalName() + "-" + tipoProduto);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private Produto buscarProdutoDisponivel(String requestedName) {
        if (requestedName == null || requestedName.equals("*")) {
            for (Produto p : inventario) if (p.isAvailable()) return p;
            return null;
        }
        for (Produto p : inventario) if (p.isAvailable() && p.getName().equalsIgnoreCase(requestedName)) return p;
        return null;
    }

    private Produto buscarProdutoPorId(String id) {
        for (Produto p : inventario) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    @Override
    protected void takeDown() {
        Utils.log(getLocalName() + " encerrando.");
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {}
    }
}
