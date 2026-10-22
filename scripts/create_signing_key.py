"""Create a persistent Android PKCS#12 signing key outside this repository.

Requires the Python cryptography package. Run once with an output directory.
Never commit, print, or publish the generated files.
"""

import base64
import secrets
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python scripts/create_signing_key.py OUTPUT_DIRECTORY")
    target = Path(sys.argv[1]).expanduser().resolve()
    target.mkdir(parents=True, exist_ok=True)
    keystore = target / "buddy-signing.p12"
    password_file = target / "buddy-signing-password.txt"
    encoded_file = target / "buddy-signing-base64.txt"
    if any(path.exists() for path in (keystore, password_file, encoded_file)):
        raise SystemExit("Signing files already exist; refusing to replace them.")

    password = secrets.token_urlsafe(32).encode("ascii")
    key = rsa.generate_private_key(public_exponent=65537, key_size=3072)
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "Adam's Buddy Android")])
    now = datetime.now(timezone.utc)
    certificate = (
        x509.CertificateBuilder()
        .subject_name(name)
        .issuer_name(name)
        .public_key(key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now - timedelta(days=1))
        .not_valid_after(now + timedelta(days=3650))
        .sign(key, hashes.SHA256())
    )
    bundle = pkcs12.serialize_key_and_certificates(
        b"buddy", key, certificate, None, serialization.BestAvailableEncryption(password)
    )
    keystore.write_bytes(bundle)
    password_file.write_bytes(password)
    encoded_file.write_bytes(base64.b64encode(bundle))
    print(f"Signing files created in {target}")


if __name__ == "__main__":
    main()
