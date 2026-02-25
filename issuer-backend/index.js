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
// Start
// ---------------------------------------------------------------------------
app.listen(PORT, () => {
  console.log(`Issuer backend running on http://localhost:${PORT}`);
});
