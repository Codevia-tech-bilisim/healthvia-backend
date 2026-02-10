# HealthVia Platform — Backend API

**Sağlık turizmi için kapsamlı dijital platform.**

Hasta-doktor etkileşimleri, randevu yönetimi, otel rezervasyonları, uçuş planlaması ve CRM (müşteri ilişkileri yönetimi) süreçlerini tek bir çatı altında birleştiren enterprise-grade backend servisi.

> **Production:** `https://api.healthviatech.website`

---

## Teknoloji Stack

```yaml
Runtime:       Java 21
Framework:     Spring Boot 3.5.3
Database:      MongoDB 7.0 (Atlas)
Security:      Spring Security + JWT (Access + Refresh Token)
Validation:    Bean Validation (JSR-303)
Video:         Zoom Server-to-Server OAuth
Cache:         Caffeine
Build:         Maven 3.9+
Container:     Docker + Docker Compose
Cloud:         Google Cloud Platform (Cloud Run, Artifact Registry, Secret Manager)
```

---

## Proje Yapısı

```
src/main/java/com/healthvia/platform/
├── common/                  # Ortak modeller, DTO'lar, utility'ler
│   ├── config/              #   CorsConfig, SecurityConfig, AppConstants
│   ├── dto/                 #   ApiResponse, ErrorResponse
│   ├── exception/           #   ResourceNotFoundException, GlobalExceptionHandler
│   ├── model/               #   BaseEntity (audit, soft delete, versioning)
│   └── util/                #   SecurityUtils, ValidationUtils
│
├── auth/                    # Kimlik doğrulama
│   ├── controller/          #   AuthController (register, login, refresh, logout)
│   ├── dto/                 #   LoginRequest, RegisterRequest, AuthResponse
│   ├── security/            #   JwtTokenProvider, JwtAuthFilter
│   └── service/             #   AuthService
│
├── user/                    # Kullanıcı ana sınıfı
│   └── entity/              #   User (BaseEntity → tüm roller için üst sınıf)
│
├── patient/                 # Hasta modülü
│   ├── entity/              #   Patient (tıbbi geçmiş, sigorta, acil iletişim)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── doctor/                  # Doktor modülü
│   ├── entity/              #   Doctor (uzmanlık, sertifikalar, çalışma saatleri)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── admin/                   # Admin / Satış Temsilcisi modülü
│   ├── entity/              #   Admin (roller, izinler, agent profili, shift, dil)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── appointment/             # Randevu sistemi
│   ├── entity/              #   Appointment (status lifecycle, video, notes)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── timeslot/                # Zaman dilimi yönetimi
│   ├── entity/              #   TimeSlot (doktora ait, tarih/saat, blok/serbest)
│   ├── repository/
│   ├── service/
│   └── controller/
│
├── treatment/               # Tedavi türleri
│   ├── entity/              #   TreatmentType (40+ kategori, TR/EN, FAQ, SEO, fiyat)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── hotel/                   # Otel yönetimi
│   ├── entity/              #   Hotel (oda tipleri, partner hastaneler, amenity)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── flight/                  # Uçuş bilgileri
│   └── entity/              #   Flight (havayolu, rota, kabin, fiyat)
│
├── lead/                    # CRM — Lead (potansiyel hasta) yönetimi
│   ├── entity/              #   Lead (çoklu kanal, atama, dönüşüm takibi)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── conversation/            # CRM — Konuşma yönetimi
│   ├── entity/              #   Conversation (kanal bazlı, durum, okunmamış)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── message/                 # CRM — Mesajlaşma
│   ├── entity/              #   Message (metin, dosya, şablon, dahili not)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
├── messagetemplate/         # CRM — Mesaj şablonları
│   ├── entity/              #   MessageTemplate (placeholder, çoklu dil/kanal)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
│
└── zoom/                    # Zoom video entegrasyonu
    ├── config/              #   ZoomConfig (Server-to-Server OAuth)
    ├── dto/                 #   ZoomMeetingRequest/Response
    └── service/             #   ZoomService (create, get, update, delete meeting)
```

---

## Modüller

### Core

