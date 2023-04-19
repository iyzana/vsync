FROM node:14-alpine AS build

WORKDIR /app
ENV INLINE_RUNTIME_CHUNK=false

COPY package.json package.json
COPY yarn.lock yarn.lock
RUN yarn

COPY . .
RUN yarn build

FROM nginx:alpine

EXPOSE 80
COPY --from=build /app/build /data/dist
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
