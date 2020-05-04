import React, { useState, useEffect, useCallback } from "react";
import "./App.css";
import YtEmbed from "./YtEmbed";
import YouTube from "react-youtube";

const ws = new WebSocket("ws://succcubbus.ddns.net:4567/room");
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
  const [prepare, setPrepare] = useState<number | null>(null);
  const [oldState, setOldState] = useState<number>(
    YouTube.PlayerState.UNSTARTED
  );

  useEffect(() => {
    ws.onclose = () => {
      console.log("disconnected");
    };
    ws.onmessage = (ev: MessageEvent) => {
      const msg = ev.data as string;
      console.log(`received ${msg}`);
      if (msg.startsWith("create")) {
        const roomId = msg.split(" ")[1];
        window.history.pushState(roomId, "", `/${roomId}`);
      } else if (msg === "invalid command") {
        window.location.href = "/";
      } else if (msg === "play") {
        if (player?.getPlayerState() !== YouTube.PlayerState.PLAYING) {
          player?.playVideo();
        }
      } else if (msg.startsWith("pause")) {
        const timestamp = parseFloat(msg.split(" ")[1]);
        if (player?.getPlayerState() === YouTube.PlayerState.PLAYING) {
          player?.pauseVideo();
        }
        const shouldSeek = Math.abs(player?.getCurrentTime() - timestamp) > 1;
        if (shouldSeek) {
          player?.seekTo(timestamp, true);
        }
      } else if (msg.startsWith("ready?")) {
        const timestamp = parseFloat(msg.split(" ")[1]);
        setPrepare(timestamp);
      }
    };
    const readyCheck = setInterval(() => {
      const currentFraction = player?.getCurrentTime() / player?.getDuration();
      const targetPreload = 5 / player?.getDuration();
      if (prepare === null) {
        clearInterval(readyCheck);
        return;
      }
      console.log(
        `ready? ${player?.getPlayerState()} ${prepare} ${player?.getCurrentTime()} ${player?.getVideoLoadedFraction()}`
      );
      if (
        Math.abs(player?.getCurrentTime() - prepare) <= 1 &&
        player?.getVideoLoadedFraction() - currentFraction >= targetPreload
      ) {
        console.log(`sending ready ${player?.getCurrentTime()}`);
        ws.send(`ready ${player?.getCurrentTime()}`);
        setPrepare(null);
      } else {
        player?.seekTo(prepare, true);
      }
    }, 250);

    return () => clearInterval(readyCheck);
  }, [player, prepare, setPrepare]);

  const onStateChange = useCallback(() => {
    const memOldState = oldState;
    const newState = player.getPlayerState();
    setOldState(newState);
    console.log("player state changed to " + newState);
    if (newState === YouTube.PlayerState.PAUSED) {
      console.log(`sending pause ${player.getCurrentTime()}`);
      ws.send(`pause ${player.getCurrentTime()}`);
    } else if (newState === YouTube.PlayerState.PLAYING) {
      console.log(`sending play ${player.getCurrentTime()}`);
      ws.send(`play ${player.getCurrentTime()}`);
    } else if (
      newState === YouTube.PlayerState.BUFFERING &&
      memOldState === YouTube.PlayerState.PLAYING
    ) {
      console.log(`sending buffer ${player.getCurrentTime()}`);
      ws.send(`buffer ${player.getCurrentTime()}`);
    } else if (newState === YouTube.PlayerState.UNSTARTED) {
      console.log("sending sync");
      ws.send("sync");
    }
  }, [player, oldState, setOldState]);

  const ready = useCallback((player: any) => {
    setPlayer(player);
  }, []);
  return (
    <main>
      <div className="container">
        <YtEmbed
          videoId="s8QYxmpuyxg"
          onStateChange={onStateChange}
          setPlayer={ready}
        />
      </div>
    </main>
  );
}

export default App;
