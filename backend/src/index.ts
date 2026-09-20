import { createApp } from './app';
import { env } from './config/env';
// Websocket is a client to recive data from the course server
import WebSocket, { WebSocketServer } from 'ws';

const app = createApp();

const server = app.listen(env.port, () => {
  console.log(`Server listening on port ${env.port}`);
});

// WebSocket server to send data to android
const wss = new WebSocketServer({
  server,
  path: '/ws',
});

wss.on('connection', (socket) => {
  console.log('WebSocket client connected');

  socket.on('close', () => {
    console.log('WebSocket client disconnected');
  });
});

// WebSocket client to connect to the course server
const courseSocket = new WebSocket('wss://8.229.22.124');

courseSocket.on('message', (data) => {
  console.log('Course message:', data.toString());

  for (const client of wss.clients) {
    if (client.readyState === WebSocket.OPEN) {
      client.send(data);
    }
  }
});

courseSocket.on('error', (error) => {
  console.error('Course WebSocket error:', error);
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}
