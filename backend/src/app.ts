import express, { type Express } from 'express';
import { env } from './config/env';

export function createApp(): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  // M1_3 APIS
  app.get('/name', (_req, res) => {
    res.json(
      { firstName: 'Tera', 
         lastName: 'Wan' }
    );
  });

  app.get('/server_time', (_req, res) => {
    const currentTime = new Date();
    const currentHour = currentTime.getHours();
    const currentMinute = currentTime.getMinutes();
    const currentSecond = currentTime.getSeconds();
    const currentHourString = currentHour.toString().padStart(2, '0');
    const currentMinuteString = currentMinute.toString().padStart(2, '0');
    const currentSecondString = currentSecond.toString().padStart(2, '0');
    const formattedTime = `${currentHourString}:${currentMinuteString}:${currentSecondString}`;
    const offsetMinutes = currentTime.getTimezoneOffset();
    const offsetSign = offsetMinutes > 0 ? '-' : '+';
    const offsetHours = Math.floor(Math.abs(offsetMinutes) / 60);
    const offsetMinutesRemainder = Math.abs(offsetMinutes) % 60;
    const offsetHoursString = offsetHours.toString().padStart(2, '0');
    const offsetMinutesString = offsetMinutesRemainder.toString().padStart(2, '0');
    const offsetString = `${offsetSign}${offsetHoursString}:${offsetMinutesString}`;
    const formattedServerTime = `${formattedTime} GMT${offsetString}`;
    res.json({ serverTime: formattedServerTime });
  });

  app.get('/server_ip', (_req, res) => {
    res.json({ serverIP: env.serverPublicIp });
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
