package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.model.Message;

import java.sql.SQLException;
import java.util.List;

public class MessageServiceImpl {
    private final MessageDAO messageDAO = new MessageDAO();

    public Message getMessageByID(int id) {
        try {
            return messageDAO.getById(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> getMessagesByAuteur(int auteurId) {
        try {
            return messageDAO.findByAuteurId(auteurId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean supprimerMessage(int id) {
        try {
            return messageDAO.deleteMessage(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
