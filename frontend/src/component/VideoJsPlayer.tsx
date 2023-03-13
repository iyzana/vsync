import './VideoJsPlayer.css';
import { useContext, useEffect, useRef } from 'react';
import videojs, { VideoJsPlayerOptions } from 'video.js';
import 'video.js/dist/video-js.css';
import { EmbeddedPlayerProps } from './Player';
import { useWebsocketMessages } from '../hook/websocket-messages';
import { WebsocketContext } from '../context/websocket';
import OverlayState from '../model/Overlay';

const opts: VideoJsPlayerOptions = {
  autoplay: true,
  controls: true,
  responsive: true,
  fluid: false,
  userActions: {
    hotkeys: function (event) {
      const player = this as videojs.Player;
      const key = event.which;
      if (key === 32 /* space */ || key === 75 /* k */) {
        if (player.paused()) {
          player.play();
        } else {
          player.pause();
        }
      }
      if (key === 37 /* left */) {
        player.currentTime(player.currentTime() - 5);
      }
      if (key === 39 /* right */) {
        player.currentTime(player.currentTime() + 5);
      }
      if (key === 74 /* j */) {
        player.currentTime(player.currentTime() - 10);
      }
      if (key === 76 /* l */) {
        player.currentTime(player.currentTime() + 10);
      }
      if (key === 38 /* up */) {
        player.volume(player.volume() + 0.05);
      }
      if (key === 40 /* down */) {
        player.volume(player.volume() - 0.05);
      }
      if (key === 77 /* m */) {
        player.muted(!player.muted());
      }
      if (key === 70 /* f */) {
        if (player.isFullscreen()) {
          player.exitFullscreen();
        } else {
          player.requestFullscreen();
        }
      }
      if (key === 67 /* c */) {
        const textTracks = player.textTracks();
        for (let i = 0; i < textTracks.length; i++) {
          const track = textTracks[i];
          if (track.kind === 'captions') {
            if (track.mode !== 'showing') {
              track.mode = 'showing';
            } else {
              track.mode = 'hidden';
            }
            return;
          }
        }
      }
    },
  },
};

export const VideoJsPlayer = ({
  videoUrl,
  setOverlay,
  volume,
  setVolume,
  playbackPermission,
  gotPlaybackPermission,
}: EmbeddedPlayerProps) => {
  const placeholderRef = useRef<HTMLDivElement | null>(null);
  const playerRef = useRef<videojs.Player | null>(null);
  const waitReadyRef = useRef(false);
  const playbackPermissionRef = useRef(playbackPermission);
  const videoUrlRef = useRef(videoUrl);
  const { sendMessage } = useContext(WebsocketContext);

  useWebsocketMessages(
    (msg: string) => {
      const player = playerRef.current;
      if (!player) {
        return;
      }
      player.ready(function () {
        if (msg === 'play') {
          console.log('processing server message play');
          player.play();
        } else if (msg.startsWith('pause')) {
          console.log('processing server message pause');
          player.pause();
          const timestamp = parseFloat(msg.split(' ')[1]);
          const shouldSeek = Math.abs(player.currentTime() - timestamp) > 0.5;
          if (shouldSeek) {
            console.log('seeking due to pause');
            player.currentTime(timestamp);
          }
        } else if (msg.startsWith('ready?')) {
          console.log('processing server message ready');
          player.pause();
          const timestamp = parseFloat(msg.split(' ')[1]);
          const shouldSeek = Math.abs(player.currentTime() - timestamp) > 0.5;
          if (shouldSeek) {
            console.log('seeking due to ready');
            player.currentTime(timestamp);
          }

          waitReadyRef.current = true;
          console.log({ readyState: player.readyState() });
          if (player.readyState() >= 3) {
            sendMessage(`ready ${player.currentTime()}`);
          }
        }
      });
    },
    [sendMessage],
  );

  useEffect(() => {
    // make sure video.js is only initialized once
    if (!playerRef.current) {
      const placeholderElement = placeholderRef.current;
      if (!placeholderElement) return;
      const videoElement = placeholderElement.appendChild(
        document.createElement('video-js'),
      );

      if (!videoElement) return;

      playerRef.current = videojs(videoElement, opts);

      const player = playerRef.current;
      player.ready(() => {
        console.log('registering player hooks');
        player.on('play', function () {
          console.log('player hook play');
          if (playbackPermissionRef.current) {
            sendMessage(`play ${player.currentTime()}`);
            waitReadyRef.current = false;
            setOverlay(OverlayState.NONE);
          } else {
            gotPlaybackPermission();
            sendMessage('sync');
          }
        });
        player.on('pause', function () {
          console.log('player hook pause');
          if (!waitReadyRef.current) {
            sendMessage(`pause ${player.currentTime()}`);
            setOverlay(OverlayState.PAUSED);
          }
        });
        player.on('stalled', function () {
          console.log('player hook stalled');
          sendMessage(`buffer ${player.currentTime()}`);
        });
        player.on('seeked', function () {
          console.log('player hook seeked');
          if (player.paused()) {
            if (waitReadyRef.current) {
              sendMessage(`play ${player.currentTime()}`);
            } else {
              sendMessage(`pause ${player.currentTime()}`);
            }
          }
        });
        player.on('ended', function () {
          console.log('player hook ended');
          sendMessage(`end ${videoUrlRef.current}`);
        });
        player.on('ratechange', function () {
          console.log('player hook ratechange');
          sendMessage(`speed ${player.playbackRate()}`);
        });
        player.on('canplay', function () {
          console.log('player hook canplay');
          if (waitReadyRef.current) {
            console.log('canplay');
            sendMessage(`ready ${player.currentTime()}`);
          }
        });
        player.on('volumechange', function () {
          console.log('player hook volumechange');
          console.log(
            'storing videojs player volume ' +
              player.muted() +
              ' ' +
              player.volume(),
          );
          if (player.muted()) {
            setVolume(0);
          } else {
            setVolume(player.volume());
          }
        });

        console.log('setting videojs player volume ' + volume);
        if (volume === 0) {
          player.volume(0);
          player.muted(true);
        } else if (volume) {
          player.muted(false);
          player.volume(volume);
        }
      });
    }
  });

  useEffect(() => {
    playbackPermissionRef.current = playbackPermission;
  }, [playbackPermission]);

  useEffect(() => {
    if (!playerRef.current) {
      return;
    }
    const player = playerRef.current;
    player.ready(() => {
      videoUrlRef.current = videoUrl;
      player.src(videoUrl);
    });
  }, [videoUrl]);

  // dispose video.js when the component unmounts
  useEffect(() => {
    return () => {
      const player = playerRef.current;

      if (player) {
        player.dispose();
        playerRef.current = null;
        waitReadyRef.current = false;
      }
    };
  }, [setVolume]);

  // video.js </3 react
  return <div className="video-player" ref={placeholderRef}></div>;
};

export default VideoJsPlayer;
