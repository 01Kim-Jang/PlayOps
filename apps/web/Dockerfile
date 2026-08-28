FROM node:22-bookworm
WORKDIR /app
COPY package*.json ./
RUN npm install --no-package-lock
COPY . .
EXPOSE 3000
CMD ["npm", "run", "dev"]
