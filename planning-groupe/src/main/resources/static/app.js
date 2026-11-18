// =====================================================================
// PlanIt — interface web
// Parle au serveur Java via l'API REST (/api/...).
// =====================================================================

const etat = {
    moi: null,                    // le membre connecté
    membres: [],
    chants: [],
    lundi: lundiDe(new Date()),   // premier jour de la semaine affichée
    evenementEdite: null,         // événement en cours de modification (ou null)
    chantCourant: null,           // chant ouvert dans l'éditeur (ou null)
    decalage: 0,                  // demi-tons de transposition
    dernierResultat: null,
};

const $ = (sel) => document.querySelector(sel);

const NOMS_TYPES = { REPETITION: "Répétition", CONCERT: "Concert", REUNION: "Réunion", AUTRE: "Autre" };
const TONALITES = [
    ["C", "Do"], ["C#", "Do#"], ["Db", "Réb"], ["D", "Ré"], ["Eb", "Mib"], ["E", "Mi"], ["F", "Fa"],
    ["F#", "Fa#"], ["Gb", "Solb"], ["G", "Sol"], ["Ab", "Lab"], ["A", "La"], ["Bb", "Sib"], ["B", "Si"],
];

// ---------------------------------------------------------------------
// Outils
// ---------------------------------------------------------------------

/** Jeton anti-CSRF : le serveur le dépose dans un cookie, on le renvoie avec chaque modification. */
function jetonCsrf() {
    const trouve = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
    return trouve ? decodeURIComponent(trouve[1]) : "";
}

async function api(methode, url, corps) {
    const entetes = {};
    if (corps !== undefined) entetes["Content-Type"] = "application/json";
    if (methode !== "GET") entetes["X-XSRF-TOKEN"] = jetonCsrf();
    const reponse = await fetch(url, {
        method: methode,
        headers: entetes,
        body: corps !== undefined ? JSON.stringify(corps) : undefined,
    });
    if (reponse.status === 401) {
        location.replace("/connexion.html");   // plus connecté : retour à la page de connexion
        throw new Error("Connexion requise");
    }
    if (!reponse.ok) {
        let message = `Erreur ${reponse.status}`;
        try {
            const erreur = await reponse.json();
            if (erreur.message) message = erreur.message;
        } catch { /* pas de JSON */ }
        throw new Error(message);
    }
    return reponse.status === 204 ? null : reponse.json();
}

function echapper(texte) {
    const div = document.createElement("div");
    div.textContent = texte ?? "";
    return div.innerHTML;
}

function toast(message) {
    const t = $("#toast");
    t.textContent = message;
    t.hidden = false;
    clearTimeout(toast.minuteur);
    toast.minuteur = setTimeout(() => (t.hidden = true), 2500);
}

function lundiDe(date) {
    const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const decalage = (d.getDay() + 6) % 7;   // lundi = 0
    d.setDate(d.getDate() - decalage);
    return d;
}

function ajouterJours(date, n) {
    const d = new Date(date);
    d.setDate(d.getDate() + n);
    return d;
}

