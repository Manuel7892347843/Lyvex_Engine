# Lyvex Engine

## Informazioni
Versione Java: 21 TLS
Build: Gradle

## Come creare il file eseguibilie?
1) Entrare nella directory del progetto
2) Creare il shadow-jar o fat-jar, tramite linea di comando oppure interfaccia apposita
3) Eseguire il comando: `powershell -ExecutionPolicy Bypass -File .\package-windows.ps1` per creare il pacchetto zippato con dentro java 21 ed i file per far eseguire il file .jar


## Descrizione del progetto

Lyvex Engine è un motore grafico 2D sviluppato in Java con l'obiettivo di creare un ambiente base per la realizzazione di applicazioni interattive e videogiochi bidimensionali.

Il progetto nasce dal mio interesse per la programmazione grafica e per il funzionamento dei game engine. Durante lo sviluppo ho cercato di realizzare una struttura che permettesse di creare progetti, gestire scene, risorse, impostazioni e componenti grafici attraverso un'interfaccia editor.

Lyvex Engine non è solo un programma finale, ma anche un percorso di studio pratico che mi ha permesso di approfondire concetti legati alla grafica, alla gestione degli input, all'organizzazione del codice e alla progettazione di software più complessi.

## Tecnologie utilizzate

Il progetto è stato sviluppato utilizzando diverse tecnologie. **Java** è il linguaggio principale, scelto per sviluppare la logica del motore, la gestione dei progetti, delle scene e dell'editor. Per la parte di rendering grafico 2D, cioè per disegnare a schermo gli elementi visivi, è stato utilizzato **OpenGL**. Per poter usare OpenGL e altre funzionalità native direttamente da Java è stata adottata la libreria **LWJGL**. L'interfaccia grafica dell'editor, composta da pannelli, menu, finestre e impostazioni, è stata realizzata con **ImGui**. **Gradle** è stato impiegato per gestire la compilazione del progetto, le dipendenze e la creazione delle versioni eseguibili. Infine, per la gestione e il salvataggio di dati strutturati è stato utilizzato **Gson**.

## Obiettivi del progetto

Gli obiettivi principali di Lyvex Engine erano molteplici. Il primo obiettivo era realizzare un motore grafico 2D funzionante, capace di gestire il rendering di elementi grafici tramite OpenGL. Un altro obiettivo fondamentale era comprendere il funzionamento interno di un game engine, analizzandone l'architettura e i vari componenti. Ho voluto anche creare un editor dotato di interfaccia grafica, in modo da poter interagire visivamente con il progetto. Era importante inoltre organizzare scene e risorse in modo ordinato, permettendo di salvare e caricare i dati del progetto in qualsiasi momento. Infine, ho cercato di gestire impostazioni come audio, scene e livelli grafici, e di migliorare le mie competenze nella programmazione Java.

## Funzionalità principali

Lyvex Engine permette di creare nuovi progetti e di aprire progetti esistenti in modo semplice. L'engine organizza automaticamente le cartelle del progetto, come quelle dedicate alle risorse, alle scene e alle impostazioni, mantenendo una struttura ordinata. È possibile creare, caricare e salvare scene, gestendo le impostazioni del progetto in modo flessibile. L'editor permette di configurare i livelli di ordinamento grafico e di gestire le impostazioni audio. L'interfaccia, realizzata con ImGui, offre un ambiente di lavoro chiaro e funzionale. Infine, il progetto può essere preparato per una versione eseguibile, pronta per essere distribuita.

## Struttura del progetto

Il progetto è organizzato in modo da separare codice sorgente, risorse, configurazioni e versioni esportate.

La cartella `src` contiene il codice sorgente principale del motore e dell'editor. La cartella `Assets` raccoglie risorse e script dei progetti creati. Le impostazioni del progetto sono contenute nella cartella `ProjectSettings`. Eventuali file compilati o generati durante lo sviluppo si trovano in `Compiled`. Le versioni esportate del programma sono salvate in `release`. I file `build.gradle` e `settings.gradle` servono rispettivamente per la configurazione di Gradle e per la configurazione generale del progetto. I file `gradlew` e `gradlew.bat` permettono di eseguire Gradle senza doverlo installare manualmente. Infine, il file `Avvia.bat` è usato per avviare la versione consegnata del programma.

