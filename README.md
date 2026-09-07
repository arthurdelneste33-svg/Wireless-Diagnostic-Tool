# 📡 Wireless Diagnostic Tool for Android

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/arthurdelneste33-svg/Wireless-Diagnostic-Tool/pulls)

**Wireless Diagnostic Tool** est une application Android moderne et intuitive conçue pour analyser, diagnostiquer et optimiser les connexions réseau sans fil (Wi-Fi, réseaux mobiles, Bluetooth) directement depuis votre appareil Android.

🔗 **Dépôt GitHub** : [arthurdelneste33-svg/Wireless-Diagnostic-Tool](https://github.com/arthurdelneste33-svg/Wireless-Diagnostic-Tool/tree/main)

---

## 📋 Table des Matières

- [Fonctionnalités Principales](#-fonctionnalités-principales)
- [Aperçu & Captures d'écran](#-aperçu--captures-décran)
- [Architecture & Technologies](#-architecture--technologies)
- [Prérequis & Permissions](#-prérequis--permissions)
- [Installation & Démarrage](#-installation--démarrage)
- [Structure du Projet](#-structure-du-projet)
- [Utilisation](#-utilisation)
- [Feuille de Route (Roadmap)](#-feuille-de-route-roadmap)
- [Contribution](#-contribution)
- [Licence & Contact](#-licence--contact)

---

## ✨ Fonctionnalités Principales

### 📶 1. Analyseur Wi-Fi
* **Analyse en temps réel** des points d'accès Wi-Fi à proximité (SSID, BSSID).
* **Graphique des canaux** : Visualisation de l'occupation spectrale sur les bandes 2.4 GHz, 5 GHz et 6 GHz (Wi-Fi 6E / Wi-Fi 7).
* **Indicateur de signal (RSSI)** : Suivi en temps réel de la puissance du signal (en dBm) avec graphiques d'évolution.
* **Sécurité & Protocoles** : Identification des méthodes de chiffrement (WPA2, WPA3, Enterprise, Open).

### 📱 2. Diagnostics Réseau Mobile & Cellulaire
* **Informations SIM / Réseau** : Identification du réseau (4G / LTE / 5G NR), code opérateur (MCC/MNC).
* **Métriques de qualité de signal** : Mesure du RSRP, RSRQ, SINR et identifiants de cellules (Cell ID, TAC/LAC).

### 🌐 3. Boîte à Outils Network & IP
* **Ping & Latence** : Test de connectivité vers des serveurs distants ou passerelles locales.
* **Traceroute** : Analyse des sauts (hops) réseau pour identifier les goulots d'étranglement.
* **Scanner LAN / Sous-réseau** : Détection des appareils connectés sur le même réseau local Wi-Fi.
* **DNS Lookup & Port Scanner** : Résolution de noms de domaine et vérification des ports ouverts.

### 📊 4. Exportation & Historique
* **Rapports de diagnostic** : Export des données de scan sous formats **CSV** ou **JSON**.
* **Historique de mesures** : Suivi des performances réseau dans le temps.

---

## 🎨 Aperçu & Captures d'écran

| Liste des Réseaux Wi-Fi | Analyse des Canaux | Outils de Diagnostic |
| :---: | :---: | :---: |
| *[Ajouter capture Wi-Fi]* | *[Ajouter capture Graphique]* | *[Ajouter capture Tools]* |

*(Note: Pensez à ajouter vos propres captures d'écran dans un dossier `docs/images/` et à mettre à jour les liens).*

---

## 🛠 Architecture & Technologies

L'application suit les recommandations modernes d'architecture Android (**Modern Android Development - MAD**) :

* **Langage** : [Kotlin](https://kotlinlang.org/)
* **Interface Utilisateur** : [Jetpack Compose](https://developer.android.com/jetpack/compose) (UI Déclarative)
* **Architecture** : MVVM (Model-View-ViewModel) + Clean Architecture
* **Asynchronisme** : Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Injection de dépendances** : Hilt / Dagger
* **Réseau** : OkHttp, Retrofit, Android Network Capabilities APIs
* **Base de données / Persistence** : Room Database & DataStore Preferences
* **Graphiques** : Vico / MPAndroidChart

---

## 🔑 Prérequis & Permissions

### Minimum Requis
* **Version Android minimale** : Android 8.0 (API level 26) ou plus récent
* **Version recommandée** : Android 13+ (API 33+)
* **Android Studio** : Jellyfish / Koala / Ladybug (ou version plus récente)
* **JDK** : Java 17

### Permissions nécessaires (`AndroidManifest.xml`)

| Permission | Description & Utilité |
| :--- | :--- |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | **Requis par Android** pour autoriser le scan des réseaux Wi-Fi et Cellulaires. |
| `ACCESS_WIFI_STATE` & `CHANGE_WIFI_STATE` | Obtenir les informations Wi-Fi et exécuter des scans réseau. |
| `ACCESS_NETWORK_STATE` | Vérifier la connectivité Internet (Wi-Fi vs Données mobiles). |
| `INTERNET` | Effectuer les tests réseau (`Ping`, `Traceroute`, requêtes HTTP/DNS). |
| `NEARBY_WIFI_DEVICES` *(Android 13+)* | Scan des appareils Wi-Fi à proximité. |
| `READ_PHONE_STATE` | Récupération des informations sur l'antenne réseau cellulaire. |

---

## 🚀 Installation & Démarrage

### 1. Cloner le dépôt

```bash
git clone [https://github.com/arthurdelneste33-svg/Wireless-Diagnostic-Tool.git](https://github.com/arthurdelneste33-svg/Wireless-Diagnostic-Tool.git)
cd Wireless-Diagnostic-Tool