/** Date au format AAAA-MM-JJ, en heure locale. */
function isoDate(d) {
    const deux = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${deux(d.getMonth() + 1)}-${deux(d.getDate())}`;
}

function heure(d) {
    return d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}

function moiId() {
    return etat.moi?.id ?? null;
}

// ---------------------------------------------------------------------
// Onglets
// ---------------------------------------------------------------------

document.querySelectorAll(".onglet").forEach((bouton) => {
    bouton.addEventListener("click", () => afficherVue(bouton.dataset.vue));
});

function afficherVue(vue) {
    document.querySelectorAll(".onglet").forEach((b) => b.classList.toggle("actif", b.dataset.vue === vue));
    document.querySelectorAll(".vue").forEach((s) => (s.hidden = s.id !== `vue-${vue}`));
    if (vue === "planning") chargerPlanning();
}

// ---------------------------------------------------------------------
// Membres
// ---------------------------------------------------------------------

async function chargerMembres() {
    etat.membres = await api("GET", "/api/membres");
    const admin = etat.moi.admin;
    $("#form-membre").hidden = !admin;
    $("#zone-invitations").hidden = !admin;
    if (admin) chargerInvitations();
    $("#info-membres").textContent = admin
        ? "Ajoute un membre directement (tu recevras un mot de passe provisoire à lui transmettre), ou crée une invitation plus bas."
        : "Seuls les administrateurs peuvent ajouter ou retirer des membres.";

    $("#membres").innerHTML = etat.membres.map((m) => {
        const cEstMoi = m.id === etat.moi.id;
        let actions = "";
        if (cEstMoi) {
            actions = `<button data-action="compte">Mon compte</button>`;
        } else if (admin) {
            actions = `<button data-action="mdp" data-id="${m.id}">Mot de passe</button>
                <button data-action="admin" data-id="${m.id}" data-admin="${!m.admin}">
                    ${m.admin ? "Retirer admin" : "Nommer admin"}</button>
                <button class="danger" data-action="supprimer" data-id="${m.id}">Supprimer</button>`;
        }
        return `<li>
            <span class="rond" style="background:${m.couleur || "var(--accent)"}"></span>
            <span class="nom">${echapper(m.nom)}${m.admin ? ` <span class="badge">admin</span>` : ""}
                <small>${echapper([m.role, m.email].filter(Boolean).join(" · "))}</small></span>
            <span class="actions">${actions}</span>
        </li>`;
    }).join("");
}

$("#form-membre").addEventListener("submit", async (e) => {
    e.preventDefault();
    const champs = e.target.elements;
    try {
        const cree = await api("POST", "/api/membres", {
            nom: champs.nom.value,
            email: champs.email.value,
            role: champs.role.value,
            couleur: champs.couleur.value,
        });
        e.target.reset();
        await chargerMembres();
        montrerCode(`Compte créé pour ${cree.membre.nom}`,
            "Transmets-lui ce mot de passe provisoire. Il devra le changer à sa première connexion.",
            cree.motDePasseProvisoire);
    } catch (err) {
        toast(err.message);
    }
});

$("#membres").addEventListener("click", async (e) => {
    const bouton = e.target.closest("[data-action]");
    if (!bouton) return;
    const { action, id } = bouton.dataset;
    const membre = etat.membres.find((m) => m.id === Number(id));
    try {
        if (action === "compte") {
            ouvrirCompte();
        } else if (action === "mdp") {
            if (!confirm(`Donner un nouveau mot de passe provisoire à ${membre.nom} ?`)) return;
            const r = await api("POST", `/api/membres/${id}/mot-de-passe`);
            montrerCode(`Nouveau mot de passe pour ${membre.nom}`,
                "Transmets-lui ce mot de passe provisoire. Il devra le changer à sa prochaine connexion.",
                r.motDePasseProvisoire);
        } else if (action === "admin") {
            await api("PUT", `/api/membres/${id}/admin`, { admin: bouton.dataset.admin === "true" });
            await chargerMembres();
        } else if (action === "supprimer") {
            if (!confirm(`Supprimer le compte de ${membre.nom} ?`)) return;
            await api("DELETE", `/api/membres/${id}`);
            await chargerMembres();
            chargerPlanning();
        }
    } catch (err) {
        toast(err.message);
    }
});

/**
 * Affiche un code à transmettre.
 * @param aCopier ce que le bouton « Copier » met dans le presse-papiers (par défaut : le code seul)
 */
function montrerCode(titre, texte, code, aCopier = code) {
    $("#code-titre").textContent = titre;
    $("#code-texte").textContent = texte;
    $("#code-valeur").textContent = code;
    $("#dialogue-code").dataset.aCopier = aCopier;
    $("#dialogue-code").showModal();
}

$("#code-fermer").addEventListener("click", () => {
    $("#code-valeur").textContent = "";
    $("#dialogue-code").dataset.aCopier = "";
    $("#dialogue-code").close();
});
$("#code-copier").addEventListener("click", async () => {
    try {
        await navigator.clipboard.writeText($("#dialogue-code").dataset.aCopier);
        toast("Copié !");
    } catch {
        toast("Copie impossible : note-le à la main");
    }
});

// ---------------------------------------------------------------------
// Invitations (administrateurs)
// ---------------------------------------------------------------------

function lienInvitation(code) {
    return `${location.origin}/connexion.html?invitation=${encodeURIComponent(code)}`;
}

function dateLimite(invitation) {
    return new Date(invitation.expireLe).toLocaleDateString("fr-FR", { weekday: "long", day: "numeric", month: "long" });
}

async function chargerInvitations() {
    const invitations = await api("GET", "/api/invitations");
    $("#invitations").innerHTML = invitations.map((i) => `<li>
        <span class="code-court">${echapper(i.code)}</span>
        <small>valable jusqu'au ${dateLimite(i)}</small>
        <span class="actions">
            <button data-invitation="${i.id}" data-action="partager">Partager</button>
            <button data-invitation="${i.id}" data-action="annuler" class="danger">Annuler</button>
        </span>
    </li>`).join("");
    $("#invitations").liste = invitations;
}

function partagerInvitation(invitation) {
    const lien = lienInvitation(invitation.code);
    montrerCode("Invitation à transmettre",
        `Envoie ce lien au futur membre : il y crée son compte avec le code ci-dessous, avant le ${dateLimite(invitation)}.`,
        invitation.code,
        `Rejoins le groupe sur PlanIt : ${lien}\nCode d'invitation : ${invitation.code}`);
}

