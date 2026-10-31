import { app } from "./app";
import { env } from "./env";

console.log(`PocketLedger API listening on http://0.0.0.0:${env.PORT}`);

export default {
  port: env.PORT,
  hostname: "0.0.0.0",
  fetch: app.fetch,
};
