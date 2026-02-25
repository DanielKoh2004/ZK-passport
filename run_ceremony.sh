#!/bin/bash
set -e

echo "============================================"
echo "  ZK Passport - Groth16 Ceremony & Testing"
echo "============================================"

# Step 1: Compile the circuit
echo ""
echo "[Step 1/6] Compiling circuit..."
circom passport.circom --r1cs --wasm --sym
echo "Circuit compiled successfully."

# Step 2: Powers of Tau trusted setup (bn128, size 12)
echo ""
echo "[Step 2/6] Running Powers of Tau trusted setup..."
npx snarkjs powersoftau new bn128 12 pot12_0000.ptau -v
npx snarkjs powersoftau contribute pot12_0000.ptau pot12_0001.ptau --name="First contribution" -v -e="random entropy for contribution"
npx snarkjs powersoftau prepare phase2 pot12_0001.ptau pot12_final.ptau -v
echo "Powers of Tau setup complete."

# Step 3: Generate zkey and export verification key
echo ""
echo "[Step 3/6] Generating zkey and verification key..."
npx snarkjs groth16 setup passport.r1cs pot12_final.ptau passport_0000.zkey
npx snarkjs zkey contribute passport_0000.zkey passport_final.zkey --name="Circuit contribution" -v -e="more random entropy"
npx snarkjs zkey export verificationkey passport_final.zkey verification_key.json
echo "Keys generated successfully."

# Step 4: Calculate witness
echo ""
echo "[Step 4/6] Calculating witness..."
node passport_js/generate_witness.js passport_js/passport.wasm input.json witness.wtns
echo "Witness calculated successfully."

# Step 5: Generate the proof
echo ""
echo "[Step 5/6] Generating zk-SNARK proof..."
npx snarkjs groth16 prove passport_final.zkey witness.wtns proof.json public.json
echo "Proof generated successfully."

# Step 6: Verify the proof
echo ""
echo "[Step 6/6] Verifying proof..."
npx snarkjs groth16 verify verification_key.json public.json proof.json
echo ""
echo "============================================"
echo "  Ceremony & Testing Complete!"
echo "============================================"
