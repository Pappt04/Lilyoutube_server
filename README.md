# 📽️  Jutjubić Backend Service (ISA 2025)

Ovo je serverska komponenta za platformu za video streaming Jutjubić, razvijena kao projekat za predmet Internet Softverske Arhitekture (ISA).

Jutjubić je zamišljen kao superiorna video platforma, fokusirana na korisnika, čija je misija da pruži iskustvo bez reklama, prekida i algoritamske manipulacije. Ovaj backend servis je odgovoran za svu ključnu poslovnu logiku, autentifikaciju korisnika, perzistenciju podataka i izlaganje RESTful API-ja koji koristi Angular frontend.

## ✎ Autori

[Papp Tamás](https://github.com/Pappt04)

[Apró Dorottya](https://github.com/adorottya)

[Mikro Arsenijević](https://github.com/watenfragen)


## 📺 Frontend

Frontend za aplikaciju je dostupan [ovde](https://github.com/Pappt04/Lilyoutube-frontend)

## 🚀 Arhitektura

Backend je izgrađen kao dekuplovani REST API koristeći Spring Boot, prateći standardnu Slojnu Arhitekturu (Controller, Service, Repository, Model) radi jasne podele odgovornosti i lakog održavanja.

Tehnološki stek: Java, Spring Boot, Spring Data JPA.

Svrha: Izlaganje sigurnih i kontrolisanih endpoint-a za upravljanje korisnicima, video zapisima, komentarima i ostalim funkcionalnostima platforme.

Komunikacija: Komunicira sa klijentom (Angular SPA) koristeći JSON format preko HTTP protokola.

Slojevi Aplikacije:

Controller Layer (/controller): Prihvata HTTP zahteve, delegira obradu na Service sloj i vraća JSON odgovore.

Service Layer (/service): Sadrži primarnu poslovnu logiku, primenjuje transakcije i upravlja autorizacijom.

Repository Layer (/repository): Interfejsi za komunikaciju sa bazom podataka (CRUD operacije) preko Spring Data JPA.

Model Layer (/model): Sadrži JPA entitete (za perzistenciju) i DTO objekte (za prenos podataka).

## 🛠️ Tehnološki Stek

Jezik: Java 21+

Okvir: Spring Boot 3+

Perzistencija: Spring Data JPA / Hibernate

Baza Podataka: PostgreSQL

Alat za izgradnju: Maven

Sigurnost: Spring Security

## ⚙️ Lokalno Pokretanje Projekta

1. Kloniranje Repozitorijuma

```
git clone https://github.com/Pappt04/Lilyoutube_server.git
cd Lilyoutube_server
```

2. Konfiguracija Okruženja (.env)

Aplikacija zahteva konfiguraciju za povezivanje sa bazom podataka i podešavanje servera.

Kreirajte fajl pod nazivom .env u korenu backend direktorijuma (u Lilyoutube_server folderu). Ovaj fajl koristite za definisanje sledećih promenljivih okruženja.

.env Template
```

# -----------------------------
# APPLICATION SERVER CONFIG
# -----------------------------
# Port na kojem će se pokrenuti Spring Boot aplikacija
SERVER_PORT=<port>

# -----------------------------
# DATABASE CONFIGURATION
# -----------------------------
# Ime baze podataka (npr. 'lilyoutube_db')
DATABASE_NAME=<ime_baze>

# Port baze podataka (npr. 5432 za PostgreSQL)
DATABASE_PORT=<port_baze>

# Host baze podataka (npr. 'localhost')
DATABASE_HOST=<host_baze>

# Korisničko ime za bazu podataka
DATABASE_USERNAME=<korisnicko_ime>

# Lozinka za bazu podataka
DATABASE_PASSWORD=<lozinka>
```

3. Pokretanje Aplikacije

Nakon konfigurisanja .env fajla, pokrenite aplikaciju koristeći Maven:
```
mvn clean install
mvn spring-boot:run
```

Aplikacija će biti dostupna na adresi http://localhost:8080 (ili portu definisanom u SERVER_PORT).

## 🌐 API Dokumentacija


