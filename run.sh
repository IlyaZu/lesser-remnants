#!/user/bin/env bash

set -eEuo pipefail

java -cp build/classes:src rotp.Rotp arg1
