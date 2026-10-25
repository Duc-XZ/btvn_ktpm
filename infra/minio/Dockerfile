FROM golang:1.24-alpine AS build
ARG MINIO_VERSION=latest
ENV CGO_ENABLED=0
RUN go install -v github.com/minio/minio@${MINIO_VERSION}

FROM alpine:3.24
RUN apk add --no-cache ca-certificates curl \
    && addgroup -S -g 10001 minio \
    && adduser -S -D -H -u 10001 -G minio minio \
    && mkdir -p /data \
    && chown minio:minio /data
COPY --from=build /go/bin/minio /usr/local/bin/minio
USER minio:minio
EXPOSE 9000 9001
HEALTHCHECK --interval=5s --timeout=5s --start-period=10s --retries=12 \
    CMD curl -fsS http://127.0.0.1:9000/minio/health/live || exit 1
ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]
