package ch.adesso.openshift.backend.messages.control;

import ch.adesso.openshift.backend.messages.entity.Message;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class MessageProvider {

    @PersistenceContext
    EntityManager em;

    public Message create(Message message) {
        return em.merge(message);
    }

    public Message get(String id) {
        return em.find(Message.class, id);
    }

    public List<Message> getAll() {
        return em.createNamedQuery("findAll", Message.class)
                .getResultList();
    }

}
