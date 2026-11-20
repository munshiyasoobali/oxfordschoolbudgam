const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_oxford_1";
const PARENT_A_UID = "parent_a_101";
const PARENT_B_UID = "parent_b_102";

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

async function seedManagementAndStudents() {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("school_config").doc("management").set({
      adminUid: ADMIN_UID,
      maskedEmail: "p***@oxfordbudgam.edu.in",
      adminName: "Principal Tariq Ahmad",
      claimedAt: now,
    });
    await db.collection("school_config").doc("payment_account").set({
      accountHolderName: "OXFORD SCHOOL EDUCATION BUDGAM",
      upiId: "oxfordbudgam@jkb",
      bankName: "J&K Bank Budgam",
      accountNumber: "0123040100004567",
      ifscCode: "JAKA0BUDGAM",
      updatedByUid: ADMIN_UID,
      updatedAt: now,
    });
    await db.collection("students").doc("OXB-101").set({
      name: "Aarav Bhat",
      grade: "Class 10-A",
      rollNo: "OXB-101",
      parentName: "Sanjay Bhat",
      contactNumber: "+91 94190 11201",
      admissionYear: "2022",
      parentUid: PARENT_A_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("students").doc("OXB-102").set({
      name: "Zoya Mir",
      grade: "Class 10-A",
      rollNo: "OXB-102",
      parentName: "Farooq Ahmad Mir",
      contactNumber: "+91 94190 11202",
      admissionYear: "2022",
      parentUid: PARENT_B_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(PARENT_A_UID).set({
      userId: PARENT_A_UID,
      studentId: "OXB-101",
      rollNo: "OXB-101",
      displayName: "Sanjay Bhat",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("fees").doc("fee_101_nov").set({
      studentId: "OXB-101",
      studentName: "Aarav Bhat",
      rollNo: "OXB-101",
      grade: "Class 10-A",
      parentUid: PARENT_A_UID,
      monthLabel: "November 2026",
      tuitionFee: 2500,
      transportFee: 800,
      examFee: 0,
      totalAmount: 3300,
      status: "PENDING",
      receiptNumber: "",
      paidDate: "",
      paymentMethod: "School Counter",
      transactionRef: "",
      payerAccountNote: "",
      createdAt: now,
      updatedAt: now,
    });
  });
}

test("Unauthenticated user: denied all reads and writes", async () => {
  await seedManagementAndStudents();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("school_config").doc("management").get());
  await assertFails(unauthDb.collection("students").doc("OXB-101").get());
  await assertFails(unauthDb.collection("fees").where("studentId", "==", "OXB-101").get());
});

test("Single Management Lock: first admin claims, second user cannot claim or overwrite", async () => {
  const now = new Date(Date.now() - 2000);
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(
    adminDb.collection("school_config").doc("management").set({
      adminUid: ADMIN_UID,
      maskedEmail: "p***@oxfordbudgam.edu.in",
      adminName: "Principal",
      claimedAt: now,
    })
  );

  const intruderDb = testEnv.authenticatedContext(PARENT_A_UID).firestore();
  await assertFails(
    intruderDb.collection("school_config").doc("management").set({
      adminUid: PARENT_A_UID,
      maskedEmail: "h***@gmail.com",
      adminName: "Intruder",
      claimedAt: now,
    })
  );
  await assertFails(
    intruderDb.collection("school_config").doc("management").update({
      adminUid: PARENT_A_UID,
    })
  );
});

test("Management can configure real payment account; non-admin cannot", async () => {
  await seedManagementAndStudents();
  const now = new Date(Date.now() - 1000);
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const parentDb = testEnv.authenticatedContext(PARENT_A_UID).firestore();

  await assertSucceeds(
    adminDb.collection("school_config").doc("payment_account").set({
      accountHolderName: "OXFORD SCHOOL EDUCATION BUDGAM",
      upiId: "oxford@jkb",
      bankName: "J&K Bank",
      accountNumber: "9988776655",
      ifscCode: "JAKA0BUDGAM",
      updatedByUid: ADMIN_UID,
      updatedAt: now,
    })
  );

  await assertFails(
    parentDb.collection("school_config").doc("payment_account").update({
      upiId: "attacker@upi",
      updatedByUid: PARENT_A_UID,
      updatedAt: now,
    })
  );
});

test("Cross-student isolation: Parent B cannot read or list Parent A student or fees", async () => {
  await seedManagementAndStudents();
  const parentBDb = testEnv.authenticatedContext(PARENT_B_UID).firestore();

  await assertFails(parentBDb.collection("students").doc("OXB-101").get());
  await assertFails(parentBDb.collection("students").get());
  await assertFails(parentBDb.collection("fees").where("studentId", "==", "OXB-101").get());
  await assertSucceeds(parentBDb.collection("students").where("parentUid", "==", PARENT_B_UID).get());
});

test("Parent real UPI/UTR fee payment succeeds once and locks PAID state", async () => {
  await seedManagementAndStudents();
  const now = new Date(Date.now() - 1000);
  const parentADb = testEnv.authenticatedContext(PARENT_A_UID).firestore();

  // Matches Kotlin query
  await assertSucceeds(
    parentADb.collection("fees").where("studentId", "==", "OXB-101").get()
  );

  // Tampering totalAmount must fail
  await assertFails(
    parentADb.collection("fees").doc("fee_101_nov").update({
      status: "PAID",
      totalAmount: 1,
      receiptNumber: "OXB-RCP-1",
      paidDate: "20 Nov 2026",
      paymentMethod: "UPI App",
      transactionRef: "UPI12345678",
      payerAccountNote: "parent@oksbi",
      updatedAt: now,
    })
  );

  // Valid payment succeeds
  await assertSucceeds(
    parentADb.collection("fees").doc("fee_101_nov").update({
      status: "PAID",
      receiptNumber: "OXB-RCP-2611-1234",
      paidDate: "20 Nov 2026",
      paymentMethod: "UPI App (oxfordbudgam@jkb)",
      transactionRef: "UPI99887766",
      payerAccountNote: "sanjay@oksbi",
      updatedAt: now,
    })
  );

  // Subsequent parent mutation on PAID invoice is blocked (terminal lock)
  await assertFails(
    parentADb.collection("fees").doc("fee_101_nov").update({
      status: "PAID",
      transactionRef: "TAMPERED999",
      updatedAt: now,
    })
  );
});

test("Parent can probe non-existent roll no and claim unclaimed seeded student", async () => {
  await seedManagementAndStudents();
  const now = new Date(Date.now() - 1000);
  const newParentUid = "parent_c_103";
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("students").doc("OXB-103").set({
      name: "Hamza Lone",
      grade: "Class 9-B",
      rollNo: "OXB-103",
      parentName: "Shabir Ahmad Lone",
      contactNumber: "+91 94190 11203",
      admissionYear: "2023",
      parentUid: "",
      createdAt: now,
      updatedAt: now,
    });
  });

  const parentCDb = testEnv.authenticatedContext(newParentUid).firestore();
  await assertSucceeds(parentCDb.collection("students").doc("OXB-999").get());
  await assertSucceeds(parentCDb.collection("students").doc("OXB-103").get());
  await assertSucceeds(
    parentCDb.collection("students").doc("OXB-103").update({
      parentUid: newParentUid,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    parentCDb.collection("users").doc(newParentUid).set({
      userId: newParentUid,
      studentId: "OXB-103",
      rollNo: "OXB-103",
      displayName: "Shabir Ahmad Lone",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Linked parent can read seeded attendance even if attendance.parentUid is ""
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("attendance").doc("OXB-103").set({
      studentId: "OXB-103",
      parentUid: "",
      days: { "1": "P", "2": "A" },
      updatedAt: now,
    });
  });
  await assertSucceeds(parentCDb.collection("attendance").doc("OXB-103").get());
  await assertFails(parentCDb.collection("attendance").doc("OXB-101").get());
  await assertFails(
    parentCDb.collection("attendance").doc("OXB-103").update({
      days: { "1": "P", "2": "P" },
      updatedAt: now,
    })
  );
});

test("Diary entries & notices: Admin creates, signed-in Parent reads and toggles homework completion", async () => {
  await seedManagementAndStudents();
  const now = new Date(Date.now() - 1000);
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const parentADb = testEnv.authenticatedContext(PARENT_A_UID).firestore();

  await assertSucceeds(
    adminDb.collection("diary").doc("diary_1").set({
      title: "Winter Notice",
      subject: "School Notice",
      description: "School timings updated.",
      targetClass: "All Classes",
      dateLabel: "20 Nov 2026",
      teacherRemarks: "Mandatory",
      entryType: "NOTICE",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    parentADb.collection("diary").doc("diary_2").set({
      title: "Fake",
      subject: "Math",
      description: "Fake",
      targetClass: "All Classes",
      dateLabel: "20 Nov 2026",
      teacherRemarks: "",
      entryType: "HOMEWORK",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(parentADb.collection("diary").get());
  await assertSucceeds(
    parentADb.collection("users").doc(PARENT_A_UID).collection("homework_completions").doc("diary_1").set({
      diaryId: "diary_1",
      completed: true,
      updatedAt: now,
    })
  );
});
