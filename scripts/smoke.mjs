const base = process.env.VEHICLEIQ_BASE_URL ?? "http://localhost:4000";
const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

async function request(path, { expected = 200, ...options } = {}) {
  const response = await fetch(`${base}${path}`, options);
  const body = await response.json().catch(() => null);
  if (response.status !== expected) {
    throw new Error(`${options.method ?? "GET"} ${path}: expected ${expected}, received ${response.status} ${JSON.stringify(body)}`);
  }
  return body;
}

async function signIn(role) {
  const session = await request("/api/auth/demo", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ role })
  });
  return { Authorization: `Bearer ${session.accessToken}` };
}

async function poll(path, headers, isComplete) {
  for (let attempt = 0; attempt < 40; attempt += 1) {
    const result = await request(path, { headers });
    if (isComplete(result)) return result;
    await wait(250);
  }
  throw new Error(`Timed out waiting for ${path}`);
}

async function run() {
  const dealer = await signIn("DEALER");
  const dealerVehicles = await request("/api/v1/vehicles", { headers: dealer });
  if (dealerVehicles.some((vehicle) => vehicle.registration === "GD20HTR")) {
    throw new Error("Tenant isolation failed: dealer can see Northstar Motors data.");
  }

  const admin = await signIn("ADMIN");
  const allVehicles = await request("/api/v1/vehicles", { headers: admin });
  const otherTenantVehicle = allVehicles.find((vehicle) => vehicle.registration === "GD20HTR");
  if (otherTenantVehicle?.latestAnalysisId) {
    await request(`/api/v1/analyses/${otherTenantVehicle.latestAnalysisId}`, { headers: dealer, expected: 404 });
  }

  const registration = `QA${Date.now().toString().slice(-6)}`;
  const vehicle = await request("/api/v1/vehicles", {
    method: "POST",
    headers: { ...dealer, "content-type": "application/json" },
    body: JSON.stringify({
      registration, make: "Toyota", model: "Corolla Design", year: 2022, mileage: 23000,
      fuelType: "Hybrid", transmission: "Automatic", purchasePrice: 14500, buyerFees: 300,
      transport: 150, repairEstimate: 250, comparablePrices: [19500, 20100, 20800],
      conditionNotes: "Local smoke-test vehicle."
    }),
    expected: 201
  });
  const queued = await request(`/api/v1/vehicles/${vehicle.id}/analyses`, {
    method: "POST", headers: { ...dealer, "content-type": "application/json" }, body: "{}", expected: 202
  });
  const report = await poll(`/api/v1/analyses/${queued.id}`, dealer, (item) => item.status === "COMPLETED");
  if (report.result.valuation.midpoint !== 20100) throw new Error("Valuation did not use the supplied comparable median.");
  if (report.result.verification.status !== "UNAVAILABLE") throw new Error("Missing provider data was not labelled unavailable.");

  const form = new FormData();
  form.append("file", new Blob(["%PDF-1.4 VehicleIQ local evidence"], { type: "application/pdf" }), "smoke-evidence.pdf");
  const evidence = await request(`/api/v1/vehicles/${vehicle.id}/evidence`, { method: "POST", headers: dealer, body: form });
  const ticket = await request("/api/v1/support/tickets", {
    method: "POST", headers: { ...dealer, "content-type": "application/json" },
    body: JSON.stringify({ subject: "Local smoke check", message: "Support workflow acceptance check." }), expected: 201
  });
  const operations = await request("/api/v1/admin/dashboard", { headers: admin });
  const audit = await request("/api/v1/admin/audit", { headers: admin });
  if (!audit.some((event) => event.resourceId === vehicle.id || event.resourceId === ticket.id)) {
    throw new Error("Expected customer actions were not found in the admin audit view.");
  }

  const badKey = await request("/partner/v1/vehicle-assessments", {
    method: "POST", headers: { "x-api-key": "invalid" }, expected: 401
  });
  if (!badKey.error) throw new Error("Invalid partner credentials were not rejected clearly.");
  const partnerHeaders = { "content-type": "application/json", "x-api-key": "viq_demo_local_only_7f0c3e91" };
  const partnerJob = await request("/partner/v1/vehicle-assessments", {
    method: "POST", headers: partnerHeaders,
    body: JSON.stringify({ registration: "APIQA26", make: "Honda", model: "Civic", year: 2021, purchasePrice: 16000, comparablePrices: [21000, 22000, 23000] }),
    expected: 202
  });
  const partnerResult = await poll(`/partner/v1/vehicle-assessments/${partnerJob.id}`, { "x-api-key": partnerHeaders["x-api-key"] }, (item) => item.status === "COMPLETED");

  console.log(JSON.stringify({
    result: "passed",
    checks: ["tenant isolation", "asynchronous analysis", "valuation median", "unavailable-source labeling", "evidence upload", "support ticket", "admin dashboard and audit", "invalid API-key rejection", "partner API completion"],
    analysisStatus: report.status,
    valuationMidpoint: report.result.valuation.midpoint,
    evidence: evidence.filename,
    openTicket: ticket.status,
    platformVehicles: operations.vehicles,
    partnerStatus: partnerResult.status
  }, null, 2));
}

run().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
