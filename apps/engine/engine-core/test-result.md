# Test Execution Result: Engine Core
**Datum posledního běhu:** 2026-03-13

## Build Status: PARTIALLY FIXED

## Souhrn oprav (Summary of Fixes)
| Kategorie | Status | Popis |
| :--- | :--- | :--- |
| Checkstyle Configuration | ✅ FIXED | Opraveny neplatné vlastnosti v checkstyle.xml |
| Java Version | ✅ FIXED | Aktualizováno z 21 na 25 pro kompatibilitu |
| StructuredLoggingConfig | ✅ FIXED | Odstraněna nepodporovaná metoda setSeverity() |
| Circular Dependency | ✅ FIXED | Presunut SecurityConfig z common do auth modulu |
| Admin Module gRPC | ❌ OPEN | Chybí závislosti a vygenerované proto soubory |

## Opravené chyby (Fixed Errors)

### 1. Checkstyle Configuration Error
**Problem:** `AbstractClassName.ignoreName` očekává Boolean, ale byl nastaven regex pattern
**Řešení:** Změněno na `ignoreName="true"` v `packages/java-base/checkstyle.xml`

### 2. Checkstyle ParameterName Error  
**Problem:** Vlastnost `ignoreOverriddenMethods` neexistuje v Checkstyle 10.x
**Řešení:** Odstraněna neplatná konfigurace

### 3. Checkstyle NoWhitespaceAfter Tokens Error
**Problem:** Neplatné tokeny `LITERAL_ASSERT`, `LITERAL_SWITCH`, `LAND` atd.
**Řešení:** Aktualizován seznam tokenů v checkstyle.xml

### 4. Java Version Error
**Problem:** `invalid source release 21 with --enable-preview`
**Řešení:** Aktualizováno `java.version`, `maven.compiler.source/target` a `release` z 21 na 25 v `packages/java-base/pom.xml`

### 5. StructuredLoggingConfig Compilation Error
**Problem:** Metoda `setSeverity()` neexistuje v logstash-logback-encoder 8.0
**Řešení:** Odstraněn volání `fieldNames.setSeverity("severity")` v `packages/java-base/java-base-observability/.../StructuredLoggingConfig.java`

### 6. Circular Dependency Error
**Problem:** Common modul závisí na auth službách, ale auth závisí na common
**Řešení:** Presunut `SecurityConfig.java` z `common` do `auth` modulu a aktualizován package declaration

## Otevřené problémy (Open Issues)

### Admin Module gRPC Compilation
**Problem:** 
- Chybí závislost na `grpc-spring-boot-starter`
- Chybí vygenerované proto Java třídy z `packages/protos/integration/v1/smart_persistence.proto`

**Soubory s chybami:**
- `apps/engine/engine-core/admin/src/main/java/com/reportplatform/admin/grpc/SmartPersistenceGrpcService.java`

**Možné řešení:**
1. Přidat závislost na grpc-spring-boot-starter do admin/pom.xml
2. Spustit generování proto kódů z `packages/protos`
3. Přidat závislost na vygenerované proto artifacty

## Doporučení (Recommendations)
1. Sestavit protos modul před sestavením engine-core
2. Přidat chybějící závislosti do admin/pom.xml
3. Zvážit odstranění nebo dokončení grpc služby pokud není potřebná
