# Vivo Communication Sharing compatibility roadmap

## Confirmed baseline
- S60 has active LTE data, voice and SMS service.
- VMDri and VDFS are active on S60.
- Decompiled S60 source is in `source.zip` at the repository root.
- `patches/VivoCommShareProtocol.kt` contains preliminary, **unverified** protocol representations.
- The DataNetShare APK is **not** a native Vivo Communication Sharing client.

## Next implementation gates
1. Extract concrete discovery entry points, advertised service identifiers, account checks and transport negotiation from `source.zip`. Save evidence with source file paths and line numbers.
2. Implement a passive discovery monitor on Redmi/Y700 with explicit Bluetooth/Wi-Fi permissions. Display discovered device identifiers and raw capability metadata; do not advertise a false device identity yet.
3. Implement a transport-state machine: IDLE, DISCOVERING, FOUND, CONNECTING, AUTHENTICATING, READY, ERROR. Log failures without logging account tokens or private device IDs.
4. Implement a user-space handshake only after the actual framing and authorization rules are confirmed from source.
5. Validate on S60 + Redmi; test data forwarding only after an authenticated session. Calls and SMS are separate later milestones.

## Limitations
- Android public APIs cannot grant Vivo privileged signatures or permissions.
- A successful build does not demonstrate S60 interoperability.
- No rooting or ROM flashing is required for the passive discovery phase.
