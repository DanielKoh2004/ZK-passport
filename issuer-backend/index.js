import crypto from "node:crypto";
import express from "express";
import cors from "cors";
import dotenv from "dotenv";

dotenv.config();

// ---------------------------------------------------------------------------
// 1. Generate Government Ed25519 Master Keypair on startup
// ---------------------------------------------------------------------------
const { publicKey, privateKey } = crypto.generateKeyPairSync("ed25519");

const pubKeyHex = publicKey
  .export({ type: "spki", format: "der" })
  .toString("hex");

console.log("Government Ed25519 public key (hex):", pubKeyHex);

// ---------------------------------------------------------------------------
// 2. Express app
// ---------------------------------------------------------------------------
const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

// ---------------------------------------------------------------------------
// Simple in-memory rate limiter (max 30 req/min per IP)
// ---------------------------------------------------------------------------
const rateLimitMap = new Map();
const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX = 30;

function rateLimit(req, res, next) {
  const ip = req.ip || req.connection.remoteAddress;
  const now = Date.now();
  const entry = rateLimitMap.get(ip);

  if (!entry || now - entry.windowStart > RATE_LIMIT_WINDOW_MS) {
    rateLimitMap.set(ip, { windowStart: now, count: 1 });
    return next();
  }

  entry.count++;
  if (entry.count > RATE_LIMIT_MAX) {
    console.warn(`Rate limit exceeded for ${ip}`);
    return res.status(429).json({ error: "Too many requests. Try again later." });
  }
  return next();
}

// Apply rate limiting to session endpoints
app.use("/api/session", rateLimit);

// ---------------------------------------------------------------------------
// GET /api/public-key
// Returns the Government's public key in hex format.
// ---------------------------------------------------------------------------
app.get("/api/public-key", (_req, res) => {
  try {
    res.json({
      algorithm: "Ed25519",
      publicKey: pubKeyHex,
      format: "hex (DER / SPKI)",
    });
  } catch (err) {
    console.error("Error returning public key:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});

// ---------------------------------------------------------------------------
// POST /api/issue-passport
// Accepts passport data, builds a W3C Verifiable Credential, signs it with
// the Government private key, and returns the signed VC.
//
// Body: { did, name, dateOfBirth, passportNumber, nationality }
// ---------------------------------------------------------------------------
app.post("/api/issue-passport", (req, res) => {
  try {
    const { did, name, dateOfBirth, passportNumber, nationality } = req.body;

    // --- Basic validation ---------------------------------------------------
    if (!did || !name || !dateOfBirth || passportNumber == null || nationality == null) {
      return res.status(400).json({
        error:
          "Missing required fields: did, name, dateOfBirth, passportNumber, nationality",
      });
    }

    // --- Build the W3C Verifiable Credential --------------------------------
    const issuanceDate = new Date().toISOString();

    const credential = {
      "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://www.w3.org/2018/credentials/examples/v1",
      ],
      type: ["VerifiableCredential", "PassportCredential"],
      issuer: {
        id: "did:gov:passport-authority",
        name: "Government Passport Authority",
      },
      issuanceDate,
      credentialSubject: {
        id: did,
        name,
        dateOfBirth: Number(dateOfBirth),
        passportNumber: Number(passportNumber),
        nationality: Number(nationality),
      },
    };

    // --- Sign the credential ------------------------------------------------
    const payload = JSON.stringify(credential);
    const signature = crypto.sign(null, Buffer.from(payload), privateKey);
    const signatureHex = signature.toString("hex");

    // --- Attach proof object to the VC --------------------------------------
    const verifiableCredential = {
      ...credential,
      proof: {
        type: "Ed25519Signature2020",
        created: issuanceDate,
        verificationMethod: "did:gov:passport-authority#key-1",
        proofPurpose: "assertionMethod",
        proofValue: signatureHex,
      },
    };

    console.log(`Issued VC for subject ${did}`);
    res.status(201).json({ verifiableCredential });
  } catch (err) {
    console.error("Error issuing passport:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});

// ---------------------------------------------------------------------------
// 3. In-memory verification session tracker
// ---------------------------------------------------------------------------
const verificationSessions = new Map();

/**
 * POST /api/session/:nonce/status
 * Called by the Verifier app after successful offline verification.
 * Body: { "status": "success" }
 * Auto-expires after 60 seconds to prevent memory leaks.
 */
app.post("/api/session/:nonce/status", (req, res) => {
  try {
    const { nonce } = req.params;
    const { status } = req.body;

    if (!nonce || !status) {
      return res.status(400).json({ error: "Missing nonce or status" });
    }

    console.log(`Session ${nonce.substring(0, 8)}… → status: ${status}`);
    verificationSessions.set(nonce, { status, updatedAt: Date.now() });

    // Auto-delete after 60 seconds
    setTimeout(() => {
      if (verificationSessions.has(nonce)) {
        verificationSessions.delete(nonce);
        console.log(`Session ${nonce.substring(0, 8)}… expired and removed`);
      }
    }, 60_000);

    res.json({ nonce, status });
  } catch (err) {
    console.error("Error updating session status:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});

/**
 * GET /api/session/:nonce/status
 * Polled by the Citizen app to check if the Officer has verified successfully.
 * Returns { "status": "pending" } if the nonce is not yet known.
 */
app.get("/api/session/:nonce/status", (req, res) => {
  try {
    const { nonce } = req.params;
    const session = verificationSessions.get(nonce);

    if (session) {
      res.json({ status: session.status });
    } else {
      res.json({ status: "pending" });
    }
  } catch (err) {
    console.error("Error reading session status:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});

// ---------------------------------------------------------------------------
// Start
// ---------------------------------------------------------------------------
app.listen(PORT, () => {
  console.log(`Issuer backend running on http://localhost:${PORT}`);
});
