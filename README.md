# cloud-itonami-isco-4412

Open Occupation Blueprint for **ISCO-08 4412**: Mail Carriers and Sorting Clerks.

This repository designs a forkable OSS business for an independent mail sorting and delivery practice: a sorting and local-delivery robot handles physical mail routing under a governor-gated actor, so the practice keeps its own delivery records instead of renting a closed logistics SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a sorting and local-delivery robot performs mail sorting by route and last-mile delivery drop-off under an actor that proposes
actions and an independent **Mail Services Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
registered/certified mail release) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
mail intake + route plan + delivery requirements
        |
        v
Mail Advisor -> Mail Services Governor -> sort/deliver, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `4412`). Required capabilities:

- :robotics
- :identity
- :telemetry
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
