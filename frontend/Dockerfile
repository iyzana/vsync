FROM node:14-alpine AS build

WORKDIR /app

COPY package.json package.json
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/build /data/dist
COPY nginx.conf /etc/nginx/conf.d/default.conf
