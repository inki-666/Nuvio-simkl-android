# Nuvio RPC Cloud Relay

## Generate a pairing token

Run:

openssl rand -hex 32

Keep this token private.

## Deploy to Google Cloud Run

From this cloud directory:

gcloud run deploy nuvio-rpc-relay \
  --source . \
  --region asia-south1 \
  --allow-unauthenticated \
  --set-env-vars NUVIO_PAIRING_TOKEN=YOUR_SECRET_TOKEN

Replace YOUR_SECRET_TOKEN with your generated token.

After deployment Google Cloud will give you a URL.

Example:

https://nuvio-rpc-relay-example.a.run.app

The Android app will use:

wss://nuvio-rpc-relay-example.a.run.app
