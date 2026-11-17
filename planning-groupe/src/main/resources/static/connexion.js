// =====================================================================
// PlanIt — page de connexion
// =====================================================================

const $ = (sel) => document.querySelector(sel);

/** Jeton anti-CSRF : le serveur le dépose dans un cookie, on le renvoie avec chaque envoi. */
function jetonCsrf() {
    const trouve = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
    return trouve ? decodeURIComponent(trouve[1]) : "";
}

async function messageErreur(reponse) {
    try {
        const erreur = await reponse.json();
        if (erreur.message) return erreur.message;
    } catch { /* pas de JSON */ }
    return `Erreur ${reponse.status}`;
}

/** Envoie l'e-mail et le mot de passe. Renvoie un message d'erreur, ou null si tout va bien. */
async function connecter(email, motDePasse) {
    const reponse = await fetch("/api/connexion", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded", "X-XSRF-TOKEN": jetonCsrf() },
        body: new URLSearchParams({ email, motDePasse }),
    });
    return reponse.ok ? null : messageErreur(reponse);
}

$("#form-connexion").addEventListener("submit", async (e) => {
    e.preventDefault();
    const champs = e.target.elements;
    const erreur = await connecter(champs.email.value, champs.motDePasse.value);
    if (erreur) {
        $("#erreur-connexion").textContent = erreur;
        champs.motDePasse.value = "";
        champs.motDePasse.focus();
    } else {
        location.replace("/");
    }
});

$("#form-premier").addEventListener("submit", async (e) => {
    e.preventDefault();
    const champs = e.target.elements;
    const reponse = await fetch("/api/inscription", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": jetonCsrf() },
        body: JSON.stringify({
            nom: champs.nom.value,
            email: champs.email.value,
            motDePasse: champs.motDePasse.value,
            code: champs.code.value,
        }),
    });
    if (!reponse.ok) {
        $("#erreur-premier").textContent = await messageErreur(reponse);
        return;
    }
    const erreur = await connecter(champs.email.value, champs.motDePasse.value);
    if (erreur) $("#erreur-premier").textContent = erreur;
    else location.replace("/");
});

/** Affiche le formulaire de création de compte : le tout premier, ou avec un code d'invitation. */
function afficherInscription(premierCompte, codeDemande, code = "") {
    const champs = $("#form-premier").elements;
    $("#form-connexion").hidden = true;
    $("#form-premier").hidden = false;
    $("#erreur-premier").textContent = "";
    $("#intro-inscription").textContent = premierCompte
        ? "Bienvenue ! Crée le premier compte : ce sera l'administrateur du groupe."
        : "Crée ton compte avec le code d'invitation reçu d'un administrateur.";
    $("#libelle-code").textContent = premierCompte ? "Code d'installation" : "Code d'invitation";
    $("#champ-code").hidden = !codeDemande;
    champs.code.required = codeDemande;
    champs.code.value = code;
    $("#retour-connexion").hidden = premierCompte;
    (codeDemande && !code ? champs.code : champs.nom).focus();
}

function afficherConnexion() {
    $("#form-premier").hidden = true;
    $("#form-connexion").hidden = false;
    $("#form-connexion").elements.email.focus();
}

$("#j-ai-un-code").addEventListener("click", () => afficherInscription(false, true));
$("#retour-connexion").addEventListener("click", afficherConnexion);

async function demarrer() {
    const session = await (await fetch("/api/session")).json();
    if (session.connecte) {
        location.replace("/");
        return;
    }
    // un lien d'invitation ressemble à /connexion.html?invitation=k7mq-x2pd-9fhw
    const codeDuLien = new URLSearchParams(location.search).get("invitation");
    if (session.premierCompte) {
        afficherInscription(true, session.codeInstallationRequis);
    } else if (codeDuLien) {
        afficherInscription(false, true, codeDuLien);
    }
}

demarrer().catch(() => {
    $("#erreur-connexion").textContent = "Serveur injoignable. Réessaie dans un instant.";
});