$("#nouvelle-invitation").addEventListener("click", async () => {
    try {
        const invitation = await api("POST", "/api/invitations");
        await chargerInvitations();
        partagerInvitation(invitation);
    } catch (err) {
        toast(err.message);
    }
});

$("#invitations").addEventListener("click", async (e) => {
    const bouton = e.target.closest("[data-invitation]");
    if (!bouton) return;
    const invitation = $("#invitations").liste.find((i) => i.id === Number(bouton.dataset.invitation));
    try {
        if (bouton.dataset.action === "partager") {
            partagerInvitation(invitation);
        } else if (confirm(`Annuler l'invitation ${invitation.code} ? Le code ne marchera plus.`)) {
            await api("DELETE", `/api/invitations/${invitation.id}`);
            await chargerInvitations();
        }
    } catch (err) {
        toast(err.message);
    }
});

// ---------------------------------------------------------------------
// Mon compte
// ---------------------------------------------------------------------

async function chargerSession() {
    const session = await api("GET", "/api/session");
    if (!session.connecte) {
        location.replace("/connexion.html");
        throw new Error("Connexion requise");
    }
    etat.moi = session.membre;
    $("#mon-compte").textContent = `👤 ${etat.moi.nom}`;
}

function ouvrirCompte() {
    const champs = $("#form-profil").elements;
    champs.nom.value = etat.moi.nom;
    champs.role.value = etat.moi.role || "";
    champs.email.value = etat.moi.email || "";
    champs.couleur.value = etat.moi.couleur || "#4f46e5";
    $("#form-mot-de-passe").reset();
    $("#erreur-compte").textContent = "";
    $("#dialogue-compte").showModal();
}

$("#mon-compte").addEventListener("click", ouvrirCompte);
$("#fermer-compte").addEventListener("click", () => $("#dialogue-compte").close());

$("#form-profil").addEventListener("submit", async (e) => {
    e.preventDefault();
    const champs = e.target.elements;
    try {
        await api("PUT", `/api/membres/${etat.moi.id}`, {
            nom: champs.nom.value,
            role: champs.role.value,
            couleur: champs.couleur.value,
            email: champs.email.value,
        });
        await chargerSession();
        await chargerMembres();
        chargerPlanning();
        $("#erreur-compte").textContent = "";
        toast("Profil enregistré");
    } catch (err) {
        $("#erreur-compte").textContent = err.message;
    }
});

