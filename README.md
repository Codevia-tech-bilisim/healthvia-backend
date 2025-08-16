# HealthVia Platform 🏥

Modern, ölçeklenebilir sağlık platformu - Hasta, doktor ve admin yönetimi için kapsamlı REST API

## 📋 İçindekiler

- [Genel Bakış](#genel-bakış)
- [Özellikler](#özellikler)
- [Teknoloji Stack](#teknoloji-stack)
- [Kurulum](#kurulum)
- [API Dokümantasyonu](#api-dokümantasyonu)
- [Veritabanı Yapısı](#veritabanı-yapısı)
- [Güvenlik](#güvenlik)
- [Katkıda Bulunma](#katkıda-bulunma)

## 🎯 Genel Bakış

HealthVia Platform, sağlık sektörü için geliştirilmiş modern bir Spring Boot uygulamasıdır. Hasta kayıt sistemi, doktor yönetimi, randevu sistemi ve admin paneli içeren kapsamlı bir sağlık yönetim sistemi sunar.

### Temel Özellikler
- 👥 **Çoklu Kullanıcı Rolleri**: Hasta, Doktor, Admin
- 🔐 **JWT Tabanlı Kimlik Doğrulama**
- 📱 **RESTful API Tasarımı**
- 🏥 **Kapsamlı Sağlık Veritabanı**
- 🔍 **Gelişmiş Arama ve Filtreleme**
- 📊 **Analytics ve Raporlama**

## ✨ Özellikler

### 👤 Hasta Yönetimi
- Detaylı hasta profilleri (kimlik, sağlık bilgileri, sigorta)
- Sağlık geçmişi takibi (alerji, kronik hastalık, ilaç kullanımı)
- BMI hesaplama ve sağlık uyarıları
- Acil durum iletişim bilgileri
- GDPR uyumlu veri yönetimi

### 👨‍⚕️ Doktor Yönetimi
- Mesleki kimlik doğrulama (diploma, lisans numarası)
- Uzmanlık alanları ve sertifikalar
- Çalışma saatleri ve randevu yönetimi
- Performans istatistikleri
- Online/yüz yüze konsültasyon seçenekleri

### 🔧 Admin Paneli
- Kullanıcı yönetimi ve onay süreçleri
- Sistem istatistikleri ve raporlar
- Doktor doğrulama sistemi
- Hiyerarşik admin yapısı
- Audit log ve güvenlik takibi

### 🔒 Güvenlik Özellikleri
- JWT tabanlı kimlik doğrulama
- Role-based access control (RBAC)
- Hesap kilitleme sistemi
- Email/telefon doğrulama
- GDPR compliance

## 🛠 Teknoloji Stack

### Backend
- **Java 21** - Modern Java özellikleri
- **Spring Boot 3.5.3** - Framework
- **Spring Security** - Güvenlik
- **Spring Data MongoDB** - Veritabanı erişimi
- **JWT (jsonwebtoken)** - Token yönetimi
- **Lombok** - Kod temizliği
- **Bean Validation** - Veri doğrulama

### Veritabanı
- **MongoDB 7** - NoSQL veritabanı
- **Spring Data MongoDB** - ODM
- **Audit Trail** - Veri izleme

### DevOps & Tools
- **Docker & Docker Compose** - Konteynerizasyon
- **Maven** - Dependency management
- **Spring Boot DevTools** - Geliştirme

## 🚀 Kurulum

### Gereksinimler
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Hızlı Başlangıç

1. **Repository'yi klonlayın**
```bash
git clone https://github.com/yourusername/healthvia-platform.git
cd healthvia-platform
```

2. **MongoDB'yi başlatın (Docker)**
```bash
docker-compose up -d mongodb
```

3. **Uygulamayı çalıştırın**
```bash
# Windows
./scripts/start-dev.bat

# Linux/Mac
./scripts/start-dev.sh

# Veya manuel
./mvnw spring-boot:run
```

4. **API'yi test edin**
```bash
curl http://localhost:8080/api/test/health
```

### Manuel Kurulum

1. **MongoDB Kurulumu**
```bash
# Docker ile
docker run -d \
  --name healthvia-mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=healthvia123 \
  -e MONGO_INITDB_DATABASE=healthvia \
  mongo:7-jammy
```

2. **Uygulama Ayarları**
```properties
# src/main/resources/application.properties
spring.data.mongodb.uri=mongodb://admin:healthvia123@localhost:27017/healthvia?authSource=admin
jwt.secret=your-secret-key
```

3. **Build ve Run**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

## 📚 API Dokümantasyonu

### Authentication Endpoints

#### Hasta Kaydı
```http
POST /api/auth/register/patient
Content-Type: application/json

{
  "firstName": "Ahmet",
  "lastName": "Yılmaz",
  "email": "ahmet@example.com",
  "phone": "+905551234567",
  "password": "SecurePass123!",
  "role": "PATIENT",
  "gender": "MALE",
  "birthDate": "1990-01-01",
  "province": "İstanbul",
  "district": "Kadıköy",
  "gdprConsent": true,
  "tcKimlikNo": "12345678901",
  "birthPlace": "İstanbul"
}
```

#### Doktor Kaydı
```http
POST /api/auth/register/doctor
Content-Type: application/json

{
  "firstName": "Dr. Ayşe",
  "lastName": "Kaya",
  "email": "dr.ayse@example.com",
  "phone": "+905551234568",
  "password": "SecurePass123!",
  "role": "DOCTOR",
  "gdprConsent": true,
  "diplomaNumber": "DOC123456",
  "medicalLicenseNumber": "LIC789012",
  "medicalSchool": "İstanbul Üniversitesi Tıp Fakültesi",
  "graduationYear": 2015,
  "primarySpecialty": "Kardiyoloji",
  "yearsOfExperience": 8,
  "currentHospital": "Acıbadem Hastanesi"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "ahmet@example.com",
  "password": "SecurePass123!"
}
```

### Hasta Endpoints

#### Profil Bilgileri
```http
GET /api/patients/me
Authorization: Bearer {token}
```

#### Sağlık Bilgilerini Güncelle
```http
PATCH /api/patients/me/health
Authorization: Bearer {token}
Content-Type: application/x-www-form-urlencoded

allergies=Polen alerjisi&chronicDiseases=Hipertansiyon&currentMedications=Aspirin
```

#### BMI Hesaplama
```http
GET /api/patients/me/bmi
Authorization: Bearer {token}
```

### Doktor Endpoints

#### Doktor Arama (Public)
```http
GET /api/doctors/public/search?specialty=Kardiyoloji&province=İstanbul&minRating=4.0
```

#### Çalışma Saatleri Güncelle
```http
PATCH /api/doctors/me/working-hours
Authorization: Bearer {token}
Content-Type: application/x-www-form-urlencoded

workingDays=MONDAY,TUESDAY,WEDNESDAY&startTime=09:00&endTime=17:00
```

### Admin Endpoints

#### Kullanıcı Listesi
```http
GET /api/admin/users?page=0&size=20
Authorization: Bearer {admin-token}
```

#### Doktor Onaylama
```http
PATCH /api/doctors/{doctorId}/verification
Authorization: Bearer {admin-token}
Content-Type: application/x-www-form-urlencoded

status=VERIFIED
```

## 🗄️ Veritabanı Yapısı

### Kullanıcı Hiyerarşisi
```
User (Base Entity)
├── Patient (Hasta bilgileri)
├── Doctor (Doktor bilgileri)
└── Admin (Yönetici bilgileri)
```

### Temel Collections
- **users** - Tüm kullanıcıların base bilgileri
- **patients** - Hasta-specific bilgiler
- **doctors** - Doktor-specific bilgiler  
- **admins** - Admin-specific bilgiler

### Örnek Document Yapısı

#### Patient Document
```json
{
  "_id": "patient123",
  "firstName": "Ahmet",
  "lastName": "Yılmaz",
  "email": "ahmet@example.com",
  "role": "PATIENT",
  "tcKimlikNo": "12345678901",
  "bloodType": "A+",
  "heightCm": 175,
  "weightKg": 75.5,
  "allergies": "Polen alerjisi",
  "chronicDiseases": "Hipertansiyon",
  "hasInsurance": true,
  "insuranceCompany": "SGK",
  "emergencyContactName": "Fatma Yılmaz",
  "emergencyContactPhone": "+905551234567",
  "created_at": "2024-01-01T10:00:00Z"
}
```

## 🔐 Güvenlik

### JWT Token Yapısı
```json
{
  "header": {
    "alg": "HS512",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user123",
    "role": "PATIENT",
    "email": "user@example.com",
    "exp": 1640995200
  }
}
```

### Role-Based Access Control

| Endpoint | PATIENT | DOCTOR | ADMIN |
|----------|---------|--------|-------|
| `/api/patients/me` | ✅ | ❌ | ✅ |
| `/api/doctors/public/*` | ✅ | ✅ | ✅ |
| `/api/doctors/me` | ❌ | ✅ | ✅ |
| `/api/admin/*` | ❌ | ❌ | ✅ |

### Güvenlik Headers
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
X-Idempotency-Key: unique-request-id (POST/PUT istekleri için)
```

## 📊 Monitoring ve Analytics

### Health Check
```http
GET /api/test/health
Response: {
  "success": true,
  "data": {
    "status": "UP",
    "timestamp": "2024-01-01T12:00:00Z",
    "message": "HealthVia Platform is running!"
  }
}
```

### İstatistikler
- Hasta sayıları (aktif, yeni kayıt)
- Doktor performans metrikleri
- Sistem kullanım istatistikleri
- Error rate ve response time

## 🚦 Durum Kodları

| HTTP Status | Açıklama |
|-------------|----------|
| 200 | Başarılı |
| 201 | Oluşturuldu |
| 400 | Geçersiz istek |
| 401 | Kimlik doğrulama gerekli |
| 403 | Yetkisiz erişim |
| 404 | Bulunamadı |
| 409 | Çakışma (duplicate data) |
| 500 | Sunucu hatası |

## 🔧 Yapılandırma

### Environment Variables
```bash
# MongoDB
MONGODB_URI=mongodb://admin:healthvia123@localhost:27017/healthvia?authSource=admin

# JWT
JWT_SECRET=your-secret-key
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

### Profiller
- **dev** - Development environment
- **test** - Test environment  
- **prod** - Production environment

## 🧪 Testing

### API Testleri
```bash
# Health check
curl http://localhost:8080/api/test/health

# Register patient
curl -X POST http://localhost:8080/api/auth/register/patient \
  -H "Content-Type: application/json" \
  -d '{...patient-data...}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"SecurePass123!"}'
```

### Unit Tests
```bash
./mvnw test
```

## 📈 Roadmap

### v1.1 (Gelecek)
- [ ] Randevu sistemi
- [ ] Online video konsültasyon
- [ ] Push notifications
- [ ] Email servisi

### v1.2 (Gelecek)
- [ ] Ödeme sistemi entegrasyonu
- [ ] Klinik yönetimi
- [ ] Rapor sistemi
- [ ] Mobile API optimizasyonları



## 📞 İletişim

- **Email**: info@healthvia.com
- **GitHub**: [@healthvia](https://github.com/healthvia)
- **Documentation**: [API Docs](https://api.healthvia.com/docs)

---

⭐ Bu projeyi beğendiyseniz star vermeyi unutmayın!
