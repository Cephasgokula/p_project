FROM node:20-alpine AS dev
WORKDIR /app
COPY package.json package-lock.json ./
COPY applicant/package.json ./applicant/
COPY lender/package.json ./lender/
COPY admin/package.json ./admin/
RUN npm ci
COPY applicant/ ./applicant/
WORKDIR /app/applicant
EXPOSE 3000
CMD ["npm", "start"]

FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
COPY applicant/package.json ./applicant/
COPY lender/package.json ./lender/
COPY admin/package.json ./admin/
RUN npm ci
COPY applicant/ ./applicant/
WORKDIR /app/applicant
RUN npm run build

FROM nginx:1.25-alpine AS prod
COPY --from=build /app/applicant/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
