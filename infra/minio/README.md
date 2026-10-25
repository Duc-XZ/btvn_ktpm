# MinIO for local development

The Compose stack builds MinIO from source using the official Go install workflow and stores data in the `minio_data` Docker volume. The first build takes longer because it compiles MinIO.

The API listens on `http://localhost:9000` and the Console on `http://localhost:9001`. Local credentials are supplied through `MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD`; do not commit them.

User Service creates the `avatars` bucket at startup, applies the configured CORS origins, and returns presigned `PUT` URLs that expire after 300 seconds. `MINIO_PUBLIC_ENDPOINT` must be reachable by the client that uploads the file. For the local Compose setup, it defaults to `http://localhost:9000`.