$("#form-mot-de-passe").addEventListener("submit", async (e) => {
    e.preventDefault();
    const champs = e.target.elements;
    try {
        await api("POST", "/api/compte/mot-de-passe", { ancien: champs.ancien.value, nouveau: champs.nouveau.value });
        e.target.reset();
        $("#erreur-compte").textContent = "";
        toast("Mot de passe changé");
    } catch (err) {
        $("#erreur-compte").textContent = err.message;
    }
});

$("#deconnexion").addEventListener("click", async () => {
    try {
        await api("POST", "/api/deconnexion");
    } finally {
        location.replace("/connexion.html");
    }
});

$("#supprimer-compte").addEventListener("click", () => {
    $("#form-suppression").reset();
    $("#erreur-suppression").textContent = "";
    $("#dialogue-suppression").showModal();
});
$("#annuler-suppression").addEventListener("click", () => $("#dialogue-suppression").close());

$("#form-suppression").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        await api("POST", "/api/compte/suppression", { motDePasse: e.target.elements.motDePasse.value });
        location.replace("/connexion.html");
    } catch (err) {
        $("#erreur-suppression").textContent = err.message;
    }
});

// Première connexion avec un mot de passe provisoire : il faut en choisir un nouveau.
$("#dialogue-provisoire").addEventListener("cancel", (e) => e.preventDefault());   // pas de fermeture par Échap

$("#form-provisoire").addEventListener("submit", async (e) => {
    e.preventDefault();
    const champs = e.target.elements;
    try {
        await api("POST", "/api/compte/mot-de-passe", { ancien: champs.ancien.value, nouveau: champs.nouveau.value });
        etat.moi.doitChangerMotDePasse = false;
        $("#dialogue-provisoire").close();
        toast("Mot de passe enregistré");
    } catch (err) {
        $("#erreur-provisoire").textContent = err.message;
    }
});

// ---------------------------------------------------------------------
// Planning
// ---------------------------------------------------------------------

async function chargerPlanning() {
    const lundi = etat.lundi;
    const dimanche = ajouterJours(lundi, 6);
    const options = { day: "numeric", month: "short" };
    $("#semaine-titre").textContent =
        `${lundi.toLocaleDateString("fr-FR", options)} – ${dimanche.toLocaleDateString("fr-FR", options)}`;

    const evenements = await api("GET", `/api/evenements?du=${isoDate(lundi)}&au=${isoDate(dimanche)}`);
    const aujourdhui = isoDate(new Date());
    const moi = moiId();
    let html = "";

    for (let i = 0; i < 7; i++) {
        const jour = ajouterJours(lundi, i);
        const cle = isoDate(jour);
        const duJour = evenements.filter((ev) => isoDate(new Date(ev.debut)) === cle);
        html += `<div class="jour ${cle === aujourdhui ? "aujourdhui" : ""}">
            <h3>${jour.toLocaleDateString("fr-FR", { weekday: "long", day: "numeric", month: "long" })}</h3>
            ${duJour.length === 0 ? `<p class="vide">Rien de prévu</p>` : duJour.map((ev) => carteEvenement(ev, moi)).join("")}
        </div>`;
    }
    $("#jours").innerHTML = html;
    $("#jours").evenements = evenements;
}

function carteEvenement(ev, moi) {
    const debut = new Date(ev.debut);
    const fin = new Date(ev.fin);
    const jeParticipe = ev.participants.some((p) => p.id === moi);
    const participants = ev.participants
        .map((p) => `<span class="pastille" style="--c:${p.couleur || "var(--accent)"}">${echapper(p.nom)}</span>`)
        .join("");
    const programme = ev.chants.length === 0 ? "" : `<ol class="programme">${ev.chants
        .map((c) => `<li><a data-ouvrir-chant="${c.id}">${echapper(c.titre)}</a>${c.tonalite ? ` (${echapper(c.tonalite)})` : ""}</li>`)
        .join("")}</ol>`;

    return `<div class="evenement ${ev.type}">
        <div class="haut">
            <span class="heure">${heure(debut)} – ${heure(fin)}</span>
            <span class="type">${NOMS_TYPES[ev.type] || ""}</span>
        </div>
        <h4>${echapper(ev.titre)}</h4>
        ${ev.lieu ? `<p class="details">📍 ${echapper(ev.lieu)}</p>` : ""}
        ${ev.description ? `<p class="details">${echapper(ev.description)}</p>` : ""}
        ${programme}
        <div class="pastilles">${participants || `<span class="vide">Aucun participant</span>`}</div>
        <div class="barre">
            <button data-participer="${ev.id}" data-oui="${!jeParticipe}">
                ${jeParticipe ? "Je ne viens plus" : "✋ Je participe"}</button>
            ${etat.moi.admin ? `<button data-modifier="${ev.id}">Modifier</button>
            <button class="danger" data-supprimer="${ev.id}">Supprimer</button>` : ""}
        </div>
    </div>`;
}

