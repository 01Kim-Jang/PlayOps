FROM mcr.microsoft.com/playwright:v1.53.0-noble
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ENV PROJECTS_ROOT=/playwright-projects
CMD ["npm", "start"]
