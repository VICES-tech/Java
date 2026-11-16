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

async function demarrer() {
    const session = await (await fetch("/api/session")).json();
    if (session.connecte) {
        location.replace("/");
        return;
    }
    if (session.premierCompte) {
        $("#form-connexion").hidden = true;
        $("#form-premier").hidden = false;
        $("#champ-code").hidden = !session.codeInstallationRequis;
        $("#form-premier").elements.code.required = session.codeInstallationRequis;
        $("#form-premier").elements.nom.focus();
    }
}

demarrer().catch(() => {
    $("#erreur-connexion").textContent = "Serveur injoignable. Réessaie dans un instant.";
});
