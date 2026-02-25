// test.js – Quick smoke test for the Issuer Backend API
// Uses Node's native fetch (available in Node 18+).

const BASE_URL = "http://localhost:3000";

async function main() {
  try {
    // ----- 1. GET /api/public-key ------------------------------------------
    console.log("=== 1. Fetching Government Public Key ===\n");
    const pkRes = await fetch(`${BASE_URL}/api/public-key`);
    if (!pkRes.ok) throw new Error(`Public key request failed: ${pkRes.status}`);
    const pkData = await pkRes.json();
    console.log(JSON.stringify(pkData, null, 2));

    // ----- 2. POST /api/issue-passport -------------------------------------
    console.log("\n=== 2. Requesting Passport Credential ===\n");

    const mockPassport = {
      did: "did:device:abc123-test-device",
      name: "Ahmad bin Ibrahim",
      dateOfBirth: 20040512,        // matches ZK circuit input
      passportNumber: 123456789,    // matches ZK circuit input
      nationality: 458,             // MY (Malaysia) numeric code, matches ZK circuit input
    };

    const issueRes = await fetch(`${BASE_URL}/api/issue-passport`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mockPassport),
    });

    if (!issueRes.ok) throw new Error(`Issue request failed: ${issueRes.status}`);
    const issueData = await issueRes.json();

    // ----- 3. Print the signed Verifiable Credential -----------------------
    console.log("\n=== 3. Signed Verifiable Credential ===\n");
    console.log(JSON.stringify(issueData.verifiableCredential, null, 2));

    console.log("\n✅ All tests passed.");
  } catch (err) {
    console.error("\n❌ Test failed:", err.message);
    process.exit(1);
  }
}

main();
