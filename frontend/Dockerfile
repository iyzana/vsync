FROM node:20-alpine AS build

WORKDIR /app
ENV INLINE_RUNTIME_CHUNK=false

COPY vendor vendor
RUN cd vendor/react-youtube/packages/react-youtube && yarn && yarn build

COPY package.json package.json
COPY yarn.lock yarn.lock
RUN yarn

COPY . .
RUN yarn build

FROM nginx:alpine

EXPOSE 80
COPY --from=build /app/build /data/dist
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
