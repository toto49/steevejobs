import "dotenv/config";
import cors from "cors";
import express from "express";
import { AccessToken } from "livekit-server-sdk";

const app = express();
const PORT = process.env.PORT || 3001;

const LIVEKIT_API_KEY = process.env.LIVEKIT_API_KEY;
const LIVEKIT_API_SECRET = process.env.LIVEKIT_API_SECRET;
const LIVEKIT_URL = process.env.LIVEKIT_URL;

app.use(cors());
app.use(express.json());

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

app.get("/token", async (req, res) => {
  const roomName = req.query.roomName;
  const participantName = req.query.participantName;

  if (!roomName || !participantName) {
    return res.status(400).send("roomName et participantName sont requis.");
  }

  if (!LIVEKIT_API_KEY || !LIVEKIT_API_SECRET || !LIVEKIT_URL) {
    return res
      .status(500)
      .send(
        "Configurez LIVEKIT_API_KEY, LIVEKIT_API_SECRET et LIVEKIT_URL dans server/.env"
      );
  }

  const token = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET, {
    identity: participantName,
    name: participantName,
  });

  token.addGrant({
    roomJoin: true,
    room: roomName,
    canPublish: true,
    canSubscribe: true,
  });

  const jwt = await token.toJwt();

  res.json({
    token: jwt,
    url: LIVEKIT_URL,
  });
});

app.listen(PORT, () => {
  console.log(`Serveur de tokens LiveKit → http://localhost:${PORT}`);
});
