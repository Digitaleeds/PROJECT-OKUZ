const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function buildValidPagePayload(userId = ALICE_UID, pageNumber = 1) {
  const ts = new Date(Date.now() - 1000);
  return {
    userId,
    pageNumber,
    completed: false,
    ruleDorsal: true,
    ruleHood: true,
    ruleTree: false,
    ruleObject: false,
    mandate: "Unknown / TBD",
    cameraDistance: "Wide Env.",
    separationForce: "Nature",
    storyline: "The cold rain lashed against the pines...",
    illustration: "Low dorsal tracking shot behind the hooded 10-year-old.",
    kidPosture: "Crouched, hood pulled low against the wind",
    dogAction: "Pointy ears pinned back, shivering under a root",
    distanceApart: "12 meters across flooded ravine",
    trigger: "Sudden thunderclap and rising creek",
    response: "Slows breathing, steadies lantern hand",
    turningPoint: "Spots firm foothold along the mossy ridge",
    terrain: "Slick shale embankment in torrential downpour",
    obstacle: "Swollen mountain stream carrying fallen branches",
    sketchStrokes: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("1. Unauthenticated user cannot read or create storyboard pages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("pages").doc("page_01").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pages")
      .doc("page_01")
      .set(buildValidPagePayload(ALICE_UID, 1))
  );
});

test("2. Authenticated owner can create and read a valid storyboard page", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("pages")
    .doc("page_01");
  await assertSucceeds(docRef.set(buildValidPagePayload(ALICE_UID, 1)));
  await assertSucceeds(docRef.get());
});

test("3. Authenticated user cannot read or write another user's storyboard page", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pages")
      .doc("page_01")
      .set(buildValidPagePayload(ALICE_UID, 1))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("pages").doc("page_01").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pages")
      .doc("page_01")
      .update({ storyline: "Tampered", updatedAt: new Date(Date.now() - 500) })
  );
});

test("4. Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("pages")
    .doc("page_01");
  await assertSucceeds(docRef.set(buildValidPagePayload(ALICE_UID, 1)));

  await assertFails(
    docRef.update({
      storyline: "Updated text",
      isVerifiedGhostField: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("5. Invalid pageNumber outside 1..25 or wrong type is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("pages")
    .doc("page_99");
  await assertFails(docRef.set(buildValidPagePayload(ALICE_UID, 26)));
  await assertFails(docRef.set(buildValidPagePayload(ALICE_UID, 0)));
});

test("6. Authenticated owner can query own pages with matching userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pages")
      .doc("page_01")
      .set(buildValidPagePayload(ALICE_UID, 1))
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pages")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});
