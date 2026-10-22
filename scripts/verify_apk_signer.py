"""Check an APK's v2 signing certificate against the private PKCS#12 key.

This reads public certificates only; it does not execute the APK or print secrets.
Requires the Python cryptography package.
"""

import hashlib
import struct
import sys
from pathlib import Path

from cryptography.hazmat.primitives.serialization import Encoding, pkcs12


def sized(blob: bytes, offset: int) -> tuple[bytes, int]:
    length = struct.unpack_from("<I", blob, offset)[0]
    start = offset + 4
    end = start + length
    if end > len(blob):
        raise ValueError("Invalid signing block length")
    return blob[start:end], end


def apk_certificate(apk: Path) -> bytes:
    data = apk.read_bytes()
    eocd = data.rfind(b"PK\x05\x06")
    if eocd < 0:
        raise ValueError("APK ZIP directory missing")
    directory = struct.unpack_from("<I", data, eocd + 16)[0]
    footer = data[directory - 24:directory]
    if footer[8:] != b"APK Sig Block 42":
        raise ValueError("APK v2 signing block missing")
    block_size = struct.unpack_from("<Q", footer, 0)[0]
    start = directory - block_size - 8
    if start < 0 or struct.unpack_from("<Q", data, start)[0] != block_size:
        raise ValueError("APK signing block corrupt")
    position = start + 8
    while position < directory - 24:
        pair_size = struct.unpack_from("<Q", data, position)[0]
        pair_id = struct.unpack_from("<I", data, position + 8)[0]
        value = data[position + 12:position + 8 + pair_size]
        if pair_id == 0x7109871A:  # APK Signature Scheme v2
            signers, _ = sized(value, 0)
            signer, _ = sized(signers, 0)
            signed_data, _ = sized(signer, 0)
            _, cursor = sized(signed_data, 0)  # Digests
            certificates, _ = sized(signed_data, cursor)
            certificate, _ = sized(certificates, 0)
            return certificate
        position += 8 + pair_size
    raise ValueError("APK Signature Scheme v2 entry missing")


def main() -> None:
    if len(sys.argv) != 4:
        raise SystemExit("Usage: python scripts/verify_apk_signer.py APK KEYSTORE PASSWORD_FILE")
    apk, store, password_file = map(Path, sys.argv[1:])
    _, certificate, _ = pkcs12.load_key_and_certificates(
        store.read_bytes(), password_file.read_bytes()
    )
    if certificate is None:
        raise ValueError("Signing certificate missing from keystore")
    actual = hashlib.sha256(apk_certificate(apk)).digest()
    expected = hashlib.sha256(certificate.public_bytes(Encoding.DER)).digest()
    if actual != expected:
        raise SystemExit("APK signer does not match the persistent key")
    print("APK signer matches the persistent key")


if __name__ == "__main__":
    main()
