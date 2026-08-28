FROM node:22-alpine
WORKDIR /app
COPY sandbox-proxy/proxy.js proxy.js
EXPOSE 3128
CMD ["node", "proxy.js"]
