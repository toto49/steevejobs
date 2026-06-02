package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.model.Message;

import java.sql.SQLException;
import java.util.List;

public class MessageServiceImpl implements MessageService {

    private final MessageDAO messageDAO;

    public MessageServiceImpl() {
        this.messageDAO = new MessageDAO();
    }

    public MessageServiceImpl(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public Message getMessage(int id) {
        try {
            return messageDAO.getById(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Message> getMessagesByAuteur(int auteurId) {
        try {
            return messageDAO.findByAuteurId(auteurId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Message> getMessagesByTicketId(int ticketId) {
        try {
            return messageDAO.findByTicketId(ticketId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur BDD : Impossible de récupérer les messages du ticket.", e);
        }
    }

    @Override
    public Message createMessage(Message message) {
        try {
            messageDAO.createMessage(message);
            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter le message.", e);
        }
    }

    @Override
    public boolean deleteMessage(int id) {
        try {
            return messageDAO.deleteMessage(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