| Modül | Açıklama | Durum |
|-------|----------|-------|
| **Auth** | JWT tabanlı kimlik doğrulama (access + refresh token), kayıt, giriş, çıkış | ✅ Production |
| **User / Patient / Doctor / Admin** | Rol bazlı kullanıcı yönetimi, profil, doğrulama | ✅ Production |
| **Appointment** | Randevu yaşam döngüsü (PENDING → CONFIRMED → COMPLETED), video konsültasyon | ✅ Production |
| **TimeSlot** | Doktor müsaitlik yönetimi, otomatik slot üretimi, çakışma kontrolü | ✅ Production |
| **Hotel** | Medikal turizm otelleri, oda tipleri, partner hastane ilişkisi | ✅ Production |
| **Flight** | Uçuş entity tanımı (controller/service henüz yok) | 🟡 Entity Only |
| **Zoom** | Server-to-Server OAuth, toplantı CRUD, video konsültasyon | ✅ Production |
| **TreatmentType** | 40+ tedavi kategorisi, TR/EN lokalizasyon, FAQ, SEO, fiyatlandırma | ✅ Ready |

### CRM

| Modül | Açıklama | Durum |
|-------|----------|-------|
| **Lead** | Çoklu kanal lead intake (WhatsApp, Instagram, Email, Web Form, Telefon), otomatik agent atama, dönüşüm takibi | ✅ Ready |
| **Conversation** | Kanal bazlı konuşma yönetimi, okunmamış takibi, pin, etiket | ✅ Ready |
| **Message** | Mesaj gönderme/alma, dosya eki, dahili notlar, teslimat durumu | ✅ Ready |
| **MessageTemplate** | Hazır mesaj şablonları, `{{placeholder}}` desteği, çoklu dil/kanal | ✅ Ready |
| **Ticket** | Konuşma içi iş takibi (otel, uçuş, randevu) | 🔲 Planned |
| **Reminder** | Hatırlatıcılar, @Scheduled zamanlama | 🔲 Planned |
| **Notification** | In-app + email bildirim altyapısı | 🔲 Planned |
| **WebSocket** | Real-time mesajlaşma (STOMP) | 🔲 Planned |

---

## API Endpoint'leri

**Base URL:** `http://localhost:8080` (dev) — `https://api.healthviatech.website` (prod)

### Auth

```
POST   /api/auth/register/patient          Hasta kaydı
POST   /api/auth/register/doctor           Doktor kaydı
POST   /api/auth/login                     Giriş
POST   /api/auth/refresh                   Token yenileme
POST   /api/auth/logout                    Çıkış
```

### Appointments

```
POST   /api/v1/appointments                Randevu oluştur
GET    /api/v1/appointments/{id}           Randevu detayı
PATCH  /api/v1/appointments/{id}/confirm   Onayla
PATCH  /api/v1/appointments/{id}/cancel    İptal
PATCH  /api/v1/appointments/{id}/complete  Tamamla
GET    /api/v1/appointments/doctor/{id}    Doktor randevuları
GET    /api/v1/appointments/patient/{id}   Hasta randevuları
```

### Time Slots

```
POST   /api/v1/slots/generate              Slot oluştur
GET    /api/v1/slots/available              Müsait slotlar
PATCH  /api/v1/slots/{id}/block            Slot blokla
```

### Treatments (Public + Admin)

```
GET    /api/v1/treatments/public            Aktif tedaviler (paginated)
GET    /api/v1/treatments/public/{slug}     Tedavi detayı (slug ile)
GET    /api/v1/treatments/public/category/{category}  Kategoriye göre
GET    /api/v1/treatments/public/popular    Popüler tedaviler
GET    /api/v1/treatments/public/featured   Öne çıkan tedaviler
GET    /api/v1/treatments/public/search     Arama
GET    /api/v1/treatments/public/by-doctor/{id}    Doktora göre
GET    /api/v1/treatments/public/by-hospital/{id}  Hastaneye göre
POST   /api/v1/treatments                  [ADMIN] Oluştur
PUT    /api/v1/treatments/{id}             [ADMIN] Güncelle
DELETE /api/v1/treatments/{id}             [ADMIN] Sil
PATCH  /api/v1/treatments/{id}/toggle-popular   [ADMIN]
PATCH  /api/v1/treatments/{id}/toggle-featured  [ADMIN]
```

### Leads (Webhook + Admin)

