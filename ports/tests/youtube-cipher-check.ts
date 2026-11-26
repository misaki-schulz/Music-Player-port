// Run with Deno and an installed EJS source directory plus player 92eef845 (IAS).
// No audio, OAuth or Minecraft is involved. Inputs can be downloaded beforehand.
const [ejsDirectory, playerFile, mode = "fixed"] = Deno.args;
if (!ejsDirectory || !playerFile) {
  throw new Error("Usage: youtube-cipher-check.ts <file-URL-of-EJS-directory/> <92eef845-IAS.js> [old|fixed]");
}
const { preprocessPlayer, getFromPrepared } = await import(
  new URL("src/yt/solver/solvers.ts", ejsDirectory).href
);
let solvers;
try {
  solvers = getFromPrepared(preprocessPlayer(await Deno.readTextFile(playerFile)));
} catch (error) {
  if (mode === "old" && String(error).includes("addOnDisposeCallback")) {
    console.log("PASS: old EJS reproduced a.addOnDisposeCallback is not a function");
    Deno.exit(0);
  }
  throw error;
}
if (mode === "old") throw new Error("Old EJS unexpectedly accepted the regression player");
// Known vector from yt-dlp/ejs commit 40b7a7d4aee5, test for player 92eef845.
const actual = solvers.n("rYKKd90cGmViBQaX");
if (actual !== "3tU2ya-IJqhNt") throw new Error("Incorrect n signature: " + actual);
console.log("PASS: fixed EJS matches the upstream n signature vector for player 92eef845");
