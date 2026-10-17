"""Verify a committed move survives restarting the Compose API, through the actual Nginx proxy."""
import argparse
import json
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--base-url", default="http://127.0.0.1:8081")
parser.add_argument("--restart", action="store_true", help="Restart the backend container owned by this project")
args = parser.parse_args()


def request(method, path, actor, body=None):
    payload = json.dumps(body).encode() if body is not None else None
    headers = {"X-Player-Id": actor, "Content-Type": "application/json"}
    req = urllib.request.Request(args.base_url + "/api" + path, data=payload, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=5) as response:
        return json.load(response)


white, black = str(uuid.uuid4()), str(uuid.uuid4())
game = request("POST", "/games", white, {"whitePlayerId": white, "blackPlayerId": black})
path = "/games/" + game["id"]
command = {"from": {"row": 5, "column": 0}, "landings": [{"row": 4, "column": 1}],
           "expectedVersion": 0, "clientMoveId": str(uuid.uuid4())}
confirmed = request("POST", path + "/moves", white, command)
assert confirmed["version"] == 1
if args.restart:
    subprocess.run(["docker", "compose", "restart", "backend"], cwd=Path(__file__).resolve().parents[1], check=True)

last_error = None
for _ in range(60):
    try:
        restored = request("GET", path, white)
        assert restored["version"] == 1 and restored["pieces"] == confirmed["pieces"]
        replay = request("POST", path + "/moves", white, command)
        assert replay["version"] == 1
        print("PASS: committed game and idempotency receipt survive API restart through Nginx")
        break
    except (urllib.error.URLError, TimeoutError) as failure:
        last_error = failure
        time.sleep(1)
else:
    raise RuntimeError("API did not recover after restart") from last_error
