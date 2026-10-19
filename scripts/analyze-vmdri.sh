#!/usr/bin/env bash
set -euo pipefail

SRC="${1:-analysis/vmdri/source}"
OUT="${2:-analysis/vmdri/results}"
mkdir -p "$OUT"

grep -RniE "BluetoothGatt|BluetoothGattServer|ScanFilter|ParcelUuid|UUID|serviceUuid|characteristic|advertis|notify|writeCharacteristic|readCharacteristic|send.*Data|recv.*Data|ctrlChannel" "$SRC" > "$OUT/ble-control.txt" || true

grep -RniE "vdfs_net_share|vdfs_comm_share|serviceId.*vdfs|businessState|sendBusinessState|notifyCommStateToRemote|StartCommShare|StopCommShare" "$SRC" > "$OUT/net-control.txt" || true

grep -RniE "CommDevInfo|DeviceType|Capality|CapabilityType|PAD_SUPPORT|openId|account|login|bind|supported|deviceType|connTypeBitmask" "$SRC" > "$OUT/capability-control.txt" || true

grep -RniE "WifiP2p|P2pInfoParcel|getLocalP2pInfo|getRemoteP2pInfo|initDataChannel|IDataChannel|sendData|ready" "$SRC" > "$OUT/p2p-data-control.txt" || true

echo "Results written to $OUT"