$("#semaine-prec").addEventListener("click", () => { etat.lundi = ajouterJours(etat.lundi, -7); chargerPlanning(); });
$("#semaine-suiv").addEventListener("click", () => { etat.lundi = ajouterJours(etat.lundi, 7); chargerPlanning(); });
$("#semaine-auj").addEventListener("click", () => { etat.lundi = lundiDe(new Date()); chargerPlanning(); });
$("#nouvel-evenement").addEventListener("click", () => ouvrirDialogue(null));

$("#jours").addEventListener("click", async (e) => {
    const cible = e.target.closest("[data-participer],[data-modifier],[data-supprimer],[data-ouvrir-chant]");
    if (!cible) return;
    const d = cible.dataset;
    try {
        if (d.participer) {
            const methode = d.oui === "true" ? "POST" : "DELETE";
            await api(methode, `/api/evenements/${d.participer}/participation`);
            chargerPlanning();
        } else if (d.modifier) {
            const ev = $("#jours").evenements.find((x) => x.id === Number(d.modifier));
            ouvrirDialogue(ev);
        } else if (d.supprimer) {
            if (!confirm("Supprimer cet événement ?")) return;
            await api("DELETE", `/api/evenements/${d.supprimer}`);
            chargerPlanning();
        } else if (d.ouvrirChant) {
            afficherVue("chants");
            ouvrirChant(Number(d.ouvrirChant));
        }
    } catch (err) {
        toast(err.message);
    }
});

function ouvrirDialogue(ev) {
    etat.evenementEdite = ev;
    const form = $("#form-evenement");
    form.reset();
    $("#erreur-evenement").textContent = "";
    $("#titre-dialogue").textContent = ev ? "Modifier l'événement" : "Nouvel événement";

    const participants = new Set(ev ? ev.participants.map((p) => p.id) : [moiId()].filter(Boolean));
    const chants = new Set(ev ? ev.chants.map((c) => c.id) : []);

    $("#choix-participants").innerHTML = etat.membres.length === 0
        ? `<span class="vide">Ajoute d'abord des membres dans l'onglet Membres.</span>`
        : etat.membres.map((m) => `<label><input type="checkbox" name="participant" value="${m.id}"
            ${participants.has(m.id) ? "checked" : ""}> ${echapper(m.nom)}</label>`).join("");
    $("#choix-chants").innerHTML = etat.chants.length === 0
        ? `<span class="vide">Aucun chant dans le répertoire.</span>`
        : etat.chants.map((c) => `<label><input type="checkbox" name="chant" value="${c.id}"
            ${chants.has(c.id) ? "checked" : ""}> ${echapper(c.titre)}</label>`).join("");

    if (ev) {
        const debut = new Date(ev.debut);
        const fin = new Date(ev.fin);
        form.titre.value = ev.titre;
        form.type.value = ev.type;
        form.date.value = isoDate(debut);
        form.heureDebut.value = heure(debut);
        form.heureFin.value = heure(fin);
        form.lieu.value = ev.lieu || "";
        form.description.value = ev.description || "";
    } else {
        const aujourdhui = new Date();
        const dansLaSemaine = aujourdhui >= etat.lundi && aujourdhui < ajouterJours(etat.lundi, 7);
        form.date.value = isoDate(dansLaSemaine ? aujourdhui : etat.lundi);
    }
    $("#dialogue-evenement").showModal();
}

