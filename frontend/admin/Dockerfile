FROM node:20-alpine AS dev
WORKDIR /app
COPY package.json package-lock.json ./
COPY applicant/package.json ./applicant/
COPY lender/package.json ./lender/
COPY admin/package.json ./admin/
RUN npm ci
COPY admin/ ./admin/
WORKDIR /app/admin
EXPOSE 3000
CMD ["npm", "start"]

FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
COPY applicant/package.json ./applicant/
COPY lender/package.json ./lender/
COPY admin/package.json ./admin/
RUN npm ci
COPY admin/ ./admin/
WORKDIR /app/admin
RUN npm run build

FROM nginx:1.25-alpine AS prod
COPY --from=build /app/admin/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
