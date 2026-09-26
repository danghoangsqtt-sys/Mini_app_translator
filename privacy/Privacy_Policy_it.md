# Mini Conversation — Informativa tecnica sulla privacy

Ultimo aggiornamento tecnico: 26 settembre 2026.

Questo documento descrive i flussi di dati implementati nell'attuale codice sorgente di Mini Conversation. Non sostituisce una revisione legale. Prima della distribuzione pubblica, l'editore deve inserire l'identità e i contatti aggiornati del titolare del trattamento, le basi giuridiche applicabili, le informative dello store e gli eventuali testi richiesti dalla giurisdizione.

## Funzionamento predefinito sul dispositivo

Mini Conversation non richiede un account Google Cloud, un progetto con fatturazione o una credenziale service-account per le modalità predefinite Conversation e Walkie-Talkie.

- L'audio del microfono viene fornito ad Android `SpeechRecognizer`. Quando il dispositivo offre un riconoscitore on-device, il riconoscimento può avvenire sul dispositivo. In caso contrario Android può usare un servizio di riconoscimento di sistema che richiede la rete o tratta l'audio secondo i termini del relativo fornitore. Mini Conversation non passa automaticamente all'implementazione Google Cloud legacy.
- Le trascrizioni parziali e finali vengono elaborate da Google ML Kit Translation sul dispositivo. I modelli di traduzione vengono scaricati e memorizzati sul dispositivo. ML Kit può contattare Google per scaricare o aggiornare i modelli, ricevere correzioni dell'SDK e inviare metriche sulle prestazioni o sull'utilizzo secondo i termini di Google.
- Android Text-to-Speech pronuncia il testo tradotto. Il motore TTS installato è scelto e controllato dal dispositivo/utente e può avere un proprio comportamento di rete e privacy.
- In modalità Conversation, i dati del profilo e il testo tradotto vengono scambiati con il peer selezionato tramite il trasporto Bluetooth esistente. Il gestore dell'applicazione non fornisce un server intermediario per le conversazioni.

## Dati memorizzati sul dispositivo

- Nome visualizzato, immagine profilo opzionale, impostazioni, modelli di traduzione scaricati, informazioni sui peer recenti e dati storici limitati sull'utilizzo vengono salvati localmente.
- La cancellazione dei dati dell'app o la disinstallazione rimuove i dati locali gestiti dall'applicazione, fatto salvo il comportamento di Android e dei componenti di terze parti.
- I payload delle conversazioni Bluetooth usano il trasporto esistente. Non utilizzare la build corrente per conversazioni altamente sensibili finché non sarà completata la revisione separata della cifratura a livello applicativo.

## Credenziale Cloud legacy opzionale

La schermata **Cloud legacy (avanzato)** conserva l'importazione/eliminazione delle credenziali solo per migrazione e compatibilità. Importare un file JSON service-account non modifica il runtime predefinito e non abilita un fallback Cloud automatico.

La credenziale selezionata viene validata, cifrata con una chiave protetta da Android Keystore, salvata nello spazio privato dell'applicazione ed esclusa da Android Auto Backup. Può essere eliminata dalla stessa schermata. Nessuna credenziale condivisa è inclusa nell'APK. Le credenziali service-account sono sensibili e non devono essere usate con un progetto Cloud personale o di produzione senza un progetto di sicurezza sottoposto a revisione indipendente.

## Permessi e capacità del dispositivo

- Il permesso microfono viene usato per il riconoscimento vocale.
- I permessi Bluetooth/Nearby Devices vengono usati per scoprire i peer e comunicare. Nelle versioni Android in cui il modello dei permessi Bluetooth è collegato alla posizione, il sistema operativo può richiedere il permesso di localizzazione; Mini Conversation non legge né memorizza coordinate GPS.
- L'accesso a Internet può essere usato per scaricare/aggiornare i modelli ML Kit, da un riconoscitore vocale di sistema basato sulla rete, dal TTS di terze parti, dai link repository/privacy o dagli strumenti legacy avviati esplicitamente.
- I permessi per notifiche e servizi in primo piano servono a rendere visibili all'utente le sessioni vocali attive.
- Le immagini profilo e i file credenziale legacy opzionali vengono scelti tramite i selettori Android; l'app non esegue scansioni della memoria condivisa alla ricerca di credenziali.

## Componenti di terze parti

I fornitori rilevanti possono includere il servizio di riconoscimento vocale Android scelto sul dispositivo, Google ML Kit, il motore Android TTS installato, Google Play e il dispositivo peer selezionato dall'utente. Ai trattamenti da loro effettuati si applicano i rispettivi termini e informative privacy.

## Controlli dell'utente

L'utente può negare o revocare i permessi nelle Impostazioni Android, eliminare i modelli di traduzione scaricati nelle impostazioni di Mini Conversation, eliminare una credenziale legacy opzionale, cancellare i dati dell'applicazione, disconnettere i peer o disinstallare l'app. Alcune funzioni non operano senza il relativo permesso, modello o capacità del dispositivo.

## Revisione necessaria prima del rilascio

Questa informativa registra il comportamento tecnico implementato. Il rilascio pubblico rimane bloccato finché l'editore non conferma le informazioni finali sul titolare/contatto e non ottiene l'eventuale revisione legale richiesta dal territorio di distribuzione e dallo store.