```
POST   /api/v1/leads/public/form            Web form'dan lead
POST   /api/v1/leads/webhook/whatsapp       WhatsApp webhook
POST   /api/v1/leads/webhook/instagram      Instagram webhook
POST   /api/v1/leads/webhook/email          Email webhook
GET    /api/v1/leads                        [ADMIN] Tüm leadler
GET    /api/v1/leads/my                     [ADMIN] Bana atanmış
GET    /api/v1/leads/unassigned             [ADMIN] Atanmamış
GET    /api/v1/leads/status/{status}        [ADMIN] Duruma göre
GET    /api/v1/leads/search                 [ADMIN] Arama
PATCH  /api/v1/leads/{id}/status            [ADMIN] Durum değiştir
PATCH  /api/v1/leads/{id}/assign            [ADMIN] Agent'a ata
PATCH  /api/v1/leads/{id}/auto-assign       [ADMIN] Otomatik ata
PATCH  /api/v1/leads/{id}/transfer          [ADMIN] Transfer et
PATCH  /api/v1/leads/{id}/convert           [ADMIN] Hastaya dönüştür
PATCH  /api/v1/leads/{id}/lost              [ADMIN] Kaybedildi
PATCH  /api/v1/leads/{id}/follow-up         [ADMIN] Takip planla
GET    /api/v1/leads/statistics             [ADMIN] İstatistikler
```

### Conversations (Admin)

```
POST   /api/v1/conversations/start          Konuşma başlat (lead + kanal)
GET    /api/v1/conversations/my             Benim konuşmalarım
GET    /api/v1/conversations/my/unread      Okunmamışlar
GET    /api/v1/conversations/my/pinned      Sabitlenenler
GET    /api/v1/conversations/lead/{leadId}  Lead'e ait konuşmalar
PATCH  /api/v1/conversations/{id}/status    Durum değiştir
PATCH  /api/v1/conversations/{id}/resolve   Çöz
PATCH  /api/v1/conversations/{id}/archive   Arşivle
PATCH  /api/v1/conversations/{id}/read      Okundu işaretle
PATCH  /api/v1/conversations/{id}/toggle-pin Sabitle/kaldır
PATCH  /api/v1/conversations/{id}/assign    Agent ata
GET    /api/v1/conversations/statistics     İstatistikler
```

### Messages (Admin)

```
POST   /api/v1/messages/send                Agent mesajı gönder
POST   /api/v1/messages/send-template       Şablon mesaj gönder
POST   /api/v1/messages/internal-note       Dahili not ekle
POST   /api/v1/messages/webhook/incoming    Dış kanaldan gelen mesaj
GET    /api/v1/messages/conversation/{id}   Konuşma mesajları
GET    /api/v1/messages/conversation/{id}/notes    Dahili notlar
GET    /api/v1/messages/conversation/{id}/search   Konuşma içi arama
GET    /api/v1/messages/search              Global arama
PATCH  /api/v1/messages/{id}/edit           Mesaj düzenle
PATCH  /api/v1/messages/{id}/delivered      Teslim durumu
PATCH  /api/v1/messages/{id}/read           Okundu durumu
```

### Message Templates (Admin)

```
GET    /api/v1/templates                    Tüm şablonlar
GET    /api/v1/templates/language/{lang}    Dile göre
GET    /api/v1/templates/category/{cat}/language/{lang}  Kategori + dil
POST   /api/v1/templates/{id}/render        Şablon render (placeholder)
POST   /api/v1/templates                    Oluştur
PUT    /api/v1/templates/{id}               Güncelle
PATCH  /api/v1/templates/{id}/toggle-active Aktif/pasif
```

### Users & Profiles

```
GET    /api/patients/me                     Hasta profili
PATCH  /api/patients/me                     Profil güncelle
GET    /api/doctors/public/search           Doktor arama (public)
GET    /api/admin/users                     [ADMIN] Kullanıcı listesi
PATCH  /api/admin/me/availability           [ADMIN] Online/offline
PATCH  /api/admin/me/agent-profile          [ADMIN] Agent profil güncelle
GET    /api/admin/agents/available           [ADMIN] Müsait agentler
```

---

## Güvenlik

**Authentication:** JWT access token (15 dk) + refresh token (7 gün), BCrypt şifreleme.

**Role-Based Access Control (RBAC):**

