#!/usr/bin/env python3
"""Writes a small valid one-page PDF with a few lines of text (demo files, no dependency).
Usage: make-pdf.py OUTPUT "Title" "line 1" "line 2" ..."""
import sys

out, title, *lines = sys.argv[1:]

def esc(text: str) -> str:
    return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

# WinAnsi (cp1252) text: French accents, — and … are supported by the standard Helvetica font
ops = ["BT", "/F1 20 Tf", "72 760 Td", f"({esc(title)}) Tj", "/F1 12 Tf", "0 -34 Td"]
for line in lines:
    ops += [f"({esc(line)}) Tj", "0 -18 Td"]
ops.append("ET")
stream = "\n".join(ops).encode("cp1252", errors="replace")

objects = [
    b"<< /Type /Catalog /Pages 2 0 R >>",
    b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
    b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R "
    b"/Resources << /Font << /F1 5 0 R >> >> >>",
    b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream",
    b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
]

pdf = bytearray(b"%PDF-1.4\n")
offsets = []
for number, body in enumerate(objects, start=1):
    offsets.append(len(pdf))
    pdf += f"{number} 0 obj\n".encode() + body + b"\nendobj\n"
xref = len(pdf)
pdf += f"xref\n0 {len(objects) + 1}\n0000000000 65535 f \n".encode()
for offset in offsets:
    pdf += f"{offset:010d} 00000 n \n".encode()
pdf += f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode()

with open(out, "wb") as f:
    f.write(pdf)