$("#form-evenement").addEventListener("submit", async (e) => {
    if (e.submitter?.value !== "valider") return;   // « Annuler » ferme simplement
    e.preventDefault();
    const form = e.target;
    if (!form.reportValidity()) return;

    // Si l'heure de fin est avant l'heure de début, l'événement finit le lendemain.
    const dateFin = form.heureFin.value <= form.heureDebut.value
        ? isoDate(ajouterJours(new Date(`${form.date.value}T00:00`), 1))
        : form.date.value;

    const corps = {
        titre: form.titre.value,
        type: form.type.value,
        debut: `${form.date.value}T${form.heureDebut.value}:00`,
        fin: `${dateFin}T${form.heureFin.value}:00`,
        lieu: form.lieu.value,
        description: form.description.value,
        participantIds: [...form.querySelectorAll("[name=participant]:checked")].map((c) => Number(c.value)),
        chantIds: [...form.querySelectorAll("[name=chant]:checked")].map((c) => Number(c.value)),
    };
    try {
        if (etat.evenementEdite) {
            await api("PUT", `/api/evenements/${etat.evenementEdite.id}`, corps);
        } else {
            await api("POST", "/api/evenements", corps);
        }
        $("#dialogue-evenement").close();
        etat.lundi = lundiDe(new Date(corps.debut));
        chargerPlanning();
        toast("Événement enregistré");
    } catch (err) {
        $("#erreur-evenement").textContent = err.message;
    }
});

// ---------------------------------------------------------------------
// Chants et transposition
// ---------------------------------------------------------------------

async function chargerChants() {
    etat.chants = await api("GET", "/api/chants");
    afficherListeChants();
}

function afficherListeChants() {
    const filtre = $("#recherche-chant").value.trim().toLowerCase();
    const chants = etat.chants.filter((c) =>
        `${c.titre} ${c.artiste || ""}`.toLowerCase().includes(filtre));
    $("#chants").innerHTML = chants.length === 0
        ? `<p class="vide">Aucun chant.</p>`
        : chants.map((c) => `<li data-id="${c.id}" class="${etat.chantCourant?.id === c.id ? "actif" : ""}">
            ${echapper(c.titre)}<small>${[c.artiste, c.tonalite, c.tempo ? `${c.tempo} BPM` : ""]
                .filter(Boolean).map(echapper).join(" · ")}</small>
        </li>`).join("");
}

$("#recherche-chant").addEventListener("input", afficherListeChants);
$("#chants").addEventListener("click", (e) => {
    const li = e.target.closest("li[data-id]");
    if (li) ouvrirChant(Number(li.dataset.id));
});
$("#nouveau-chant").addEventListener("click", () => ouvrirChant(null, true));

function ouvrirChant(id, focus = false) {
    const chant = id ? etat.chants.find((c) => c.id === id) : null;
    etat.chantCourant = chant;
    $("#chant-titre").value = chant?.titre || "";
    $("#chant-artiste").value = chant?.artiste || "";
    $("#chant-tonalite").value = chant?.tonalite || "";
    $("#chant-tempo").value = chant?.tempo || "";
    $("#chant-contenu").value = chant?.contenu || "";
    autoriserModificationChant(!chant || etat.moi.admin, Boolean(chant) && etat.moi.admin);
    etat.decalage = 0;
    $("#tonalite-cible").value = "";
    afficherListeChants();
    transposer();
    chargerPiecesJointes();
    if (focus) $("#chant-titre").focus();
}

/**
 * Un nouveau chant se remplit librement ; un chant existant ne se modifie
 * ou ne se supprime que par un administrateur.
 */
function autoriserModificationChant(modifiable, supprimable) {
    for (const champ of ["#chant-titre", "#chant-artiste", "#chant-tonalite", "#chant-tempo", "#chant-contenu"]) {
        $(champ).readOnly = !modifiable;
    }
    $("#enregistrer-chant").hidden = !modifiable;
    $("#appliquer").hidden = !modifiable;
    $("#supprimer-chant").hidden = !supprimable;
    $("#info-chant").hidden = modifiable;
}

