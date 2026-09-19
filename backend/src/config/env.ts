import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

const serverPublicIp = process.env.SERVER_PUBLIC_IP;
if (serverPublicIp === undefined || serverPublicIp === '') {
  throw new Error('SERVER_PUBLIC_IP is required');
}

export const env = {
  port,
  serverPublicIp,
} as const;

