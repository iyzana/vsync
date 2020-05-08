import React, { useState, useEffect, useCallback } from "react";
import "./App.css";
import YtEmbed from "./YtEmbed";
import YouTube from "react-youtube";
import Queue from "./Queue";
import QueueItem from "./QueueItem";
import Input from "./Input";

const server = "succcubbus.ddns.net:4567";

const ws = new WebSocket(`ws://${server}/room`);
ws.onopen = () => {
  const path = window.location.pathname;
  if (path === "" || path === "/") {
    ws.send("create");
  } else {
    const roomId = path.substring(1);
    ws.send(`join ${roomId}`);
  }
};

function App() {
  const [player, setPlayer] = useState<any | null>(null);
  const [preloadTime, setPreloadTime] = useState<number | null>(null);
  const [nextReadyCheck, setNextReadyCheck] = useState<number>(100);
  const [initialized, setInitialized] = useState<boolean>(false);
  const [oldState, setOldState] = useState<number>(
    YouTube.PlayerState.UNSTARTED
  );
  const [videoId, setVideoId] = useState<string>("");
  const [queue, setQueue] = useState<QueueItem[]>([]);
  const [errors, setErrors] = useState<string[]>([]);

  useEffect(() => {
    const interval = setInterval(() => {
      ws.send("ping");
    }, 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    ws.onclose = () => {
      console.log("disconnected");
      /* setErrors((errors) => [...errors, "Connection closed"]); */
    };
    ws.onerror = () => {
      console.log("error");
      setErrors((errors) => [...errors, "Connection lost"]);
    };
  }, [setErrors]);

  useEffect(() => {
    ws.onmessage = (ev: MessageEvent) => {
      const msg = ev.data as string;
      console.log(`received ${msg}`);
      if (msg.startsWith("create")) {
        const roomId = msg.split(" ")[1];
        window.history.pushState(roomId, "", `/${roomId}`);
      } else if (msg === "invalid command") {
        setErrors((errors) => [...errors, "You found a bug"]);
      } else if (msg === "server full") {
        setErrors((errors) => [...errors, "Server is too full"]);
      } else if (msg === "join err not-found") {
        setErrors((errors) => [...errors, "Room does not exist"]);
      } else if (msg === "play") {
        if (player?.getPlayerState() !== YouTube.PlayerState.PLAYING) {
          player?.playVideo();
        }
      } else if (msg.startsWith("pause")) {
        const timestamp = parseFloat(msg.split(" ")[1]);
        const shouldSeek = Math.abs(player?.getCurrentTime() - timestamp) > 1;
        if (shouldSeek) {
          setTimeout(() => {
            player?.seekTo(timestamp, true);
          }, 150);
        }
        if (player?.getPlayerState() !== YouTube.PlayerState.PAUSED) {
          console.log("pause setting pause");
          player?.pauseVideo();
        }
      } else if (msg.startsWith("ready?")) {
        const timestamp = parseFloat(msg.split(" ")[1]);
        setNextReadyCheck(100);
        setPreloadTime(timestamp);
      } else if (msg.startsWith("video")) {
        const videoId = msg.split(" ")[1];
        setVideoId(videoId);
      } else if (msg.startsWith("queue add")) {
        const msgParts = msg.split(" ");
        const queueItem: QueueItem = JSON.parse(msgParts.slice(2).join(" "));
        setQueue((queue) => [...queue, queueItem]);
      } else if (msg.startsWith("queue rm")) {
        const videoId = msg.split(" ")[2];
        setQueue((queue) => queue.filter((video) => video.videoId !== videoId));
      } else if (msg === "queue err not-found") {
        setErrors((errors) => [...errors, "Video not found"]);
      } else if (msg === "queue err duplicate") {
        setErrors((errors) => [...errors, "Already in queue"]);
      }
    };
    return () => {
      ws.onmessage = null;
    };
  }, [player, setPreloadTime, setQueue, setNextReadyCheck, setErrors]);

  useEffect(() => {
    if (preloadTime === null) {
      return;
    }

    const readyCheck = setInterval(() => {
      const currentFraction = player?.getCurrentTime() / player?.getDuration();
      const targetPreload = 5 / player?.getDuration();
      if (preloadTime === null) {
        return;
      }
      console.log(
        `ready? ${player?.getPlayerState()} ${preloadTime} ${player?.getCurrentTime()} ${player?.getVideoLoadedFraction()}`
      );
      const loaded = player?.getVideoLoadedFraction();
      if (
        Math.abs(player?.getCurrentTime() - preloadTime) <= 1 &&
        (loaded - currentFraction >= targetPreload || loaded === 1)
      ) {
        console.log(`sending ready ${player?.getCurrentTime()}`);
        ws.send(`ready ${player?.getCurrentTime()}`);
        setPreloadTime(null);
      } else {
        if (
          player?.getPlayerState() === YouTube.PlayerState.PLAYING ||
          player?.getPlayerState() === YouTube.PlayerState.BUFFERING
        ) {
          console.log("ready setting pause");
          player?.pauseVideo();
        }

        player?.seekTo(preloadTime, true);
        // youtube does not update videoLoadedFraction
        // without updated seek event
        setPreloadTime((time) => time!! + 0.01);
        setNextReadyCheck((check) => Math.min(5000, check * 2));
      }
    }, nextReadyCheck);

    return () => clearInterval(readyCheck);
  }, [player, preloadTime, setPreloadTime, nextReadyCheck, setNextReadyCheck]);

  const onStateChange = useCallback(() => {
    const memOldState = oldState;
    const newState = player.getPlayerState();
    setOldState(newState);
    console.log("player state changed to " + newState);
    if (newState === YouTube.PlayerState.PAUSED) {
      console.log(`sending pause ${player.getCurrentTime()}`);
      ws.send(`pause ${player.getCurrentTime()}`);
    } else if (newState === YouTube.PlayerState.PLAYING) {
      if (initialized) {
        console.log(`sending play ${player.getCurrentTime()}`);
        ws.send(`play ${player.getCurrentTime()}`);
      } else {
        setInitialized(true);
        // the youtube player behaves strange if it is paused
        // almost immediately after starting, so delay sync
        setTimeout(() => {
          console.log("sending sync");
          ws.send("sync");
        }, 500);
      }
    } else if (
      newState === YouTube.PlayerState.BUFFERING &&
      memOldState === YouTube.PlayerState.PLAYING
    ) {
      console.log(`sending buffer ${player.getCurrentTime()}`);
      ws.send(`buffer ${player.getCurrentTime()}`);
    }
  }, [player, oldState, setOldState, initialized, setInitialized]);

  const ready = useCallback((player: any) => {
    setPlayer(player);
  }, []);
  return (
    <div className="with-sidebar">
      <div>
        <section className="main">
          <main className="embed">
            <YtEmbed
              videoId={videoId}
              onStateChange={onStateChange}
              setPlayer={ready}
            />
          </main>
        </section>
        <section>
          <div className="control">
            <Queue
              videos={queue}
              removeVideo={(video) => ws.send(`queue rm ${video}`)}
            />
            <Input ws={ws} errors={errors} setErrors={setErrors} />
          </div>
        </section>
      </div>
    </div>
  );
}

export default App;
