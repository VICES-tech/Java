package fr.vices.planning.web;

import fr.vices.planning.depot.InvitationDepot;
import fr.vices.planning.modele.Invitation;
import fr.vices.planning.securite.MembreCourant;
import fr.vices.planning.securite.ServiceComptes;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Invitations, réservées aux administrateurs : /api/invitations */
@RestController
@RequestMapping("/api/invitations")
public class InvitationControleur {

    private final InvitationDepot invitations;
    private final MembreCourant membreCourant;
    private final ServiceComptes comptes;

    public InvitationControleur(InvitationDepot invitations, MembreCourant membreCourant, ServiceComptes comptes) {
        this.invitations = invitations;
        this.membreCourant = membreCourant;
        this.comptes = comptes;
    }

    /** Les invitations encore utilisables. */
    @GetMapping
    public List<Invitation> lister() {
        membreCourant.exigerAdmin();
        return invitations.findByUtiliseeFalseAndExpireLeAfterOrderByExpireLeAsc(LocalDateTime.now());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Invitation creer() {
        membreCourant.exigerAdmin();
        return comptes.creerInvitation();
    }

    /** Annule une invitation qui n'a pas encore servi (code envoyé par erreur, par exemple). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void annuler(@PathVariable Long id) {
        membreCourant.exigerAdmin();
        invitations.deleteById(id);
    }
}
