pragma circom 2.1.5;
include "node_modules/circomlib/circuits/poseidon.circom";
include "node_modules/circomlib/circuits/comparators.circom";

template ZKPassport() {
    signal input dateOfBirth;     // Format: YYYYMMDD (e.g., 20040512)
    signal input passportNumber;  // Integer representation
    signal input nationality;     // Integer representation
    signal input currentAgeThreshold; // Format: YYYYMMDD
    
    signal output passportHash;
    signal output isEligible;

    component ageCheck = GreaterEqThan(32);
    ageCheck.in[0] <== currentAgeThreshold;
    ageCheck.in[1] <== dateOfBirth;
    ageCheck.out === 1;
    isEligible <== ageCheck.out;

    component hasher = Poseidon(3);
    hasher.inputs[0] <== dateOfBirth;
    hasher.inputs[1] <== passportNumber;
    hasher.inputs[2] <== nationality;
    passportHash <== hasher.out;
}
component main {public [currentAgeThreshold]} = ZKPassport();
