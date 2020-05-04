import React, { useState, useEffect } from "react";
import "./App.css";
import YtEmbed from "./YtEmbed";
import YouTube from "react-youtube";

const ws = new WebSocket("ws://localhost:4567/room");

function App() {
  const [player, setPlayer] = useState<any | null>(null);

  useEffect(() => {
    ws.onopen = () => {
      const path = window.location.pathname;
      if (path === "" || path === "/") {
        ws.send("create");
      } else {
        const roomId = path.substring(1);
        ws.send(`join ${roomId}`);
      }
    };
  }, [ws]);

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
        if (player.getPlayerState() !== YouTube.PlayerState.PLAYING) {
          player?.playVideo();
        }
      } else if (msg === "pause") {
        if (player.getPlayerState() !== YouTube.PlayerState.PAUSED) {
          player?.pauseVideo();
        }
      }
    };
  }, [ws, player]);

  const onStateChange = () => {
    console.log("player state changed to " + player.getPlayerState());
    if (player.getPlayerState() === YouTube.PlayerState.PAUSED) {
      console.log("sending pause");
      ws.send("pause");
    } else if (player.getPlayerState() === YouTube.PlayerState.PLAYING) {
      console.log("sending play");
      ws.send("play");
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
