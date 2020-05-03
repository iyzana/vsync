import React, { useState, useEffect } from "react";
import "./App.css";
import YtEmbed from "./YtEmbed";
import YouTube from "react-youtube";

function App() {
  const [player, setPlayer] = useState<any | null>(null);
  const [ws, setWs] = useState<WebSocket | null>(null);
  const [expectedEvents, setExpectedEvents] = useState<number[]>([]);

  useEffect(() => {
    const ws = new WebSocket("ws://localhost:4567/room");
    setWs(ws);
    return () => ws.close();
  }, []);

  useEffect(() => {
    if (ws === null) {
      return;
    }
    ws.onopen = () => {
      const path = window.location.pathname;
      console.log(path);
      if (path === "" || path === "/") {
        ws.send("create");
      } else {
        const roomId = path.substring(1);
        ws.send(`join ${roomId}`);
      }
    };
  }, [ws]);

  useEffect(() => {
    if (ws === null) {
      return;
    }
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
        if (
          player.getPlayerState() !== YouTube.PlayerState.PLAYING ||
          expectedEvents.indexOf(YouTube.PlayerState.PAUSED) !== -1
        ) {
          setExpectedEvents((ev) => [...ev, YouTube.PlayerState.PLAYING]);
          player?.playVideo();
        }
      } else if (msg === "pause") {
        if (
          player.getPlayerState() !== YouTube.PlayerState.PAUSED ||
          expectedEvents.indexOf(YouTube.PlayerState.PLAYING) !== -1
        ) {
          setExpectedEvents((ev) => [...ev, YouTube.PlayerState.PAUSED]);
          player?.pauseVideo();
        }
      }
    };
  }, [ws, player]);

  const onStateChange = () => {
    console.log("player state changed to " + player.getPlayerState());
    const expectedTransition = expectedEvents[0];
    if (player.getPlayerState() !== expectedTransition) {
      if (player.getPlayerState() === YouTube.PlayerState.PAUSED) {
        console.log("sending pause");
        ws?.send("pause");
      } else if (player.getPlayerState() === YouTube.PlayerState.PLAYING) {
        console.log("sending play");
        ws?.send("play");
      }
    } else {
      setExpectedEvents((ev) => ev.slice(1));
    }
  };
  const ready = (player: any) => {
    setPlayer(player);
  };
  return (
    <main>
      <div className="container">
        <YtEmbed
          videoId="5NPBIwQyPWE"
          onStateChange={onStateChange}
          setPlayer={ready}
        />
      </div>
    </main>
  );
}

export default App;