| Endpoint Pattern | Erişim |
|-----------------|--------|
| `/api/auth/**` | Herkese açık |
| `/api/v1/treatments/public/**` | Herkese açık |
| `/api/v1/leads/public/**`, `/api/v1/leads/webhook/**` | Herkese açık |
| `/api/patients/**` | PATIENT |
| `/api/doctors/**` | DOCTOR |
| `/api/v1/leads/**`, `/api/v1/conversations/**`, `/api/v1/messages/**` | ADMIN |
| `/api/admin/**` | ADMIN |

**Veri Güvenliği:** Soft delete, audit trail (createdAt, updatedAt, createdBy), versioning, GDPR/KVKK uyumlu.

---

## Veritabanı

**MongoDB Atlas** — 14 collection:

```
users, patients, doctors, admins,
appointments, time_slots,
treatment_types, hotels, flights,
leads, conversations, messages, message_templates,
zoom_meetings
```

Her entity `BaseEntity`'den türer: `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`, `deletedAt`, `deletedBy`, `version`.

Compound index'ler performans için tanımlı (ör: `{'status': 1, 'assignedAgentId': 1}` leads üzerinde).

---

## Kurulum

### Gereksinimler

- Java 21+
- Docker & Docker Compose
- Maven 3.9+
- MongoDB 7.0+ (veya Docker)

### Hızlı Başlangıç

```bash
# 1. Klonla
git clone https://github.com/codevia-io/healthvia-platform.git
cd healthvia-platform

# 2. MongoDB başlat
docker-compose up -d

# 3. Uygulamayı çalıştır
./mvnw spring-boot:run

# 4. Health check
curl http://localhost:8080/api/test/health
```

### Environment Değişkenleri (Production)

```properties
SPRING_PROFILES_ACTIVE=prod
MONGODB_URI=mongodb+srv://...
JWT_SECRET=...
ZOOM_ACCOUNT_ID=...
ZOOM_CLIENT_ID=...
ZOOM_CLIENT_SECRET=...
CORS_ALLOWED_ORIGINS=https://healthviatech.website,https://www.healthviatech.website,https://admin.healthviatech.website
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=...
MAIL_PASSWORD=...
```

### GCP Cloud Run Deploy

```bash
# Build & push
./mvnw clean package -DskipTests
docker build -t europe-west1-docker.pkg.dev/PROJECT/healthvia/api:latest .
docker push europe-west1-docker.pkg.dev/PROJECT/healthvia/api:latest

# Deploy
gcloud run deploy healthvia-api \
  --image europe-west1-docker.pkg.dev/PROJECT/healthvia/api:latest \
  --region europe-west1 \
  --platform managed \
  --allow-unauthenticated
```

---

## Roadmap

### v1.2 — CRM Tamamlama (Devam Eden)

- [x] TreatmentType modülü (40+ kategori, TR/EN)
- [x] Lead modülü (çoklu kanal, auto-assign, conversion tracking)
- [x] Conversation modülü (kanal bazlı, durum lifecycle)
- [x] Message modülü (mesajlaşma, dahili not, teslimat)
- [x] MessageTemplate modülü (placeholder, çoklu dil)
- [x] Admin entity — satış temsilcisi alanları
- [ ] Ticket modülü — iş takibi
- [ ] Reminder modülü — hatırlatıcılar
- [ ] WebSocket — real-time messaging
- [ ] Notification — email + in-app bildirim

### v1.3 — Payload CMS

- [ ] Collections genişletme (Treatments, Hotels, Blog, Pricing)
- [ ] Globals (Homepage, SiteSettings, About, Contact)
- [ ] Custom dashboard view
- [ ] 3-panel CRM inbox UI

### v1.4 — Frontend Entegrasyonu

- [ ] Patient dashboard
- [ ] Doctor panel
- [ ] Appointment booking flow
- [ ] Treatment listing & detail pages

### v1.5 — Production Polish

- [ ] CI/CD (GitHub Actions)
- [ ] Payment integration (Stripe / İyzico)
- [ ] SMS integration
- [ ] Rate limiting
- [ ] E2E test coverage

---

## Ekip

| İsim | Rol |
|------|-----|
| Celal | Full-Stack Lead, Backend Architecture |
| Selim Bedirhan Öztürk | Developer |
| Hüseyin Şen | Developer |

**Organizasyon:** Codevia Bilişim ve Yazılım A.Ş. — Ankara Üniversitesi Teknokent

---

## Lisans

MIT License — detaylar için [LICENSE](LICENSE) dosyasına bakın.
