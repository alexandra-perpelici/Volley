# Volley

## Supabase Setup

This app uses Supabase Postgres as the production database. The project URL and database host are configured in `src/main/resources/application.properties`; keep the database password out of Git.

Set the password before starting the app:

```powershell
$env:SUPABASE_DB_PASSWORD="<your-supabase-database-password>"
.\mvnw.cmd spring-boot:run
```

Optional overrides:

```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://aws-1-eu-central-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0"
$env:SUPABASE_DB_USERNAME="postgres.gqxwanflddgeggfndrzh"
$env:SUPABASE_URL="https://gqxwanflddgeggfndrzh.supabase.co"
$env:SUPABASE_PUBLISHABLE_KEY="sb_publishable_zkjleEMVcxTcHWc-vG09qA_F1sjsSjx"
```

The schema initializer creates `users`, `courts`, and `reservations`, and seeds two volleyball courts.

## Push Notification Setup

The Android installable app is a PWA. Admin users can install it from Chrome and enable alerts for new reservations.

Set these environment variables in Render to activate push delivery:

```text
VAPID_PUBLIC_KEY=<generated-public-key>
VAPID_PRIVATE_KEY=<generated-private-key>
VAPID_SUBJECT=mailto:your-email@example.com
```

Generate a key pair with:

```powershell
npx --yes web-push generate-vapid-keys
```
