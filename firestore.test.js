const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- 1. Unauthenticated Denials ---
test("Unauthenticated user cannot read or write profiles, phonemeMastery, or practiceSessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauthorized",
      currentStreakDays: 1,
      totalSessions: 0,
      overallClarityScore: 0,
      preferredStartSpeed: 0.8,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("phonemeMastery")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("practiceSessions")
      .get()
  );
});

// --- 2. LearnerProfile Create, Partial Update, Immutability, & Red Team ---
test("LearnerProfile: owner create & partial update succeed; ghost fields & immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Mirror VoxFormRepository.ensureLearnerProfile create payload (no `id` field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rivera",
      currentStreakDays: 1,
      totalSessions: 0,
      overallClarityScore: 0,
      preferredStartSpeed: 0.8,
      focusPhonemeIds: ["th_voiceless", "r_american"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Read own profile succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Listing /users root collection must be rejected
  await assertFails(aliceDb.collection("users").get());

  // Partial update on mutable fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalSessions: 3,
      overallClarityScore: 84,
      preferredStartSpeed: 0.9,
      focusPhonemeIds: ["r_american"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject shadow/ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      overallClarityScore: 90,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject out-of-range clarity score
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      overallClarityScore: 150,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

// --- 3. PhonemeMastery CRUD & Query Alignment ---
test("PhonemeMastery: owner create, partial update, list succeed; mismatched ID or cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const ref = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("phonemeMastery")
    .doc("th_voiceless");

  await assertSucceeds(
    ref.set({
      phonemeId: "th_voiceless",
      ipaSymbol: "/θ/",
      category: "Dental",
      masteryScore: 72,
      attemptCount: 1,
      recommendedSpeed: 0.8,
      recentIssueNote: "Extend tongue tip slightly further between teeth.",
      lastPracticedAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner list query succeeds (matches Kotlin observePhonemeMastery)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("phonemeMastery")
      .get()
  );

  // Partial update on mutable fields succeeds
  await assertSucceeds(
    ref.update({
      masteryScore: 86,
      attemptCount: 2,
      recommendedSpeed: 0.9,
      recentIssueNote: "Clear steady airflow.",
      lastPracticedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mismatched phonemeId field vs doc ID
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("phonemeMastery")
      .doc("r_american")
      .set({
        phonemeId: "wrong_id",
        ipaSymbol: "/ɹ/",
        category: "Rhotic",
        masteryScore: 50,
        attemptCount: 1,
        recommendedSpeed: 0.8,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject cross-user read, list, and write
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("phonemeMastery")
      .doc("th_voiceless")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("phonemeMastery")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("phonemeMastery")
      .doc("th_voiceless")
      .update({
        masteryScore: 10,
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

// --- 4. PracticeSessions Append-Only & Query Alignment ---
test("PracticeSessions: owner create, ordered list, delete succeed; updates and extra id field fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  const sessionRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("practiceSessions")
    .doc("session_001");

  await assertSucceeds(
    sessionRef.set({
      phraseId: "phrase_th_01",
      phraseText: "I think thirty-three things are worth thanking them for.",
      targetPhonemes: ["th_voiceless", "th_voiced", "r_american"],
      speedMultiplier: 0.8,
      overallScore: 82,
      phonemeAccuracy: 80,
      rhythmScore: 85,
      flowScore: 81,
      coachingSummary: "Crisp rhythm; lighten tongue pressure on /θ/.",
      nextStepRecommendation: "Try 0.90x cadence focusing on initial /θ/.",
      createdAt: created,
    })
  );

  // Ordered query matching Kotlin observePracticeSessions
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("practiceSessions")
      .orderBy("createdAt", "desc")
      .limit(20)
      .get()
  );

  // Sessions are append-only: updates must be rejected
  await assertFails(
    sessionRef.update({
      overallScore: 100,
    })
  );

  // Reject writing @DocumentId `id` key into payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("practiceSessions")
      .doc("session_002")
      .set({
        id: "session_002",
        phraseId: "phrase_th_01",
        phraseText: "Test phrase",
        targetPhonemes: ["th_voiceless"],
        speedMultiplier: 0.8,
        overallScore: 80,
        phonemeAccuracy: 80,
        rhythmScore: 80,
        flowScore: 80,
        coachingSummary: "Summary",
        nextStepRecommendation: "Next step",
        createdAt: created,
      })
  );

  // Cross-user access denied
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("practiceSessions")
      .doc("session_001")
      .get()
  );

  // Owner delete succeeds
  await assertSucceeds(sessionRef.delete());
});