$("#enregistrer-chant").addEventListener("click", enregistrerChant);

async function enregistrerChant() {
    const corps = {
        titre: $("#chant-titre").value,
        artiste: $("#chant-artiste").value,
        tonalite: $("#chant-tonalite").value,
        tempo: Number($("#chant-tempo").value) || null,   // case vide = pas de tempo
        contenu: $("#chant-contenu").value,
    };
    if (!corps.titre.trim()) {
        toast("Donne un titre au chant");
        $("#chant-titre").focus();
        return;
    }
    try {
        const chant = etat.chantCourant
            ? await api("PUT", `/api/chants/${etat.chantCourant.id}`, corps)
            : await api("POST", "/api/chants", corps);
        await chargerChants();
        etat.chantCourant = etat.chants.find((c) => c.id === chant.id);
        autoriserModificationChant(etat.moi.admin, etat.moi.admin);
        afficherListeChants();
        chargerPiecesJointes();
        toast("Chant enregistré");
    } catch (err) {
        toast(err.message);
    }
}

$("#supprimer-chant").addEventListener("click", async () => {
    if (!etat.chantCourant || !confirm("Supprimer ce chant ?")) return;
    await api("DELETE", `/api/chants/${etat.chantCourant.id}`);
    await chargerChants();
    ouvrirChant(null);
});

// ---------------------------------------------------------------------
// Pièces jointes d'un chant (audio, vidéo, PDF, images…)
// ---------------------------------------------------------------------

function tailleLisible(octets) {
    if (octets < 1024) return `${octets} o`;
    if (octets < 1024 * 1024) return `${Math.round(octets / 1024)} Ko`;
    return `${(octets / 1024 / 1024).toFixed(1)} Mo`;
}

function icone(type) {
    if (type.startsWith("audio/")) return "🎧";
    if (type.startsWith("video/")) return "🎬";
    if (type.startsWith("image/")) return "🖼️";
    if (type === "application/pdf") return "📄";
    return "📁";
}

async function chargerPiecesJointes() {
    const chant = etat.chantCourant;
    $("#pj-ajouter").hidden = !chant;
    if (!chant) {
        $("#pj-liste").innerHTML = "";
        $("#pj-info").textContent = "Enregistre le chant pour pouvoir y joindre des fichiers.";
        return;
    }
    const liste = await api("GET", `/api/chants/${chant.id}/pieces-jointes`);
    if (etat.chantCourant?.id !== chant.id) return;   // l'utilisateur a changé de chant entre-temps
    $("#pj-info").textContent = liste.length === 0 ? "Aucun fichier pour l'instant." : "";
    $("#pj-liste").innerHTML = liste.map((pj) => {
        const url = `/api/pieces-jointes/${pj.id}`;
        let lecteur = "";
        if (pj.type.startsWith("audio/")) lecteur = `<audio controls preload="none" src="${url}"></audio>`;
        else if (pj.type.startsWith("video/")) lecteur = `<video controls preload="metadata" src="${url}"></video>`;
        return `<li>
            <div class="pj-ligne">
                <a href="${url}" target="_blank" rel="noopener">${icone(pj.type)} ${echapper(pj.nom)}</a>
                <small>${tailleLisible(pj.taille)}</small>
                ${etat.moi.admin ? `<button class="danger" data-supprimer-pj="${pj.id}" aria-label="Supprimer ${echapper(pj.nom)}">✕</button>` : ""}
            </div>
            ${lecteur}
        </li>`;
    }).join("");
}

$("#pj-fichier").addEventListener("change", async (e) => {
    const chant = etat.chantCourant;
    const fichiers = [...e.target.files];
    e.target.value = "";
    if (!chant) return;
    for (const fichier of fichiers) {
        $("#pj-info").textContent = `Envoi de « ${fichier.name} »…`;
        const donnees = new FormData();
        donnees.append("fichier", fichier);
        try {
            const reponse = await fetch(`/api/chants/${chant.id}/pieces-jointes`, {
                method: "POST",
                headers: { "X-XSRF-TOKEN": jetonCsrf() },
                body: donnees,
            });
            if (!reponse.ok) {
                const erreur = await reponse.json().catch(() => ({}));
                throw new Error(erreur.message || `Erreur ${reponse.status}`);
            }
        } catch (err) {
            toast(`${fichier.name} : ${err.message}`);
        }
    }
    await chargerPiecesJointes();
});

