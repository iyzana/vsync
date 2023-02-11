# vsync

Allows for synchronous watching of online-videos with other humans.
Supports YouTube videos as well as videos from many other websites.
Provides a queue to watch multiple videos one after another.
Go to `https://vsync.randomerror.de/` and share the generated link for others to join.

All connected humans can queue, play/pause or seek in the video,
the control is not limited to the creator of the room.
For YouTube videos the YouTube player, including all its shortcuts, can be utilized fully.
No pop-ups asking you to create/join a room, no strange external control bar.

Queue videos by Link, Youtube-Id or any query.
When entering a query the first YouTube result will be added to the queue, use at your own risk.

![screenshot](./screenshot.png)

## todo

- playback speed synchronization
- not sure what happens for people without an [ad blocker](https://github.com/gorhill/uBlock/)
- playlist support
- handle unplayable videos

## running using docker

Pre-built docker images are available at `ghcr.io/iyzana/yt-sync-backend:latest` and
`ghcr.io/iyzana/yt-sync-frontend:latest` for the backend and fronted respectively.

An example docker-compose file if provided at `docker-compose.yml`.
This file assumes that an docker network named `nginx_default` already exists and that another
container serves as a reverse proxy for both services.

An example nginx `server {}`-block is provided at `nginx-docker.conf` for how to configure nginx as
a reverse proxy for these services.

## manually building and running the backend

The backend is a Kotlin application, a Java 17 installation is required to compile and run it.

To compile the backend on Linux run the following commands:

```sh
cd backend
./gradlew shadowJar
```

The Jar-file of the application will be written to `build/libs/yt-sync-all.jar`.
Copy it to your preferred location and then run it using `java -jar yt-sync-all.jar`.
The application requires `yt-dlp` to be installed for fetching video information.

## manually building and running the frontend

Building the fronted requires a recent [nodejs](https://nodejs.org/en/), as well as
[yarn](https://yarnpkg.com/) installation.

To build the frontend on Linux run the following commands:

```sh
cd frontend
yarn
yarn build
```

The build output will be written to the `build` directory.
To serve the frontend a webserver is required.
An example configuration for the webserver [nginx](https://nginx.org/en/) is included at
`nginx.conf`.
The configuration expects nginx to be started inside the `build` directory.
