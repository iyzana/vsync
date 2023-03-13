import './YoutubePlayer.css';
import YouTube, { YouTubePlayer } from 'react-youtube';
import { useCallback, useContext, useEffect, useState } from 'react';
import { EmbeddedPlayerProps } from './Player';
import { useWebsocketMessages } from '../hook/websocket-messages';
import { WebsocketContext } from '../context/websocket';
import OverlayState from '../model/Overlay';

const opts = {
  width: '100%',
  height: '100%',
  playerVars: {
    autoplay: 1,
    modestbranding: 1,
    rel: 0,
  },
};

const getVideoId = (videoUrl: string) => {
  const url = new URL(videoUrl);
  const queryParam = url.searchParams.get('v');
  if (queryParam) {
    return queryParam;
  }
  const path = url.pathname;
  if (path.startsWith('/embed/')) {
    return path.substring('/embed/'.length);
  }
  if (path.startsWith('/shorts/')) {
    return path.substring('/shorts/'.length);
  }
  if (path.startsWith('/')) {
    return path.substring('/'.length);
  }
};

function YoutubePlayer({
  videoUrl,
  setOverlay,
  volume,
  setVolume,
  initialized,
  setInitialized,
}: EmbeddedPlayerProps) {
  const [player, setPlayer] = useState<YouTubePlayer | null>(null);
  const [preloadTime, setPreloadTime] = useState<number | null>(null);
  const [nextReadyCheck, setNextReadyCheck] = useState<number>(100);
  const [oldState, setOldState] = useState<number>(
    YouTube.PlayerState.UNSTARTED,
  );
  const [hasEverPlayed, setHasEverPlayed] = useState<boolean>(false);
  const { sendMessage } = useContext(WebsocketContext);

  useWebsocketMessages(
    (msg: string) => {
      if (!player) {
        return;
      }
      if (msg === 'play') {
        console.log('processing server message play');
        if (player?.getPlayerState() !== YouTube.PlayerState.PLAYING) {
          player?.playVideo();
        }
      } else if (msg.startsWith('pause')) {
        console.log('processing server message pause');
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
        console.log('processing server message ready');
        const timestamp = parseFloat(msg.split(' ')[1]);
        setNextReadyCheck(100);
        setPreloadTime(timestamp);
      } else if (msg.startsWith('video')) {
        console.log('processing server message video');
        if (hasEverPlayed) {
          console.log(
            'storing player volume live ' +
              player.isMuted() +
              ' ' +
              player.getVolume() / 100,
          );
          if (player.isMuted()) {
            setVolume(0);
          } else {
            setVolume(player.getVolume() / 100);
          }
        }
        setHasEverPlayed(false);
      }
    },
    [player, hasEverPlayed, setVolume],
  );

  const onStateChange = useCallback(() => {
    const newState = player.getPlayerState();
    setOldState(newState);
    console.log('player state changed to ' + newState);
    if (newState === YouTube.PlayerState.PAUSED) {
      if (preloadTime === null) {
        setOverlay(OverlayState.PAUSED);
      }
      if (hasEverPlayed) {
        sendMessage(`pause ${player.getCurrentTime()}`);
      }
    } else if (newState === YouTube.PlayerState.PLAYING) {
      setHasEverPlayed(true);
      if (initialized) {
        sendMessage(`play ${player.getCurrentTime()}`);
        setOverlay(OverlayState.NONE);
      } else {
        setInitialized(true);
        sendMessage('sync');
      }
    } else if (newState === YouTube.PlayerState.ENDED) {
      sendMessage(`end ${videoUrl}`);
    } else if (
      newState === YouTube.PlayerState.BUFFERING &&
      oldState === YouTube.PlayerState.PLAYING
    ) {
      sendMessage(`buffer ${player.getCurrentTime()}`);
    }
  }, [
    player,
    videoUrl,
    oldState,
    hasEverPlayed,
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
  }, [player, preloadTime, hasEverPlayed, nextReadyCheck, sendMessage]);

  // sync volume before disposing youtube player
  useEffect(() => {
    return () => {
      if (player) {
        console.log(
          'storing player volume live ' +
            player.isMuted() +
            ' ' +
            player.getVolume() / 100,
        );
        if (player.isMuted()) {
          setVolume(0);
        } else {
          setVolume(player.getVolume() / 100);
        }
      }
    };
  }, [player, setVolume, setOverlay]);

  const onPlaybackRateChange = useCallback(
    (event: { target: any; data: number }) => {
      sendMessage(`speed ${event.data}`);
    },
    [sendMessage],
  );

  const onReady = useCallback(
    ({ target: player }: { target: any }) => {
      setPlayer(player);
      if (volume) {
        console.log('setting yt player volume ' + volume);

        if (volume === 0) {
          player.setVolume(0);
          player.mute();
        } else if (volume) {
          player.unMute();
          player.setVolume(volume * 100);
        }
      }

      if (!initialized) {
        setOverlay(OverlayState.UNSTARTED);
      }
    },
    [volume, initialized, setOverlay],
  );

  return (
    <YouTube
      className="youtube-player"
      opts={opts}
      videoId={getVideoId(videoUrl)}
      onReady={onReady}
      onStateChange={onStateChange}
      onPlaybackRateChange={onPlaybackRateChange}
    ></YouTube>
  );
}

export default YoutubePlayer;