$("#pj-liste").addEventListener("click", async (e) => {
    const id = e.target.dataset.supprimerPj;
    if (!id || !confirm("Supprimer ce fichier ?")) return;
    try {
        await api("DELETE", `/api/pieces-jointes/${id}`);
        await chargerPiecesJointes();
    } catch (err) {
        toast(err.message);
    }
});

// Liste des tonalités cibles
$("#tonalite-cible").innerHTML += TONALITES
    .map(([anglo, latin]) => `<option value="${anglo}">${anglo} (${latin})</option>`).join("");

$("#monter").addEventListener("click", () => decaler(+1));
$("#descendre").addEventListener("click", () => decaler(-1));
$("#tonalite-cible").addEventListener("change", transposer);
$("#notation").addEventListener("change", transposer);
$("#chant-contenu").addEventListener("input", () => {
    clearTimeout(transposer.minuteur);
    transposer.minuteur = setTimeout(transposer, 300);
});
$("#chant-tonalite").addEventListener("change", transposer);

function decaler(n) {
    $("#tonalite-cible").value = "";
    etat.decalage = Math.max(-11, Math.min(11, etat.decalage + n));
    transposer();
}

async function transposer() {
    const texte = $("#chant-contenu").value;
    const cible = $("#tonalite-cible").value;
    const source = $("#chant-tonalite").value.trim();
    const mineur = etat.dernierResultat?.tonaliteSource?.endsWith("m");
    const corps = {
        texte,
        tonaliteSource: source || null,
        notation: $("#notation").value,
    };
    if (cible) corps.tonaliteCible = cible + (mineur ? "m" : "");
    else corps.demiTons = etat.decalage;

    try {
        const r = await api("POST", "/api/transposition", corps);
        etat.dernierResultat = r;
        etat.decalage = r.demiTons;
        $("#decalage").textContent = r.demiTons > 0 ? `+${r.demiTons}` : String(r.demiTons);
        $("#resultat").textContent = r.texte;
        $("#info-tonalite").textContent = r.tonaliteSource
            ? `Tonalité : ${r.tonaliteSource} → ${r.tonaliteCible ?? "?"}`
            : texte.trim() ? "Aucun accord reconnu." : "";
    } catch (err) {
        $("#info-tonalite").textContent = err.message;
    }
}

$("#copier").addEventListener("click", async () => {
    try {
        await navigator.clipboard.writeText($("#resultat").textContent);
        toast("Copié !");
    } catch {
        toast("Copie impossible sur ce navigateur");
    }
});

$("#appliquer").addEventListener("click", async () => {
    const r = etat.dernierResultat;
    if (!r || r.demiTons === 0) return;
    $("#chant-contenu").value = r.texte;
    if (r.tonaliteCible) $("#chant-tonalite").value = r.tonaliteCible;
    etat.decalage = 0;
    $("#tonalite-cible").value = "";
    await transposer();
    if (etat.chantCourant) await enregistrerChant();
    else toast("Pense à enregistrer le chant");
});

// ---------------------------------------------------------------------
// Démarrage + rafraîchissement automatique (pour voir les ajouts des autres)
// ---------------------------------------------------------------------

async function demarrer() {
    try {
        await chargerSession();
        await Promise.all([chargerMembres(), chargerChants()]);
        await chargerPlanning();
        ouvrirChant(null);
        if (etat.moi.doitChangerMotDePasse) $("#dialogue-provisoire").showModal();
    } catch (err) {
        toast("Serveur injoignable : " + err.message);
    }
}

setInterval(() => {
    if (!$("#vue-planning").hidden && !$("#dialogue-evenement").open) {
        chargerMembres().then(chargerPlanning).catch(() => {});
    }
}, 30_000);

demarrer();
