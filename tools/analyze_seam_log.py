#!/usr/bin/env python3
"""Summarize bounded AP-DIAG telemetry. Standard library only; no network or writes."""
import argparse
import collections
import json
import re


def analyze(lines):
    sessions = set()
    totals = {side: collections.Counter() for side in ("client", "server")}
    timings = {side: {} for side in totals}
    windows = collections.Counter()
    important = collections.deque(maxlen=30)
    max_camera_speed = 0.0
    for line in lines:
        match = re.search(r"\[AP-DIAG\] session=(\S+) side=(client|server) (.*)", line)
        if not match:
            continue
        session, side, message = match.groups()
        sessions.add(session)
        if message.startswith("event="):
            name = message.split()[0][6:]
            if name == "crossing" or name.startswith(("transfer.", "diagnostic.", "dh.reset")) or "renderer_reset" in name or name == "mesh.clear":
                important.append({"side": side, "session": session, "event": message.rstrip()})
            continue
        summary = re.search(r"summary=(\d+) epochMs=(\d+) windowMs=(\d+) (.*?) counts=\{(.*?)\} timings=\{(.*?)\}", message)
        if not summary:
            continue
        windows[side] += 1
        state, counts, times = summary.group(4, 5, 6)
        for key, value in re.findall(r"([\w.]+)=(-?\d+)", counts):
            totals[side][key] += int(value)
        for key, count, average, maximum in re.findall(r"([\w.]+)=n=(\d+) avgMs=([\d.]+) maxMs=([\d.]+)", times):
            row = timings[side].setdefault(key, {"samples": 0, "weighted_ms": 0.0, "peak_ms": 0.0})
            row["samples"] += int(count)
            row["weighted_ms"] += int(count) * float(average)
            row["peak_ms"] = max(row["peak_ms"], float(maximum))
        speed = re.search(r"maxCameraSpeed=([\d.Ee+-]+)", state)
        if speed:
            max_camera_speed = max(max_camera_speed, float(speed.group(1)))
    for side in timings:
        for row in timings[side].values():
            row["mean_ms"] = round(row.pop("weighted_ms") / row["samples"], 3)
    return {"sessions": sorted(sessions), "summary_windows": dict(windows),
            "counts": {s: dict(sorted(c.items())) for s, c in totals.items()}, "timings": timings,
            "max_cruising_camera_speed": max_camera_speed, "recent_markers": list(important)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", help="Path to Minecraft latest.log")
    args = parser.parse_args()
    with open(args.log, encoding="utf-8", errors="replace") as file:
        result = analyze(file)
    if not result["sessions"]:
        parser.exit(1, "No AP-DIAG lines found: check diagnostic JAR and eastWest.seamDiagnostics.\n")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
