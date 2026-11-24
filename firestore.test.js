const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PATO_UID = "pato_owner_001";
const OTHER_UID = "other_user_999";

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

function validClientPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    fullName: "Carlos Mendoza",
    phone: "3001234567",
    address: "Oficina 302",
    paymentModality: "SEMANAL",
    agreedPrice: 15000,
    notes: "Sin cebolla",
    status: "ACTIVO",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validLunchPayload(uid, clientId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    clientId,
    clientNameSnapshot: "Carlos Mendoza",
    dateEpochDay: 20780,
    quantity: 2,
    unitPrice: 15000,
    totalAmount: 30000,
    note: "Con jugo extra",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validPaymentPayload(uid, clientId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    clientId,
    clientNameSnapshot: "Carlos Mendoza",
    dateEpochDay: 20780,
    amount: 15000,
    method: "NEQUI",
    paymentType: "ABONO",
    note: "Abono parcial",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

// --- UNAUTHENTICATED DENIALS ---

test("Unauthenticated user: denied read and write across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${PATO_UID}/clients`).get());
  await assertFails(unauthDb.collection(`users/${PATO_UID}/lunch_entries`).get());
  await assertFails(unauthDb.collection(`users/${PATO_UID}/payments`).get());
  await assertFails(
    unauthDb
      .collection(`users/${PATO_UID}/clients`)
      .doc("client_1")
      .set(validClientPayload(PATO_UID))
  );
});

// --- CLIENTS CRUD & VALIDATION ---

test("Owner: can create, list, get, update, toggle status, and delete client", async () => {
  const patoDb = testEnv.authenticatedContext(PATO_UID).firestore();
  const clientRef = patoDb.collection(`users/${PATO_UID}/clients`).doc("client_1");

  // Create without optional fields (mirrors Kotlin .filterValues { it != null })
  const minimal = validClientPayload(PATO_UID);
  delete minimal.phone;
  delete minimal.address;
  delete minimal.notes;
  await assertSucceeds(clientRef.set(minimal));

  // List subcollection (mirrors Kotlin .snapshots())
  await assertSucceeds(patoDb.collection(`users/${PATO_UID}/clients`).get());
  await assertSucceeds(clientRef.get());

  // Partial update: status toggle
  await assertSucceeds(
    clientRef.update({
      status: "INACTIVO",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Full profile edit preserving immutable createdAt/userId
  await assertSucceeds(
    clientRef.update({
      fullName: "Carlos M.",
      phone: "3105559999",
      address: "Calle 10 # 5-20",
      paymentModality: "QUINCENAL",
      agreedPrice: 16000,
      notes: "Sopa aparte",
      status: "ACTIVO",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field shadow update
  await assertFails(
    clientRef.update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    clientRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject @DocumentId "id" inside payload on create
  await assertFails(
    patoDb
      .collection(`users/${PATO_UID}/clients`)
      .doc("client_2")
      .set(validClientPayload(PATO_UID, { id: "client_2" }))
  );

  // Reject future timestamp
  await assertFails(
    patoDb
      .collection(`users/${PATO_UID}/clients`)
      .doc("client_3")
      .set(validClientPayload(PATO_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Reject invalid modality or negative price
  await assertFails(
    patoDb
      .collection(`users/${PATO_UID}/clients`)
      .doc("client_4")
      .set(validClientPayload(PATO_UID, { paymentModality: "ANUAL" }))
  );
  await assertFails(
    patoDb
      .collection(`users/${PATO_UID}/clients`)
      .doc("client_5")
      .set(validClientPayload(PATO_UID, { agreedPrice: 0 }))
  );

  // Delete succeeds for owner
  await assertSucceeds(clientRef.delete());
});

// --- LUNCH ENTRIES CRUD & RELATIONAL INTEGRITY ---

test("Owner: lunch entries require existing client, valid math, and immutable clientId", async () => {
  const patoDb = testEnv.authenticatedContext(PATO_UID).firestore();
  const clientRef = patoDb.collection(`users/${PATO_UID}/clients`).doc("client_1");
  await assertSucceeds(clientRef.set(validClientPayload(PATO_UID)));

  // Reject orphaned lunch entry for non-existent client
  await assertFails(
    patoDb
      .collection(`users/${PATO_UID}/lunch_entries`)
      .doc("entry_orphan")
      .set(validLunchPayload(PATO_UID, "missing_client"))
  );

  // Reject tampered totalAmount != quantity * unitPrice
  await assertFails(
    patoDb
      .collection(`users/${PATO_UID}/lunch_entries`)
      .doc("entry_bad_math")
      .set(validLunchPayload(PATO_UID, "client_1", { quantity: 2, unitPrice: 15000, totalAmount: 10000 }))
  );

  // Valid create
  const entryRef = patoDb.collection(`users/${PATO_UID}/lunch_entries`).doc("entry_1");
  await assertSucceeds(entryRef.set(validLunchPayload(PATO_UID, "client_1")));

  // List subcollection
  await assertSucceeds(patoDb.collection(`users/${PATO_UID}/lunch_entries`).get());

  // Valid edit (correct quantity & totalAmount)
  await assertSucceeds(
    entryRef.update({
      quantity: 3,
      unitPrice: 15000,
      totalAmount: 45000,
      note: "Corregido a 3 almuerzos",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject mutating clientId on update
  await assertFails(
    entryRef.update({
      clientId: "client_other",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertSucceeds(entryRef.delete());
});

// --- PAYMENTS CRUD & VALIDATION ---

test("Owner: payments require existing client, valid method/type, and allow partial edits & delete", async () => {
  const patoDb = testEnv.authenticatedContext(PATO_UID).firestore();
  await assertSucceeds(
    patoDb.collection(`users/${PATO_UID}/clients`).doc("client_1").set(validClientPayload(PATO_UID))
  );

  const payRef = patoDb.collection(`users/${PATO_UID}/payments`).doc("pay_1");
  await assertSucceeds(payRef.set(validPaymentPayload(PATO_UID, "client_1")));
  await assertSucceeds(patoDb.collection(`users/${PATO_UID}/payments`).get());

  // Reject invalid payment method
  await assertFails(
    patoDb
      .collection(`users/${PATO_UID}/payments`)
      .doc("pay_bad_method")
      .set(validPaymentPayload(PATO_UID, "client_1", { method: "CRYPTO" }))
  );

  // Reject orphaned payment
  await assertFails(
    patoDb
      .collection(`users/${PATO_UID}/payments`)
      .doc("pay_orphan")
      .set(validPaymentPayload(PATO_UID, "ghost_client"))
  );

  // Valid edit
  await assertSucceeds(
    payRef.update({
      amount: 30000,
      method: "EFECTIVO",
      paymentType: "TOTAL",
      note: "Pago completo",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(payRef.delete());
});

// --- CROSS-USER ISOLATION ---

test("Cross-user isolation: another authenticated user cannot read or modify Pato's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection(`users/${PATO_UID}/clients`).doc("client_1").set(validClientPayload(PATO_UID));
    await db.collection(`users/${PATO_UID}/lunch_entries`).doc("entry_1").set(validLunchPayload(PATO_UID, "client_1"));
    await db.collection(`users/${PATO_UID}/payments`).doc("pay_1").set(validPaymentPayload(PATO_UID, "client_1"));
  });

  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  // Read denials (PII & financial isolation)
  await assertFails(otherDb.collection(`users/${PATO_UID}/clients`).get());
  await assertFails(otherDb.collection(`users/${PATO_UID}/clients`).doc("client_1").get());
  await assertFails(otherDb.collection(`users/${PATO_UID}/lunch_entries`).get());
  await assertFails(otherDb.collection(`users/${PATO_UID}/payments`).get());

  // Write/spoofing denials
  await assertFails(
    otherDb.collection(`users/${PATO_UID}/clients`).doc("client_1").update({ fullName: "Hacked" })
  );
  await assertFails(
    otherDb.collection(`users/${PATO_UID}/clients`).doc("spoof").set(validClientPayload(OTHER_UID))
  );
  await assertFails(
    otherDb.collection(`users/${PATO_UID}/payments`).doc("pay_1").delete()
  );
});
