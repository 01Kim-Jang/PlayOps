ARG PLAYWRIGHT_VERSION=1.53.0
FROM mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}-noble

ARG PLAYWRIGHT_VERSION=1.53.0
ARG NODE_VERSION=20.20.2

ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENV PATH="/opt/node/bin:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl xz-utils \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    arch="$(dpkg --print-architecture)"; \
    case "$arch" in \
      amd64) node_arch="x64" ;; \
      arm64) node_arch="arm64" ;; \
      *) echo "Unsupported architecture: $arch" >&2; exit 1 ;; \
    esac; \
    curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${node_arch}.tar.xz" -o /tmp/node.tar.xz; \
    mkdir -p /opt/node; \
    tar -xJf /tmp/node.tar.xz -C /opt/node --strip-components=1; \
    rm /tmp/node.tar.xz; \
    ln -sf /opt/node/bin/node /usr/local/bin/node; \
    ln -sf /opt/node/bin/npm /usr/local/bin/npm; \
    ln -sf /opt/node/bin/npx /usr/local/bin/npx; \
    corepack enable; \
    corepack prepare pnpm@9.15.9 --activate; \
    corepack prepare yarn@1.22.22 --activate; \
    npm install -g "@playwright/test@${PLAYWRIGHT_VERSION}"; \
    node -v; \
    npm -v; \
    pnpm -v

WORKDIR /workspace

CMD ["tail", "-f", "/dev/null"]