Questa struttura permette di mantenere il progetto ordinato e più semplice da gestire.

## Architettura generale

Lyvex Engine è diviso in più parti principali, ognuna con un compito specifico.

### Core del motore

Il core contiene la logica principale del motore, come la gestione del ciclo di esecuzione, delle scene, dei progetti, delle risorse e delle impostazioni. Rappresenta il nucleo centrale su cui si basa tutto il funzionamento dell'engine.

### Sistema di rendering

La parte grafica utilizza OpenGL tramite LWJGL. Questo permette di disegnare elementi 2D a schermo e di gestire la parte visiva del motore, occupandosi del disegno di sprite, texture e altri elementi grafici.

### Editor

L'editor è realizzato con ImGui e permette di interagire con il progetto attraverso finestre, menu e pannelli. Da qui è possibile modificare impostazioni, gestire scene e configurare elementi del progetto in modo visuale e immediato.

### Gestione dei dati

Le informazioni del progetto vengono salvate in file dedicati, in modo da poter riaprire e continuare il lavoro successivamente. Per la gestione dei dati strutturati viene utilizzato Gson, che facilita la serializzazione e la deserializzazione degli oggetti Java.

## Competenze sviluppate

Durante lo sviluppo di Lyvex Engine ho migliorato diverse competenze tecniche. Ho consolidato la programmazione in Java e la programmazione orientata agli oggetti, applicandole a un progetto di dimensioni reali. Ho imparato a utilizzare Gradle per la gestione delle dipendenze e della compilazione. L'uso di librerie esterne mi ha permesso di capire come integrare strumenti di terze parti in un progetto. Ho acquisito le basi di OpenGL e imparato a gestire il rendering 2D. La creazione di interfacce con ImGui mi ha dato esperienza nella progettazione di UI per strumenti di sviluppo. Ho anche migliorato l'organizzazione di scene e risorse, il salvataggio e il caricamento di dati, il debugging e la risoluzione di errori, e la progettazione di un software modulare.

## Difficoltà incontrate

Una delle difficoltà principali è stata organizzare il progetto in modo ordinato, separando correttamente le varie parti del motore: rendering, interfaccia, gestione delle scene, salvataggio dei dati e impostazioni. Trovare l'equilibrio giusto tra modularità e semplicità non è stato immediato.

Un'altra difficoltà è stata l'utilizzo di tecnologie più avanzate come OpenGL e ImGui, che richiedono una buona comprensione del ciclo di rendering e della gestione delle finestre grafiche. Anche la gestione delle dipendenze tramite Gradle è stata importante, perché ha permesso di collegare correttamente le librerie necessarie e creare una versione eseguibile del progetto.

## Motivazione della scelta

Ho scelto di presentare Lyvex Engine perché è un progetto che rappresenta bene il mio interesse per la programmazione e per lo sviluppo di videogiochi. Rispetto ad altri lavori, questo progetto mi ha permesso di affrontare problemi più complessi e realistici, simili a quelli presenti nello sviluppo di software veri. Mi ha dato la possibilità di partire da un'idea personale e trasformarla gradualmente in uno strumento funzionante. Per questo motivo considero Lyvex Engine uno dei progetti più significativi del mio percorso.

## Possibili sviluppi futuri

In futuro Lyvex Engine potrebbe essere migliorato aggiungendo diverse funzionalità. Tra queste, un sistema di fisica 2D per gestire collisioni e movimenti realistici, strumenti più avanzati per modificare le scene, e un sistema di animazioni per dare vita agli oggetti. Si potrebbe sviluppare un editor più completo per gli oggetti e una gestione più avanzata degli asset. L'esportazione dei giochi creati e il supporto a più piattaforme renderebbero l'engine più versatile. Infine, miglioramenti alle prestazioni del rendering e una documentazione tecnica più dettagliata completerebbero il quadro.
