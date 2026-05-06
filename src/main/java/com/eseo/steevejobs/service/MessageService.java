package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.Message;

import java.util.List;

public interface MessageService {

    Message getMessage(int id);

    List<Message> getMessagesByAuteur(int AuteurId);

    boolean supprimerMessage(int id);

}
