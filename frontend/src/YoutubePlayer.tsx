import './YoutubePlayer.css';
import YouTube from 'react-youtube';
import { useCallback, useEffect, useState } from 'react';
import { EmbeddedPlayerProps } from './Player';

const opts = {
  width: '100%',
  height: '100%',
  playerVars: {
    autoplay: 1,
    modestbranding: 1,
    rel: 0,
  },
};

function YoutubePlayer({
  msg,
  videoUrl,
  sendMessage,
  setOverlay,
  volume,
  setVolume,
  initialized,
  setInitialized,
}: EmbeddedPlayerProps) {
  const [player, setPlayer] = useState<any | null>(null);
  const [preloadTime, setPreloadTime] = useState<number | null>(null);
  const [nextReadyCheck, setNextReadyCheck] = useState<number>(100);
  const [oldState, setOldState] = useState<number>(
    YouTube.PlayerState.UNSTARTED,
  );
  const [hasEverPlayed, setHasEverPlayed] = useState<boolean>(false);

  useEffect(() => {
    if (!msg) {
      return;
    } else if (msg === 'play') {
      if (player?.getPlayerState() !== YouTube.PlayerState.PLAYING) {
        player?.playVideo();
      }
    } else if (msg.startsWith('pause')) {
      if (
        player?.getPlayerState() === YouTube.PlayerState.PLAYING ||
        player?.getPlayerState() === YouTube.PlayerState.BUFFERING
      ) {
        if (hasEverPlayed) {
          console.log('pause setting pause');
          player?.pauseVideo();
        }
      }
      const timestamp = parseFloat(msg.split(' ')[1]);
      const shouldSeek = Math.abs(player?.getCurrentTime() - timestamp) > 1;
      if (shouldSeek) {
        setTimeout(() => {
          player?.seekTo(timestamp, true);
        }, 150);
      }
    } else if (msg.startsWith('ready?')) {
      const timestamp = parseFloat(msg.split(' ')[1]);
      setNextReadyCheck(100);
      setPreloadTime(timestamp);
    }
  }, [msg, player, hasEverPlayed]);

  useEffect(() => {
    if (msg && msg.startsWith('video')) {
      setHasEverPlayed(false);
    }
  }, [msg, setHasEverPlayed]);

  const onStateChange = useCallback(() => {
    const memOldState = oldState;
    const newState = player.getPlayerState();
    setOldState(newState);
    console.log('player state changed to ' + newState);
    if (newState === YouTube.PlayerState.PAUSED) {
      if (preloadTime === null) {
        setOverlay('PAUSED');
      }
      if (hasEverPlayed) {
        console.log(`sending pause ${player.getCurrentTime()}`);
        sendMessage(`pause ${player.getCurrentTime()}`);
      }
    } else if (newState === YouTube.PlayerState.PLAYING) {
      setHasEverPlayed(true);
      if (initialized) {
        console.log(`sending play ${player.getCurrentTime()}`);
        sendMessage(`play ${player.getCurrentTime()}`);
        setOverlay(null);
      } else {
        setInitialized(true);
        // the youtube player behaves strange if it is paused
        // almost immediately after starting, so delay sync
        console.log('sending sync');
        sendMessage('sync');
      }
    } else if (newState === YouTube.PlayerState.ENDED) {
      console.log(`sending end`);
      sendMessage(`end ${videoUrl}`);
    } else if (
      newState === YouTube.PlayerState.BUFFERING &&
      memOldState === YouTube.PlayerState.PLAYING
    ) {
      console.log(`sending buffer ${player.getCurrentTime()}`);
      sendMessage(`buffer ${player.getCurrentTime()}`);
    }
  }, [
    player,
    videoUrl,
    oldState,
    setOldState,
    hasEverPlayed,
    setHasEverPlayed,
    preloadTime,
    initialized,
    setInitialized,
    sendMessage,
    setOverlay,
  ]);

  useEffect(() => {
    if (preloadTime === null) {
      return;
    }

    const readyCheck = setInterval(() => {
      if (preloadTime === null) {
        return;
      }
      console.log(
        `ready? ${player?.getPlayerState()} ${preloadTime} ${player?.getCurrentTime()} ${player?.getVideoLoadedFraction()}`,
      );
      if (
        Math.abs(player?.getCurrentTime() - preloadTime) <= 1 &&
        player?.getPlayerState() !== YouTube.PlayerState.BUFFERING
      ) {
        console.log(`sending ready ${player?.getCurrentTime()}`);
        sendMessage(`ready ${player?.getCurrentTime()}`);
        setPreloadTime(null);
      } else {
        if (
          player?.getPlayerState() === YouTube.PlayerState.PLAYING ||
          player?.getPlayerState() === YouTube.PlayerState.BUFFERING
        ) {
          if (hasEverPlayed) {
            console.log('ready setting pause');
            player?.pauseVideo();
          }
        }

        player?.seekTo(preloadTime, true);
        // youtube does not update videoLoadedFraction
        // without updated seek event
        setPreloadTime((time) => time!! + 0.01);
        setNextReadyCheck((check) => Math.min(5000, check * 2));
      }
    }, nextReadyCheck);

    return () => clearInterval(readyCheck);
  }, [
    player,
    preloadTime,
    hasEverPlayed,
    setPreloadTime,
    nextReadyCheck,
    setNextReadyCheck,
    sendMessage,
  ]);

  // sync volume before disposing youtube player
  useEffect(() => {
    return () => {
      if (player) {
        setVolume(player.getVolume() / 100);
      }
    };
  }, [player, setVolume]);

  const onPlaybackRateChange = (event: { target: any; data: number }) => {
    sendMessage(`speed ${event.data}`);
  };

  const videoId = new URL(videoUrl).searchParams.get('v') ?? videoUrl;

  const onReady = useCallback(
    ({ target: player }: { target: any }) => {
      setPlayer(player);
      if (volume) {
        player.setVolume(volume * 100);
      }
    },
    [setPlayer, volume],
  );

  return (
    <YouTube
      className="youtube-player"
      opts={opts}
      videoId={videoId}
      onReady={onReady}
      onStateChange={onStateChange}
      onPlaybackRateChange={onPlaybackRateChange}
    ></YouTube>
  );
}

export default YoutubePlayer;
